/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.rule.index;

import java.util.stream.Stream;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;

public class RuleIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleIndexer(DbClient dbClient, EsClient esClient,
                     DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void index(RuleKey customRuleKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      doIndex(createBulkIndexer(Size.REGULAR),
        RuleResultSetIterator.create(dbClient, dbSession, defaultOrganizationProvider.get().getUuid(), customRuleKey));
    }
  }

  public void indexOnStartup() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      doIndex(createBulkIndexer(Size.LARGE),
        RuleResultSetIterator.create(dbClient, dbSession, defaultOrganizationProvider.get().getUuid()));
    }
  }

  private static void doIndex(BulkIndexer bulk, Stream<RuleDoc> rules) {
    bulk.start();
    rules.map(RuleIndexer::newIndexRequest)
      .forEachOrdered(bulk::add);
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(Size size) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_RULE.getIndex());
    bulk.setSize(size);
    return bulk;
  }

  private static IndexRequest newIndexRequest(RuleDoc rule) {
    return new IndexRequest(INDEX_TYPE_RULE.getIndex(), INDEX_TYPE_RULE.getType(), rule.key().toString()).source(rule.getFields());
  }

  public void delete(RuleKey ruleKey) {
    esClient.prepareDelete(RuleIndexDefinition.INDEX_TYPE_RULE, ruleKey.toString()).get();
  }
}
