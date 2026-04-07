import { assignableUsersQueryKey } from '../lib/assignable-users-api';
import { employeeUsersQueryKey } from '../lib/organization-users-api';
import { organizationProfileQueryKey } from '../lib/org-profile-api';

describe('react-query keys', () => {
  it('employeeUsersQueryKey', () => {
    expect(employeeUsersQueryKey('o1')).toEqual(['employee-users', 'o1']);
  });

  it('organizationProfileQueryKey', () => {
    expect(organizationProfileQueryKey('o1')).toEqual(['organization-profile', 'o1']);
  });

  it('assignableUsersQueryKey', () => {
    expect(assignableUsersQueryKey('o1')).toEqual(['assignable-users', 'o1']);
  });
});
