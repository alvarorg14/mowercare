import {
  getOrganizationIdFromAccessToken,
  getRoleFromAccessToken,
  getSubjectFromAccessToken,
} from '../lib/jwt-org';

function jwtWithPayload(payload: Record<string, unknown>): string {
  const header = Buffer.from('{}', 'utf8').toString('base64url');
  const body = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  return `${header}.${body}.sig`;
}

describe('jwt-org', () => {
  const org = '550e8400-e29b-41d4-a716-446655440000';
  const sub = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

  describe('getOrganizationIdFromAccessToken', () => {
    it('returns organizationId from valid payload', () => {
      const token = jwtWithPayload({ organizationId: org, sub });
      expect(getOrganizationIdFromAccessToken(token)).toBe(org);
    });

    it('returns null when organizationId is missing', () => {
      const token = jwtWithPayload({ sub });
      expect(getOrganizationIdFromAccessToken(token)).toBeNull();
    });

    it('returns null for malformed token (no dots)', () => {
      expect(getOrganizationIdFromAccessToken('nodots')).toBeNull();
    });

    it('returns null for invalid JSON in payload', () => {
      const bad = `xx.${Buffer.from('{', 'utf8').toString('base64url')}.yy`;
      expect(getOrganizationIdFromAccessToken(bad)).toBeNull();
    });
  });

  describe('getSubjectFromAccessToken', () => {
    it('returns sub claim', () => {
      const token = jwtWithPayload({ sub, organizationId: org });
      expect(getSubjectFromAccessToken(token)).toBe(sub);
    });

    it('returns null when sub missing', () => {
      expect(getSubjectFromAccessToken(jwtWithPayload({ organizationId: org }))).toBeNull();
    });
  });

  describe('getRoleFromAccessToken', () => {
    it('returns ADMIN when role claim is ADMIN', () => {
      expect(getRoleFromAccessToken(jwtWithPayload({ role: 'ADMIN' }))).toBe('ADMIN');
    });

    it('returns TECHNICIAN when role claim is TECHNICIAN', () => {
      expect(getRoleFromAccessToken(jwtWithPayload({ role: 'TECHNICIAN' }))).toBe('TECHNICIAN');
    });

    it('returns null for unknown role string', () => {
      expect(getRoleFromAccessToken(jwtWithPayload({ role: 'SUPERUSER' }))).toBeNull();
    });

    it('returns null when role missing', () => {
      expect(getRoleFromAccessToken(jwtWithPayload({ sub }))).toBeNull();
    });
  });
});
