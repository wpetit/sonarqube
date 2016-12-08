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

package org.sonarqube.ws.client.projectanalysis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class CreateEventRequest {
  private final String analysis;
  private final Category category;
  private final String name;
  private final String description;

  private CreateEventRequest(Builder builder) {
    analysis = builder.analysis;
    category = builder.category;
    name = builder.name;
    description = builder.description;
  }

  public String getAnalysis() {
    return analysis;
  }

  public Category getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String analysis;
    private Category category = Category.OTHER;
    private String name;
    private String description;

    private Builder() {
      // enforce static factory method
    }

    public Builder setAnalysis(String analysis) {
      this.analysis = analysis;
      return this;
    }

    public Builder setCategory(Category category) {
      this.category = category;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      this.description = description;
      return this;
    }

    public CreateEventRequest build() {
      checkArgument(analysis != null, "Analysis key is required");
      checkArgument(category != null, "Category is required");
      checkArgument(name != null, "Name is required");

      return new CreateEventRequest(this);
    }
  }

  public enum Category {
    VERSION("Version"), OTHER("Other");

    private final String label;

    Category(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public static Category fromLabel(String label) {
      for (Category category : values()) {
        if (category.getLabel().equals(label)) {
          return category;
        }
      }

      throw new IllegalArgumentException("Unknown event category label '" + label + "'");
    }
  }
}