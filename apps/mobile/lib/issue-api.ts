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
