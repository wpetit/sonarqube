/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.notification.ws;

import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Notifications.ListResponse;
import org.sonarqube.ws.Notifications.Notification;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.Collectors.toList;
import static org.sonar.core.util.stream.Collectors.toOneElement;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.ACTION_LIST;

public class ListAction implements NotificationsWsAction {
  private static final Splitter PROPERTY_KEY_SPLITTER = Splitter.on(".");

  private final DbClient dbClient;
  private final UserSession userSession;
  private final List<String> globalDispatchers;
  private final List<String> perProjectDispatchers;
  private final List<String> channels;

  public ListAction(NotificationCenter notificationCenter, DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.globalDispatchers = notificationCenter.getDispatcherKeysForProperty(GLOBAL_NOTIFICATION, "true").stream().sorted().collect(Collectors.toList());
    this.perProjectDispatchers = notificationCenter.getDispatcherKeysForProperty(PER_PROJECT_NOTIFICATION, "true").stream().sorted().collect(Collectors.toList());
    this.channels = notificationCenter.getChannels().stream().map(NotificationChannel::getKey).sorted().collect(Collectors.toList());
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION_LIST)
      .setDescription("List notifications of the authenticated user.<br>" +
        "Requires authentication.")
      .setSince("6.3")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ListResponse listResponse = Stream.of(request)
      .peek(checkPermissions())
      .map(search())
      .collect(toOneElement());

    writeProtobuf(listResponse, request, response);
  }

  private Function<Request, ListResponse> search() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        return Stream
          .of(ListResponse.newBuilder())
          .map(r -> r.addAllChannels(channels))
          .map(r -> r.addAllGlobalTypes(globalDispatchers))
          .map(r -> r.addAllPerProjectTypes(perProjectDispatchers))
          .map(addNotifications(dbSession))
          .map(ListResponse.Builder::build)
          .collect(toOneElement());
      }
    };
  }

  private UnaryOperator<ListResponse.Builder> addNotifications(DbSession dbSession) {
    return response -> {
      List<PropertyDto> properties = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setUserId(userSession.getUserId()).build(), dbSession);
      Map<Long, ComponentDto> componentsById = searchProjects(dbSession, properties);

      Predicate<PropertyDto> isNotification = prop -> prop.getKey().startsWith("notification.");
      Predicate<PropertyDto> isComponentInDb = prop -> prop.getResourceId() == null || componentsById.containsKey(prop.getResourceId());

      Notification.Builder notification = Notification.newBuilder();

      properties.stream()
        .filter(isNotification)
        .filter(channelAndDispatcherAuthorized())
        .filter(isComponentInDb)
        .map(toWsNotification(notification, componentsById))
        .sorted(comparing(Notification::getProject, nullsFirst(naturalOrder()))
          .thenComparing(comparing(Notification::getChannel))
          .thenComparing(comparing(Notification::getType)))
        .forEach(response::addNotifications);

      return response;
    };
  }

  private Predicate<PropertyDto> channelAndDispatcherAuthorized() {
    return prop -> {
      List<String> key = PROPERTY_KEY_SPLITTER.splitToList(prop.getKey());
      return key.size() == 3
        && channels.contains(key.get(2))
        && isDispatcherAuthorized(prop, key.get(1));
    };
  }

  private boolean isDispatcherAuthorized(PropertyDto prop, String dispatcher) {
    return (prop.getResourceId() != null && perProjectDispatchers.contains(dispatcher)) || globalDispatchers.contains(dispatcher);
  }

  private Map<Long, ComponentDto> searchProjects(DbSession dbSession, List<PropertyDto> properties) {
    Collection<String> authorizedComponentUuids = dbClient.authorizationDao().selectAuthorizedRootProjectsUuids(dbSession, userSession.getUserId(), UserRole.USER);
    return dbClient.componentDao().selectByIds(dbSession,
      properties.stream()
        .filter(prop -> prop.getResourceId() != null)
        .map(PropertyDto::getResourceId)
        .distinct()
        .collect(toList()))
      .stream()
      .filter(c -> authorizedComponentUuids.contains(c.uuid()))
      .collect(Collectors.uniqueIndex(ComponentDto::getId));
  }

  private static Function<PropertyDto, Notification> toWsNotification(Notification.Builder notification, Map<Long, ComponentDto> projectsById) {
    return property -> {
      notification.clear();
      List<String> propertyKey = Splitter.on(".").splitToList(property.getKey());
      notification.setType(propertyKey.get(1));
      notification.setChannel(propertyKey.get(2));
      setNullable(property.getResourceId(), componentId -> {
        ComponentDto project = projectsById.get(componentId);
        notification.setProject(project.getKey());
        notification.setProjectName(project.name());
        return notification;
      });

      return notification.build();
    };
  }

  private Consumer<Request> checkPermissions() {
    return request -> userSession.checkLoggedIn();
  }
}