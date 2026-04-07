# Mobile UI refresh (Story 5.5)

## Rollout

- **Single theme:** All presentation uses `paperTheme` in `apps/mobile/lib/theme.ts` via `PaperProvider` in `app/_layout.tsx`. There is no parallel theme or feature flag for this release; changes ship together.
- **Future splits:** If a larger visual experiment is needed later, prefer a new Epic 5 story (or `expo-constants` / remote config) so list/detail can be toggled without duplicating theme objects.

## Verification

From `apps/mobile`: `npm run lint`, `npm run typecheck`, `npm test`, `npm run check:contrast`.
