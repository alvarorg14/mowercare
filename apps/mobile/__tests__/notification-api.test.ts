import {
  formatNotificationEventType,
  notificationListQueryKey,
} from '../lib/notification-api';

describe('notificationListQueryKey', () => {
  it('returns stable query key tuple', () => {
    expect(notificationListQueryKey('org-1', 1, 20)).toEqual(['notifications', 'org-1', 1, 20]);
  });
});

describe('formatNotificationEventType', () => {
  it('maps known taxonomy strings', () => {
    expect(formatNotificationEventType('issue.created')).toBe('Issue created');
    expect(formatNotificationEventType('issue.assigned')).toBe('Assignment changed');
    expect(formatNotificationEventType('issue.status_changed')).toBe('Status changed');
  });

  it('title-cases unknown issue.* types', () => {
    expect(formatNotificationEventType('issue.foo_bar')).toBe('Foo Bar');
  });

  it('returns Activity for empty after strip', () => {
    expect(formatNotificationEventType('issue.')).toBe('Activity');
  });
});
