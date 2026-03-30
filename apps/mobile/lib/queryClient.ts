import { QueryClient } from '@tanstack/react-query';

/**
 * Field / flaky networks: 3 query retries, exponential backoff capped at 30s.
 * Mutations default to 0 retries to avoid duplicate writes (explicit per-call later).
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: 3,
        retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30_000),
        staleTime: 60_000,
      },
      mutations: {
        retry: 0,
      },
    },
  });
}

/** Singleton for sign-out cache clear and root layout. */
export const queryClient = createQueryClient();
