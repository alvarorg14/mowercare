import { inviteUserFormSchema } from '../lib/organization-users-invite-schema';
import { orgProfileSchema } from '../lib/auth/org-profile-schema';
import { loginSchema } from '../lib/auth/login-schema';
import { issueCreateSchema } from '../lib/issue-create-schema';

describe('loginSchema', () => {
  const valid = {
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
    email: 'a@b.com',
    password: 'password1',
  };

  it('accepts valid login values', () => {
    expect(loginSchema.safeParse(valid).success).toBe(true);
  });

  it('rejects invalid organization UUID with user-facing message', () => {
    const r = loginSchema.safeParse({ ...valid, organizationId: 'not-uuid' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0]?.message).toBe('Valid organization ID (UUID) required');
    }
  });

  it('rejects password shorter than 8 characters', () => {
    const r = loginSchema.safeParse({ ...valid, password: 'short' });
    expect(r.success).toBe(false);
  });

  it('rejects invalid email', () => {
    const r = loginSchema.safeParse({ ...valid, email: 'bad' });
    expect(r.success).toBe(false);
  });
});

describe('issueCreateSchema', () => {
  const base = {
    title: 'Fix mower',
    status: 'OPEN' as const,
    priority: 'MEDIUM' as const,
  };

  it('accepts minimal valid create payload', () => {
    expect(issueCreateSchema.safeParse(base).success).toBe(true);
  });

  it('rejects empty trimmed title', () => {
    const r = issueCreateSchema.safeParse({ ...base, title: '   ' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues.some((i) => i.message.includes('Title'))).toBe(true);
    }
  });

  it('rejects invalid status enum', () => {
    const r = issueCreateSchema.safeParse({ ...base, status: 'UNKNOWN' });
    expect(r.success).toBe(false);
  });

  it('rejects assigneeUserId that is not a UUID when present', () => {
    const r = issueCreateSchema.safeParse({ ...base, assigneeUserId: 'x' });
    expect(r.success).toBe(false);
  });

  it('accepts valid assignee UUID', () => {
    const r = issueCreateSchema.safeParse({
      ...base,
      assigneeUserId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8',
    });
    expect(r.success).toBe(true);
  });
});

describe('orgProfileSchema', () => {
  it('accepts non-empty trimmed name', () => {
    expect(orgProfileSchema.safeParse({ name: ' Acme ' }).success).toBe(true);
  });

  it('rejects blank name with Name is required', () => {
    const r = orgProfileSchema.safeParse({ name: '   ' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0]?.message).toBe('Name is required');
    }
  });
});

describe('inviteUserFormSchema', () => {
  it('accepts invite without initial password', () => {
    expect(
      inviteUserFormSchema.safeParse({
        email: 'u@example.com',
        role: 'TECHNICIAN',
      }).success,
    ).toBe(true);
  });

  it('rejects short initialPassword when provided', () => {
    const r = inviteUserFormSchema.safeParse({
      email: 'u@example.com',
      role: 'ADMIN',
      initialPassword: 'short',
    });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(
        r.error.issues.some((i) => i.message === 'Password must be at least 8 characters'),
      ).toBe(true);
    }
  });

  it('rejects invalid email with Enter a valid email', () => {
    const r = inviteUserFormSchema.safeParse({
      email: 'nope',
      role: 'ADMIN',
    });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues.some((i) => i.message === 'Enter a valid email')).toBe(true);
    }
  });
});
