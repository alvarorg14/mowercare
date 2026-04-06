import type { Router } from 'expo-router';

import {
  __resetPushNavigationStateForTests,
  flushPendingPushDeepLink,
  handlePushNotificationOpen,
  orgIdsEqual,
  parsePushPayloadFromData,
} from '../lib/push-navigation';

/** Matches `expo-notifications` — avoid importing the package in tests (side effects / Expo Go warning). */
const DEFAULT_ACTION_IDENTIFIER = 'expo.modules.notifications.actions.DEFAULT';

function mockRouter(): Router {
  return {
    replace: jest.fn(),
    push: jest.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any;
}

function responseWithData(data: Record<string, unknown>) {
  return {
    actionIdentifier: DEFAULT_ACTION_IDENTIFIER,
    notification: {
      request: {
        identifier: 'nid-1',
        content: { data },
      },
    },
  };
}

describe('parsePushPayloadFromData', () => {
  const org = '550e8400-e29b-41d4-a716-446655440000';
  const issue = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

  it('parses valid string payload', () => {
    expect(parsePushPayloadFromData({ organizationId: org, issueId: issue })).toEqual({
      ok: true,
      organizationId: org.toLowerCase(),
      issueId: issue,
    });
  });

  it('rejects missing keys', () => {
    expect(parsePushPayloadFromData({ organizationId: org })).toEqual({ ok: false });
    expect(parsePushPayloadFromData({ issueId: issue })).toEqual({ ok: false });
  });

  it('rejects invalid issue uuid', () => {
    expect(
      parsePushPayloadFromData({ organizationId: org, issueId: 'not-a-uuid' }),
    ).toEqual({ ok: false });
  });

  it('rejects invalid org uuid', () => {
    expect(
      parsePushPayloadFromData({ organizationId: 'bad', issueId: issue }),
    ).toEqual({ ok: false });
  });
});

describe('orgIdsEqual', () => {
  it('compares case-insensitively', () => {
    expect(
      orgIdsEqual('550E8400-E29B-41D4-A716-446655440000', '550e8400-e29b-41d4-a716-446655440000'),
    ).toBe(true);
  });
});

describe('handlePushNotificationOpen', () => {
  beforeEach(() => {
    __resetPushNavigationStateForTests();
    jest.clearAllMocks();
  });

  const org = '550e8400-e29b-41d4-a716-446655440000';
  const issue = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

  it('navigates with replace on cold_start when authed and org matches', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: org,
      source: 'cold_start',
    });
    expect(router.replace).toHaveBeenCalledWith(`/(app)/(tabs)/issues/${issue}`);
    expect(router.push).not.toHaveBeenCalled();
  });

  it('navigates with push on listener when authed and org matches', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: org,
      source: 'listener',
    });
    expect(router.push).toHaveBeenCalledWith(`/(app)/(tabs)/issues/${issue}`);
  });

  it('routes to wrong_org screen when org mismatches', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: '7ba7b810-9dad-11d1-80b4-00c04fd430c8',
      source: 'listener',
    });
    expect(router.replace).toHaveBeenCalledWith('/(app)/push-link-error?reason=wrong_org');
  });

  it('no-ops while restoring session', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: true,
      isRestoringSession: true,
      sessionOrgId: org,
      source: 'listener',
    });
    expect(router.replace).not.toHaveBeenCalled();
    expect(router.push).not.toHaveBeenCalled();
  });

  it('skips duplicate cold_start for the same notification key', () => {
    const router = mockRouter();
    const r = responseWithData({ organizationId: org, issueId: issue });
    handlePushNotificationOpen(r, {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: org,
      source: 'cold_start',
    });
    expect(router.replace).toHaveBeenCalledTimes(1);
    handlePushNotificationOpen(r, {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: org,
      source: 'cold_start',
    });
    expect(router.replace).toHaveBeenCalledTimes(1);
  });

  it('routes to invalid link screen when authed and payload is invalid', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: 'not-a-uuid' }), {
      router,
      isAuthenticated: true,
      isRestoringSession: false,
      sessionOrgId: org,
      source: 'listener',
    });
    expect(router.replace).toHaveBeenCalledWith('/(app)/push-link-error?reason=invalid');
    expect(router.push).not.toHaveBeenCalled();
  });
});

describe('flushPendingPushDeepLink', () => {
  beforeEach(() => {
    __resetPushNavigationStateForTests();
    jest.clearAllMocks();
  });

  const org = '550e8400-e29b-41d4-a716-446655440000';
  const issue = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

  it('flushes queued open after sign-in', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: false,
      isRestoringSession: false,
      sessionOrgId: null,
      source: 'cold_start',
    });
    expect(router.replace).not.toHaveBeenCalled();

    flushPendingPushDeepLink({
      router,
      isAuthenticated: true,
      sessionOrgId: org,
    });
    expect(router.replace).toHaveBeenCalledWith(`/(app)/(tabs)/issues/${issue}`);
  });

  it('flushes queued invalid link after sign-in', () => {
    const router = mockRouter();
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: 'bad-id' }), {
      router,
      isAuthenticated: false,
      isRestoringSession: false,
      sessionOrgId: null,
      source: 'cold_start',
    });
    expect(router.replace).not.toHaveBeenCalled();

    flushPendingPushDeepLink({
      router,
      isAuthenticated: true,
      sessionOrgId: org,
    });
    expect(router.replace).toHaveBeenCalledWith('/(app)/push-link-error?reason=invalid');
  });

  it('flushes queued open to wrong_org when session org differs', () => {
    const router = mockRouter();
    const otherOrg = '7ba7b810-9dad-11d1-80b4-00c04fd430c8';
    handlePushNotificationOpen(responseWithData({ organizationId: org, issueId: issue }), {
      router,
      isAuthenticated: false,
      isRestoringSession: false,
      sessionOrgId: null,
      source: 'cold_start',
    });
    expect(router.replace).not.toHaveBeenCalled();

    flushPendingPushDeepLink({
      router,
      isAuthenticated: true,
      sessionOrgId: otherOrg,
    });
    expect(router.replace).toHaveBeenCalledWith('/(app)/push-link-error?reason=wrong_org');
  });
});
