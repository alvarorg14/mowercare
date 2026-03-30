import { z } from 'zod';

/** Aligns with API `OrganizationProfilePatchRequest`: not blank, max 255. */
export const orgProfileSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(255, 'Max 255 characters'),
});

export type OrgProfileFormValues = z.infer<typeof orgProfileSchema>;
