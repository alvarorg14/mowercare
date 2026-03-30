/** Matches `UserRole` enum names from the API (`JwtService.issueAccessToken`). */
export type JwtRole = 'ADMIN' | 'TECHNICIAN';

/**
 * Read organizationId claim from access JWT (no signature verification — server is source of truth).
 */
export function getOrganizationIdFromAccessToken(accessToken: string): string | null {
  try {
    const parts = accessToken.split('.');
    if (parts.length < 2) return null;
    const payload = base64UrlToUtf8(parts[1]);
    const data = JSON.parse(payload) as { organizationId?: string };
    return typeof data.organizationId === 'string' ? data.organizationId : null;
  } catch {
    return null;
  }
}

/**
 * Read JWT `sub` claim — user id (UUID string). No signature verification — server is source of truth.
 */
export function getSubjectFromAccessToken(accessToken: string): string | null {
  try {
    const parts = accessToken.split('.');
    if (parts.length < 2) return null;
    const payload = base64UrlToUtf8(parts[1]);
    const data = JSON.parse(payload) as { sub?: string };
    return typeof data.sub === 'string' ? data.sub : null;
  } catch {
    return null;
  }
}

/**
 * Read role claim from access JWT (no signature verification — server is source of truth).
 */
export function getRoleFromAccessToken(accessToken: string): JwtRole | null {
  try {
    const parts = accessToken.split('.');
    if (parts.length < 2) return null;
    const payload = base64UrlToUtf8(parts[1]);
    const data = JSON.parse(payload) as { role?: string };
    const r = data.role;
    if (r === 'ADMIN' || r === 'TECHNICIAN') return r;
    return null;
  } catch {
    return null;
  }
}

function base64UrlToUtf8(segment: string): string {
  const b64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const pad = b64.length % 4 === 0 ? '' : '='.repeat(4 - (b64.length % 4));
  const binary = globalThis.atob(b64 + pad);
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}
