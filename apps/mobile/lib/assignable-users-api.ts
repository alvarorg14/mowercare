import { authenticatedFetchJson } from './api';
import { getSessionOrganizationId } from './auth/session';

import type { UserRole } from './organization-users-api';
import type { AccountStatus } from './organization-users-api';

export type AssignableUserResponse = {
  id: string;
  email: string;
  role: UserRole;
  accountStatus: AccountStatus;
};

export function assignableUsersQueryKey(orgId: string) {
  return ['assignable-users', orgId] as const;
}

export async function listAssignableUsers(): Promise<AssignableUserResponse[]> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson<AssignableUserResponse[]>(
    `/api/v1/organizations/${orgId}/assignable-users`,
    { method: 'GET' },
  );
}
