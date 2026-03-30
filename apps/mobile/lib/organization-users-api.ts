import { authenticatedFetchJson } from './api';
import { getSessionOrganizationId } from './auth/session';

/** Mirrors API `UserRole`. */
export type UserRole = 'ADMIN' | 'TECHNICIAN';

/** Mirrors API `AccountStatus`. */
export type AccountStatus = 'ACTIVE' | 'PENDING_INVITE' | 'DEACTIVATED';

export type EmployeeUserResponse = {
  id: string;
  email: string;
  role: UserRole;
  accountStatus: AccountStatus;
  createdAt: string;
};

export type CreateEmployeeUserRequest = {
  email: string;
  role: UserRole;
  /** Omit for invite-only (pending) account. */
  initialPassword?: string;
};

export type CreateEmployeeUserResponse = {
  id: string;
  email: string;
  role: UserRole;
  accountStatus: AccountStatus;
  inviteToken?: string | null;
};

export type UpdateEmployeeUserRoleRequest = {
  role: UserRole;
};

export function employeeUsersQueryKey(orgId: string) {
  return ['employee-users', orgId] as const;
}

export async function listEmployeeUsers(): Promise<EmployeeUserResponse[]> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson<EmployeeUserResponse[]>(
    `/api/v1/organizations/${orgId}/users`,
    { method: 'GET' },
  );
}

export async function createEmployeeUser(
  body: CreateEmployeeUserRequest,
): Promise<CreateEmployeeUserResponse> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  const payload: Record<string, unknown> = { email: body.email.trim(), role: body.role };
  if (body.initialPassword && body.initialPassword.trim().length > 0) {
    payload.initialPassword = body.initialPassword;
  }
  return authenticatedFetchJson<CreateEmployeeUserResponse>(
    `/api/v1/organizations/${orgId}/users`,
    { method: 'POST', body: JSON.stringify(payload) },
  );
}

export async function updateEmployeeRole(
  userId: string,
  role: UserRole,
): Promise<EmployeeUserResponse> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  const body: UpdateEmployeeUserRoleRequest = { role };
  return authenticatedFetchJson<EmployeeUserResponse>(
    `/api/v1/organizations/${orgId}/users/${userId}`,
    { method: 'PATCH', body: JSON.stringify(body) },
  );
}

export async function deactivateEmployeeUser(userId: string): Promise<EmployeeUserResponse> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson<EmployeeUserResponse>(
    `/api/v1/organizations/${orgId}/users/${userId}/deactivate`,
    { method: 'POST' },
  );
}
