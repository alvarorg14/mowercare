let accessToken: string | null = null;
let tokenType = 'Bearer';
let organizationId: string | null = null;
/** Mirrors JWT `role` claim; updated whenever `setSession` runs (login, refresh). */
let userRole: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function getTokenType(): string {
  return tokenType;
}

export function getSessionOrganizationId(): string | null {
  return organizationId;
}

export function getSessionRole(): string | null {
  return userRole;
}

export function setSession(
  access: string | null,
  type: string | undefined,
  org: string | null,
  role?: string | null,
): void {
  accessToken = access;
  tokenType = type || 'Bearer';
  organizationId = org;
  userRole = role ?? null;
}
