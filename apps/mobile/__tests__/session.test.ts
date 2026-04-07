import {
  getAccessToken,
  getSessionOrganizationId,
  getSessionRole,
  getTokenType,
  setSession,
} from '../lib/auth/session';

describe('auth/session', () => {
  beforeEach(() => {
    setSession(null, undefined, null, null);
  });

  it('starts with empty session and default Bearer token type', () => {
    expect(getAccessToken()).toBeNull();
    expect(getSessionOrganizationId()).toBeNull();
    expect(getSessionRole()).toBeNull();
    expect(getTokenType()).toBe('Bearer');
  });

  it('sets access, custom token type, organization, and role', () => {
    const org = '550e8400-e29b-41d4-a716-446655440000';
    setSession('access-token', 'Custom', org, 'TECHNICIAN');
    expect(getAccessToken()).toBe('access-token');
    expect(getTokenType()).toBe('Custom');
    expect(getSessionOrganizationId()).toBe(org);
    expect(getSessionRole()).toBe('TECHNICIAN');
  });

  it('defaults token type to Bearer when type is undefined', () => {
    setSession('t', undefined, '550e8400-e29b-41d4-a716-446655440000', 'ADMIN');
    expect(getTokenType()).toBe('Bearer');
  });

  it('clears role when setSession omits role', () => {
    setSession('a', undefined, '550e8400-e29b-41d4-a716-446655440000', 'ADMIN');
    setSession('a', undefined, '550e8400-e29b-41d4-a716-446655440000');
    expect(getSessionRole()).toBeNull();
  });
});
