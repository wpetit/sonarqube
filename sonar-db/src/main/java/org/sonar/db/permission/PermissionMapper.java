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
package org.sonar.db.permission;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Only the requests involving both user and group permissions.
 *
 * @see GroupPermissionMapper
 * @see UserPermissionMapper
 */
public interface PermissionMapper {

  List<Long> keepAuthorizedProjectIdsForAnonymous(@Param("role") String role, @Param("componentIds") Collection<Long> componentIds);

  List<Long> keepAuthorizedProjectIdsForUser(@Param("userId") long userId, @Param("role") String role, @Param("componentIds") Collection<Long> componentIds);

  List<String> keepAuthorizedComponentKeysForAnonymous(@Param("role") String role, @Param("componentKeys") Collection<String> componentKeys);

  List<String> keepAuthorizedComponentKeysForUser(@Param("userId") Integer userId, @Param("role") String role, @Param("componentKeys") Collection<String> componentKeys);

  List<Long> keepAuthorizedUsersForRoleAndProject(@Param("role") String role, @Param("componentId") long componentId, @Param("userIds") List<Long> userIds);

}