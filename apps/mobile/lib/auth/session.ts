let accessToken: string | null = null;
let tokenType = 'Bearer';
let organizationId: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function getTokenType(): string {
  return tokenType;
}

export function getSessionOrganizationId(): string | null {
  return organizationId;
}

export function setSession(access: string | null, type: string | undefined, org: string | null): void {
  accessToken = access;
  tokenType = type || 'Bearer';
  organizationId = org;
}
