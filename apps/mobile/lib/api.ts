import { getApiBaseUrl } from './config';
import { refreshApi } from './auth-api';
import { clearRefreshToken, getRefreshToken, setRefreshToken } from './auth-storage';
import { getAccessToken, getSessionOrganizationId, getTokenType, setSession } from './auth/session';
import { getOrganizationIdFromAccessToken, getRoleFromAccessToken } from './jwt-org';
import { ApiProblemError, type ProblemBody } from './http';

/**
 * Refresh rotation + session update. Returns false if refresh fails (caller should clear UI session).
 */
export async function refreshSession(): Promise<boolean> {
  const refresh = await getRefreshToken();
  if (!refresh) return false;
  try {
    const tokens = await refreshApi({ refreshToken: refresh });
    await setRefreshToken(tokens.refreshToken);
    const orgId = getOrganizationIdFromAccessToken(tokens.accessToken);
    setSession(tokens.accessToken, tokens.tokenType, orgId, getRoleFromAccessToken(tokens.accessToken));
    return true;
  } catch {
    await clearRefreshToken();
    setSession(null, 'Bearer', null, null);
    return false;
  }
}

/**
 * GET/POST with Bearer; on 401 + AUTH_INVALID_TOKEN, refresh once and retry once.
 */
export async function authenticatedFetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const url = `${getApiBaseUrl()}${path}`;
  const method = init?.method ?? 'GET';

  const buildHeaders = (access: string): HeadersInit => {
    const h = new Headers(init?.headers);
    h.set('Authorization', `${getTokenType()} ${access}`);
    if (method !== 'GET' && method !== 'HEAD' && !h.has('Content-Type')) {
      h.set('Content-Type', 'application/json');
    }
    return h;
  };

  const exec = async (access: string) =>
    fetch(url, {
      ...init,
      method,
      headers: buildHeaders(access),
    });

  let access = getAccessToken();
  if (!access) {
    throw new ApiProblemError(401, { code: 'AUTH_INVALID_TOKEN', title: 'Unauthorized', detail: 'Not signed in' });
  }

  let res = await exec(access);

  if (res.status === 401) {
    const text401 = await res.text();
    let problem: ProblemBody | undefined;
    try {
      problem = text401 ? (JSON.parse(text401) as ProblemBody) : undefined;
    } catch {
      problem = undefined;
    }
    if (problem?.code === 'AUTH_INVALID_TOKEN') {
      const ok = await refreshSession();
      if (!ok || !getAccessToken()) {
        throw new ApiProblemError(401, {
          code: 'AUTH_REFRESH_INVALID',
          title: 'Unauthorized',
          detail: 'Session expired. Sign in again.',
        });
      }
      res = await exec(getAccessToken()!);
    } else {
      throw new ApiProblemError(401, problem || { title: 'Unauthorized', detail: text401 });
    }
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let parsed: unknown;
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = undefined;
    }
  }

  if (!res.ok) {
    const problem = parsed as ProblemBody | undefined;
    if (problem && typeof problem === 'object') {
      throw new ApiProblemError(res.status, problem);
    }
    throw new ApiProblemError(res.status, { title: 'Error', detail: text || res.statusText });
  }

  return parsed as T;
}

/** AC5 probe — valid JWT + tenant path alignment. */
export async function verifyTenantScope(): Promise<void> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  await authenticatedFetchJson(`/api/v1/organizations/${orgId}/tenant-scope`, {
    method: 'GET',
  });
}
