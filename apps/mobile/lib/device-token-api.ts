import { authenticatedFetchJson } from './api';

/** Matches API enum `DevicePushPlatform` (camelCase JSON uses same names). */
export type DevicePushPlatform = 'IOS' | 'ANDROID' | 'UNKNOWN';

export async function registerDevicePushToken(
  organizationId: string,
  token: string,
  platform: DevicePushPlatform,
): Promise<{ id: string }> {
  return authenticatedFetchJson<{ id: string }>(`/api/v1/organizations/${organizationId}/device-push-tokens`, {
    method: 'PUT',
    body: JSON.stringify({ token, platform }),
  });
}

export async function revokeDevicePushToken(organizationId: string, token: string): Promise<void> {
  await authenticatedFetchJson<void>(`/api/v1/organizations/${organizationId}/device-push-tokens`, {
    method: 'DELETE',
    body: JSON.stringify({ token }),
  });
}
