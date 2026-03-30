import { authenticatedFetchJson } from './api';
import { getSessionOrganizationId } from './auth/session';

export type OrganizationProfile = {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
};

export function organizationProfileQueryKey(orgId: string) {
  return ['organization-profile', orgId] as const;
}

export function fetchOrganizationProfile(): Promise<OrganizationProfile> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/profile`, { method: 'GET' });
}

export function patchOrganizationProfile(body: { name: string }): Promise<OrganizationProfile> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/profile`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}
