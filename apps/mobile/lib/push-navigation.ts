import type { Router } from 'expo-router';

/** Same rule as `app/(app)/(tabs)/issues/[id].tsx` — keep in sync for push payloads. */
export const ISSUE_UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function normalizeUuidString(s: string): string {
  return s.trim().toLowerCase();
}

export function isLikelyIssueUuid(s: string): boolean {
  return ISSUE_UUID_RE.test(s.trim());
}

export type ParsedPushPayload =
  | { ok: true; organizationId: string; issueId: string }
  | { ok: false };

export function parsePushPayloadFromData(data: Record<string, unknown> | undefined | null): ParsedPushPayload {
  if (!data) return { ok: false };
  const rawIssue = data.issueId;
  const rawOrg = data.organizationId;
  if (typeof rawIssue !== 'string' || typeof rawOrg !== 'string') return { ok: false };
  const issueId = rawIssue.trim();
  const organizationId = normalizeUuidString(rawOrg);
  if (!isLikelyIssueUuid(issueId)) return { ok: false };
  if (!isLikelyIssueUuid(organizationId)) return { ok: false };
  return { ok: true, organizationId, issueId };
}

export function orgIdsEqual(a: string, b: string): boolean {
  return normalizeUuidString(a) === normalizeUuidString(b);
}

export type PendingPushDeepLink =
  | { kind: 'open'; organizationId: string; issueId: string }
  | { kind: 'invalid' };

let pendingPushDeepLink: PendingPushDeepLink | null = null;

export function clearPendingPushDeepLink(): void {
  pendingPushDeepLink = null;
}

/** For tests — reset module state. */
export function __resetPushNavigationStateForTests(): void {
  pendingPushDeepLink = null;
  lastColdStartDedupeKey = null;
}

let lastColdStartDedupeKey: string | null = null;

export function buildPushResponseDedupeKey(response: {
  notification: { request: { identifier: string } };
  actionIdentifier: string;
}): string {
  return `${response.notification.request.identifier}::${response.actionIdentifier}`;
}

/**
 * When user opens app from a quit state via notification tap, the same response can be returned
 * again on later resumes — skip if we already handled this key from cold_start.
 */
function markColdStartHandled(key: string): void {
  lastColdStartDedupeKey = key;
}

function shouldSkipRepeatedColdStart(key: string): boolean {
  return key === lastColdStartDedupeKey;
}

/**
 * AC4: queue a single pending open when auth/org is not ready; flushed after sign-in.
 * Invalid payload while logged out queues `{ kind: 'invalid' }` and shows error screen after auth.
 */
export function handlePushNotificationOpen(
  response: {
    notification: {
      request: {
        identifier: string;
        content: { data?: Record<string, unknown> };
      };
    };
    actionIdentifier: string;
  },
  ctx: {
    router: Router;
    isAuthenticated: boolean;
    isRestoringSession: boolean;
    sessionOrgId: string | null;
    source: 'cold_start' | 'listener';
  },
): void {
  if (ctx.isRestoringSession) return;

  const key = buildPushResponseDedupeKey(response);
  if (ctx.source === 'cold_start' && shouldSkipRepeatedColdStart(key)) {
    return;
  }

  const parsed = parsePushPayloadFromData(response.notification.request.content.data);

  if (!parsed.ok) {
    if (!ctx.isAuthenticated) {
      pendingPushDeepLink = { kind: 'invalid' };
      if (ctx.source === 'cold_start') markColdStartHandled(key);
      return;
    }
    ctx.router.replace('/(app)/push-link-error?reason=invalid');
    if (ctx.source === 'cold_start') markColdStartHandled(key);
    return;
  }

  const { organizationId, issueId } = parsed;

  if (!ctx.isAuthenticated || !ctx.sessionOrgId) {
    pendingPushDeepLink = { kind: 'open', organizationId, issueId };
    if (ctx.source === 'cold_start') markColdStartHandled(key);
    return;
  }

  if (!orgIdsEqual(organizationId, ctx.sessionOrgId)) {
    ctx.router.replace('/(app)/push-link-error?reason=wrong_org');
    if (ctx.source === 'cold_start') markColdStartHandled(key);
    return;
  }

  if (ctx.source === 'cold_start') {
    markColdStartHandled(key);
    ctx.router.replace(`/(app)/(tabs)/issues/${issueId}`);
  } else {
    ctx.router.push(`/(app)/(tabs)/issues/${issueId}`);
  }
}

export function flushPendingPushDeepLink(ctx: {
  router: Router;
  isAuthenticated: boolean;
  sessionOrgId: string | null;
}): void {
  if (!ctx.isAuthenticated || !pendingPushDeepLink) return;

  const p = pendingPushDeepLink;
  pendingPushDeepLink = null;

  if (p.kind === 'invalid') {
    ctx.router.replace('/(app)/push-link-error?reason=invalid');
    return;
  }

  if (!ctx.sessionOrgId) {
    pendingPushDeepLink = p;
    return;
  }

  if (!orgIdsEqual(p.organizationId, ctx.sessionOrgId)) {
    ctx.router.replace('/(app)/push-link-error?reason=wrong_org');
    return;
  }

  ctx.router.replace(`/(app)/(tabs)/issues/${p.issueId}`);
}
