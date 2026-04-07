import {
  buildIssueListQueryString,
  defaultIssueListParams,
  type IssueListParams,
} from '../lib/issue-api';

describe('issue list query helpers', () => {
  describe('defaultIssueListParams', () => {
    it('returns open scope with defaults', () => {
      expect(defaultIssueListParams()).toEqual({
        scope: 'open',
        statuses: [],
        priorities: [],
        sort: 'updatedAt',
        direction: 'desc',
      });
    });

    it('honors explicit scope', () => {
      expect(defaultIssueListParams('mine').scope).toBe('mine');
    });
  });

  describe('buildIssueListQueryString', () => {
    it('encodes scope sort and direction', () => {
      const params: IssueListParams = {
        scope: 'all',
        statuses: [],
        priorities: [],
        sort: 'createdAt',
        direction: 'asc',
      };
      const q = buildIssueListQueryString(params);
      expect(q).toContain('scope=all');
      expect(q).toContain('sort=createdAt');
      expect(q).toContain('direction=asc');
    });

    it('repeats status and priority query params', () => {
      const params: IssueListParams = {
        scope: 'open',
        statuses: ['OPEN', 'IN_PROGRESS'],
        priorities: ['HIGH'],
        sort: 'updatedAt',
        direction: 'desc',
      };
      const q = buildIssueListQueryString(params);
      expect(q).toMatch(/status=OPEN/);
      expect(q).toMatch(/status=IN_PROGRESS/);
      expect(q).toContain('priority=HIGH');
    });
  });
});
