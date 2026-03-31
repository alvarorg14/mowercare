import { z } from 'zod';

/** Matches API `IssueStatus` / `IssuePriority` enum names. */
export const issueStatuses = ['OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED'] as const;
export const issuePriorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;

export const issueCreateSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(500),
  description: z.string().max(20000).optional(),
  status: z.enum(issueStatuses),
  priority: z.enum(issuePriorities),
  customerLabel: z.string().max(500).optional(),
  siteLabel: z.string().max(500).optional(),
  assigneeUserId: z
    .string()
    .optional()
    .refine((s) => !s || /^[0-9a-fA-F-]{36}$/.test(s.trim()), 'Must be a valid UUID'),
});

export type IssueCreateFormValues = z.infer<typeof issueCreateSchema>;
