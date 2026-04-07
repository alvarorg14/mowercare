import { issueRowStatusColor, issueRowStatusLabel } from '../components/IssueRow';

describe('IssueRow helpers', () => {
  it('issueRowStatusColor maps API statuses', () => {
    expect(issueRowStatusColor('OPEN')).toMatch(/^#/);
    expect(issueRowStatusColor('IN_PROGRESS')).toMatch(/^#/);
    expect(issueRowStatusColor('WAITING')).toMatch(/^#/);
    expect(issueRowStatusColor('RESOLVED')).toMatch(/^#/);
    expect(issueRowStatusColor('UNKNOWN')).toMatch(/^#/);
  });

  it('issueRowStatusLabel replaces underscores', () => {
    expect(issueRowStatusLabel('IN_PROGRESS')).toBe('IN PROGRESS');
  });
});
