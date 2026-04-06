import { summarizeChangeEvent } from '../components/IssueActivityTimeline';
import type { IssueChangeEventItem } from '../lib/issue-api';

function base(overrides: Partial<IssueChangeEventItem>): IssueChangeEventItem {
  return {
    id: 'e1',
    occurredAt: '2026-01-01T00:00:00Z',
    changeType: 'CREATED',
    actorUserId: 'u1',
    actorLabel: 'Tester',
    oldValue: null,
    newValue: null,
    oldAssigneeLabel: null,
    newAssigneeLabel: null,
    ...overrides,
  };
}

describe('summarizeChangeEvent', () => {
  it('describes creation', () => {
    expect(summarizeChangeEvent(base({ changeType: 'CREATED' }))).toBe('Issue created');
  });

  it('describes status change with humanized labels', () => {
    expect(
      summarizeChangeEvent(
        base({
          changeType: 'STATUS_CHANGED',
          oldValue: 'OPEN',
          newValue: 'IN_PROGRESS',
        }),
      ),
    ).toBe('Status: OPEN → IN PROGRESS');
  });

  it('describes assignee change including unassigned', () => {
    expect(
      summarizeChangeEvent(
        base({
          changeType: 'ASSIGNEE_CHANGED',
          oldAssigneeLabel: null,
          newAssigneeLabel: 'alice@example.com',
        }),
      ),
    ).toBe('Assignee: Unassigned → alice@example.com');
  });

  it('falls back to raw change type for unknown kinds', () => {
    expect(summarizeChangeEvent(base({ changeType: 'FUTURE_EVENT' }))).toBe('FUTURE_EVENT');
  });
});
