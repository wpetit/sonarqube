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
package org.sonar.server.qualityprofile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class QProfileFactoryTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();

  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private QProfileFactory underTest = new QProfileFactory(db.getDbClient(), new SequenceUuidFactory(), System2.INSTANCE, activeRuleIndexer);
  private RuleDto rule;
  private RuleParamDto ruleParam;

  @Before
  public void setUp() throws Exception {
    rule = db.rules().insertRule(RuleTesting.newRuleDto());
    ruleParam = db.rules().insertRuleParam(rule);
  }

  @Test
  public void deleteByKeys_deletes_profiles_in_db_and_elasticsearch() {
    OrganizationDto org = db.organizations().insert();
    QualityProfileDto profile1 = createRandomProfile(org);
    QualityProfileDto profile2 = createRandomProfile(org);
    QualityProfileDto profile3 = createRandomProfile(org);

    List<String> profileKeys = asList(profile1.getKey(), profile2.getKey(), "does_not_exist");
    underTest.deleteByKeys(db.getSession(), profileKeys);

    assertOnlyExists(org, profile3);
    verify(activeRuleIndexer).deleteByProfileKeys(profileKeys);
  }

  @Test
  public void deleteByKeys_accepts_empty_list_of_keys() {
    OrganizationDto org = db.organizations().insert();
    QualityProfileDto profile1 = createRandomProfile(org);

    underTest.deleteByKeys(db.getSession(), Collections.emptyList());

    assertOnlyExists(org, profile1);
    verifyZeroInteractions(activeRuleIndexer);
  }

  private void assertOnlyExists(OrganizationDto org, QualityProfileDto profile) {
    List<QualityProfileDto> profiles = db.getDbClient().qualityProfileDao().selectAll(db.getSession(), org);
    assertThat(profiles).extracting(QualityProfileDto::getKey).containsOnly(profile.getKey());

    List<ActiveRuleDto> activeRules = db.getDbClient().activeRuleDao().selectAll(db.getSession());
    assertThat(activeRules).extracting(ActiveRuleDto::getProfileId).containsOnly(profile.getId());

    List<ActiveRuleParamDto> activeParams = db.getDbClient().activeRuleDao().selectAllParams(db.getSession());
    List<Integer> activeRuleIds = activeRules.stream().map(ActiveRuleDto::getId).collect(Collectors.toList());
    assertThat(activeParams).extracting(ActiveRuleParamDto::getActiveRuleId).containsOnlyElementsOf(activeRuleIds);
  }

  private QualityProfileDto createRandomProfile(OrganizationDto org) {
    QualityProfileDto profile = db.qualityProfiles().insert(org);
    ComponentDto project = db.components().insertProject(org);
    db.qualityProfiles().associateProjectWithQualityProfile(project, profile);
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(profile.getId())
      .setRuleId(rule.getId())
      .setSeverity(Severity.BLOCKER);
    db.getDbClient().activeRuleDao().insert(db.getSession(), activeRuleDto);
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setRulesParameterId(ruleParam.getId()).setKey("foo").setValue("bar");
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRuleDto, activeRuleParam);
    db.getSession().commit();
    return profile;
  }

}
