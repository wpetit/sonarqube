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
// @flow
import React from 'react';
import PageHeader from './PageHeader';
import MembersList from './MembersList';
import UsersSearch from '../../users/components/UsersSearch';
import ListFooter from '../../../components/controls/ListFooter';
import type { Organization } from '../../../store/organizations/duck';
import type { Member } from '../../../store/organizationsMembers/actions';

type Props = {
  members: Array<Member>,
  state: { loading?: boolean, total?: number, pageIndex?: number, query?: string },
  organization: Organization,
  fetchOrganizationMembers: (organizationKey: string, query?: string) => void,
  fetchMoreOrganizationMembers: (organizationKey: string, query?: string) => void
};

type State = {
  query: string | null
};

export default class OrganizationMembers extends React.PureComponent {
  props: Props;

  state: State = {
    query: null
  };
  componentDidMount() {
    const notLoadedYet = this.props.members.length < 1 ||
      this.props.state.query !== this.state.query;
    if (!this.props.loading && notLoadedYet) {
      this.handleSearchMembers();
    }
  }

  handleSearchMembers = (query?: string) => {
    this.setState({ query });
    this.props.fetchOrganizationMembers(this.props.organization.key, query);
  };

  handleLoadMoreMembers = () => {
    this.props.fetchMoreOrganizationMembers(this.props.organization.key, this.props.state.query);
  };

  addMember() {
    // TODO Not yet implemented
  }

  render() {
    const { organization, state, members } = this.props;
    return (
      <div className="page page-limited">
        <PageHeader organization={organization} loading={state.loading} total={state.total} />
        <UsersSearch onSearch={this.handleSearchMembers} />
        <MembersList members={members} organization={organization} />
        {state.total != null &&
          <ListFooter
            count={members.length}
            total={state.total}
            ready={!state.loading}
            loadMore={this.handleLoadMoreMembers}
          />}
      </div>
    );
  }
}
