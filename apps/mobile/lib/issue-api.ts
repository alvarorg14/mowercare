import { authenticatedFetchJson } from './api';
import { getSessionOrganizationId } from './auth/session';

export type IssueCreated = {
  id: string;
  title: string;
  status: string;
  priority: string;
  description?: string | null;
  customerLabel?: string | null;
  siteLabel?: string | null;
  assigneeUserId?: string | null;
  createdAt: string;
};

export type IssueCreatePayload = {
  title: string;
  description?: string | null;
  status: string;
  priority: string;
  customerLabel?: string | null;
  siteLabel?: string | null;
  assigneeUserId?: string | null;
};

export function createIssue(body: IssueCreatePayload): Promise<IssueCreated> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');

  const payload: Record<string, unknown> = {
    title: body.title,
    status: body.status,
    priority: body.priority,
  };
  if (body.description != null && String(body.description).trim() !== '') {
    payload.description = body.description;
  }
  if (body.customerLabel != null && String(body.customerLabel).trim() !== '') {
    payload.customerLabel = body.customerLabel.trim();
  }
  if (body.siteLabel != null && String(body.siteLabel).trim() !== '') {
    payload.siteLabel = body.siteLabel.trim();
  }
  if (body.assigneeUserId != null && String(body.assigneeUserId).trim() !== '') {
    payload.assigneeUserId = body.assigneeUserId.trim();
  }

  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/issues`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/** Query `scope` values — matches API (`open` default). */
export type IssueListScope = 'open' | 'all' | 'mine';

export type IssueListItem = {
  id: string;
  title: string;
  status: string;
  priority: string;
  customerLabel?: string | null;
  siteLabel?: string | null;
  assigneeUserId?: string | null;
  assigneeLabel?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type IssueListResult = { items: IssueListItem[] };

/** Full issue payload from GET by id (align with OpenAPI / `IssueDetailResponse`). */
export type IssueDetail = {
  id: string;
  title: string;
  status: string;
  priority: string;
  description?: string | null;
  customerLabel?: string | null;
  siteLabel?: string | null;
  assigneeUserId?: string | null;
  assigneeLabel?: string | null;
  createdAt: string;
  updatedAt: string;
};

export function getIssue(issueId: string): Promise<IssueDetail> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/issues/${issueId}`);
}

/** Partial update — only include fields that change (camelCase, matches API). */
export type IssueUpdatePayload = {
  title?: string;
  description?: string | null;
  status?: string;
  priority?: string;
  assigneeUserId?: string | null;
  customerLabel?: string | null;
  siteLabel?: string | null;
};

export function patchIssue(issueId: string, body: IssueUpdatePayload): Promise<IssueDetail> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/issues/${issueId}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function listIssues(scope: IssueListScope): Promise<IssueListResult> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  const q = new URLSearchParams({ scope });
  return authenticatedFetchJson(`/api/v1/organizations/${orgId}/issues?${q.toString()}`);
}

/** Align with `IssueChangeEventItemResponse` / `IssueChangeEventsResponse`. */
export type IssueChangeEventItem = {
  id: string;
  occurredAt: string;
  changeType: string;
  actorUserId: string;
  actorLabel: string | null;
  oldValue: string | null;
  newValue: string | null;
  oldAssigneeLabel: string | null;
  newAssigneeLabel: string | null;
};

export type IssueChangeEventsResult = {
  items: IssueChangeEventItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export function listIssueChangeEvents(
  issueId: string,
  page = 0,
  size = 50,
): Promise<IssueChangeEventsResult> {
  const orgId = getSessionOrganizationId();
  if (!orgId) throw new Error('Missing organization id');
  const q = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return authenticatedFetchJson(
    `/api/v1/organizations/${orgId}/issues/${issueId}/change-events?${q.toString()}`,
  );
}
