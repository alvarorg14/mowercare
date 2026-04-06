import { authenticatedFetchJson } from './api';
import { getSessionOrganizationId } from './auth/session';

export type NotificationItem = {
  id: string;
  issueId: string;
  issueTitle: string;
  eventType: string;
  occurredAt: string;
  read: boolean;
};

export type NotificationListResult = {
  items: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export function notificationListQueryKey(orgId: string | null, page = 0, size = 50): unknown[] {
  return ['notifications', orgId, page, size];
}

export function listNotifications(page = 0, size = 50): Promise<NotificationListResult> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  const q = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/notifications?${q.toString()}`);
}

export function markNotificationRead(recipientId: string): Promise<void> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson(
    `/api/v1/organizations/${orgId}/notifications/${recipientId}/read`,
    { method: 'PATCH' },
  );
}

/** User-visible label for taxonomy strings (MVP). */
export function formatNotificationEventType(eventType: string): string {
  switch (eventType) {
    case 'issue.created':
      return 'Issue created';
    case 'issue.assigned':
      return 'Assignment changed';
    case 'issue.status_changed':
      return 'Status changed';
    default: {
      const cleaned = eventType.replace(/^issue\./, '').replace(/_/g, ' ').trim();
      if (!cleaned) return 'Activity';
      return cleaned
        .split(/\s+/)
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
        .join(' ');
    }
  }
}
