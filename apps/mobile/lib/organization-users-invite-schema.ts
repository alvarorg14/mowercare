import { z } from 'zod';

/** Aligns with API `CreateEmployeeUserRequest` — omit `initialPassword` when inviting. */
export const inviteUserFormSchema = z
  .object({
    email: z.string().min(1, 'Email is required').email('Enter a valid email'),
    role: z.enum(['ADMIN', 'TECHNICIAN']),
    initialPassword: z.string().optional(),
  })
  .refine(
    (data) => {
      const p = data.initialPassword?.trim();
      if (!p) return true;
      return p.length >= 8;
    },
    { message: 'Password must be at least 8 characters', path: ['initialPassword'] },
  );

export type InviteUserFormValues = z.infer<typeof inviteUserFormSchema>;
