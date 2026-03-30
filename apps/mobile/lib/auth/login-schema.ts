import { z } from 'zod';

export const loginSchema = z.object({
  organizationId: z.string().uuid('Valid organization ID (UUID) required'),
  email: z.string().email(),
  password: z.string().min(8).max(255),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
