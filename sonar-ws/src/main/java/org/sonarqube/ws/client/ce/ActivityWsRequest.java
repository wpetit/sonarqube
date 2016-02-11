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

package org.sonarqube.ws.client.ce;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ActivityWsRequest {
  private String componentId;
  private String componentQuery;
  private List<String> status;
  private String type;
  private Boolean onlyCurrents;
  private String minSubmittedAt;
  private String maxExecutedAt;
  private Integer page;
  private Integer pageSize;

  @CheckForNull
  public String getComponentId() {
    return componentId;
  }

  public ActivityWsRequest setComponentId(@Nullable String componentId) {
    this.componentId = componentId;
    return this;
  }

  @CheckForNull
  public String getComponentQuery() {
    return componentQuery;
  }

  public ActivityWsRequest setComponentQuery(@Nullable String componentQuery) {
    this.componentQuery = componentQuery;
    return this;
  }

  @CheckForNull
  public List<String> getStatus() {
    return status;
  }

  public ActivityWsRequest setStatus(@Nullable List<String> status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getType() {
    return type;
  }

  public ActivityWsRequest setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public Boolean getOnlyCurrents() {
    return onlyCurrents;
  }

  public ActivityWsRequest setOnlyCurrents(@Nullable Boolean onlyCurrents) {
    this.onlyCurrents = onlyCurrents;
    return this;
  }

  @CheckForNull
  public String getMinSubmittedAt() {
    return minSubmittedAt;
  }

  public ActivityWsRequest setMinSubmittedAt(@Nullable String minSubmittedAt) {
    this.minSubmittedAt = minSubmittedAt;
    return this;
  }

  @CheckForNull
  public String getMaxExecutedAt() {
    return maxExecutedAt;
  }

  public ActivityWsRequest setMaxExecutedAt(@Nullable String maxExecutedAt) {
    this.maxExecutedAt = maxExecutedAt;
    return this;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public ActivityWsRequest setPage(@Nullable Integer page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public ActivityWsRequest setPageSize(@Nullable Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }
}
