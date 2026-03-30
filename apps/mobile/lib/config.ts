import Constants from 'expo-constants';

import { DEFAULT_API_BASE_URL } from '../default-api-base-url.js';

export { DEFAULT_API_BASE_URL };

function stripTrailingSlash(url: string): string {
  return url.replace(/\/$/, '');
}

/**
 * API base URL: `EXPO_PUBLIC_API_BASE_URL` overrides `app.config` extra.
 * Default is local Spring Boot; real devices must use host LAN IP or tunnel.
 */
export function getApiBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_BASE_URL?.trim();
  const fromExtra = Constants.expoConfig?.extra?.apiBaseUrl as string | undefined;
  const raw = fromEnv || fromExtra || DEFAULT_API_BASE_URL;
  return stripTrailingSlash(raw);
}
