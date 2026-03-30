import Constants from 'expo-constants';

/** Single default for `app.config` `extra` and runtime fallback (AC4). */
export const DEFAULT_API_BASE_URL = 'http://localhost:8080';

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
