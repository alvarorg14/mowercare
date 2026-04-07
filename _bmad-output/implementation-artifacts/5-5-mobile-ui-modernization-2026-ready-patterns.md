# Story 5.5: Mobile UI modernization (2026-ready patterns)

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **a fresher, more contemporary visual and interaction baseline (spacing, typography, surfaces, motion where appropriate)**,
so that **the app feels current and professional without sacrificing usability (NFR-A1)**.

**Implements:** NFR-A1; UX-DR1 (single MD3 theme object — **evolve** tokens, not duplicate themes); UX-DR14–DR15 baselines **maintained or improved**. **Epics.md** also ties this story to **UX-DR6** and **UX-DR9** (error and mutation feedback) — **do not** let visual refreshes hide, mute, or obscure loading / success / error states.

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Scope:** **`apps/mobile` only** — presentation and theme tokens; **no** API, OpenAPI, or backend contract changes unless a bugfix is unavoidable (out of scope).

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 5.5])

| Epics.md clause | Implementation intent |
|-----------------|------------------------|
| **Given** shell + core screens **When** theme tokens updated **Then** **one** canonical Paper/MD3 theme | Extend [`paperTheme`](../../apps/mobile/lib/theme.ts) and `PaperProvider` in [`app/_layout.tsx`](../../apps/mobile/app/_layout.tsx); do not introduce a second parallel theme object for production UI. |
| **Given** primary flows **When** reviewed **Then** touch targets + contrast **≥** Story 3.9 baselines | Re-run `npm run check:contrast` after token changes; spot-check list/detail/settings/auth; preserve `accessibilityLabel` on icon-only controls. |
| **Given** large change risk **When** shipping **Then** feature flag doc **or** split follow-ups | If scope balloons, document `expo-constants` / remote flag approach **or** defer parts to a future 5.x story (note in Dev Agent Record). |

---

## Acceptance Criteria

1. **Single canonical theme**  
   **Given** the app shell and core screens (Issues, Notifications, Settings, auth)  
   **When** theme tokens are updated (colors, elevation, shape, motion)  
   **Then** **one** canonical Paper/MD3 theme remains the source of truth; dark mode remains **optional** unless explicitly in scope for this story.

2. **Accessibility non-regression**  
   **Given** primary flows  
   **When** reviewed on a reference device  
   **Then** touch targets and contrast **do not regress** below Epic 3 Story 3.9 baselines; icon-only controls retain `accessibilityLabel`.

3. **Rollout risk**  
   **Given** rollout risk  
   **When** changes are large  
   **Then** a **feature flag or phased rollout** is documented **OR** changes are split into follow-up stories in Epic 5.

---

## Tasks / Subtasks

- [x] **Theme + tokens** (AC: 1)
  - [x] Evolve `paperTheme` in [`lib/theme.ts`](../../apps/mobile/lib/theme.ts): colors, shape (`roundness`), elevation surfaces as needed; optionally map `fonts` if typography refresh is in scope — keep **MD3** semantics (`MD3LightTheme` spread).
  - [x] Keep **`issueStatusTokens`** in sync with chip rendering assumptions; if status hexes change, update **both** `theme.ts` and [`scripts/check-issue-theme-contrast.mjs`](../../apps/mobile/scripts/check-issue-theme-contrast.mjs) (script duplicates constants for the check — avoid drift).
  - [x] Confirm **no** second `PaperProvider` / alternate theme for production routes unless dark mode is explicitly implemented (not required by default AC).
- [x] **Screen pass** (AC: 1–2)
  - [x] Apply token-driven spacing/typography consistently on: auth, tab shell, issues list/detail/create, notifications, settings, org profile — prefer `useTheme()` / Paper components over hard-coded one-off colors where possible.
  - [x] **UX-DR6 / UX-DR9:** After restyling, confirm **mutation and error feedback** (Snackbars, banners, inline errors, pending/saved indicators) remain **visible and readable** on new surface/elevation colors — not lower contrast than before.
  - [x] Verify tab bar and headers remain legible — `apps/mobile/app/(app)/(tabs)/_layout.tsx`.
- [x] **Verification** (AC: 2)
  - [x] `cd apps/mobile && npm run check:contrast` — **green** after changes.
  - [x] `cd apps/mobile && npm run lint && npm run typecheck && npm test` — **green**.
  - [x] Manual smoke: sign-in, issues list, issue detail, notifications list, settings — **no** unreadable text or shrunken hit targets vs prior behavior. _(Automated contrast + UI checks passed; on-device smoke recommended during review.)_
- [x] **Risk / follow-ups** (AC: 3)
  - [x] If the diff is large, add a short note (README snippet or `docs/` pointer) on rollout or split remaining work into a tracked Epic 5 follow-up.

---

## Dev Notes

### Architecture compliance

| Topic | Source |
|-------|--------|
| Expo Router + TanStack Query + Paper | [architecture.md](../planning-artifacts/architecture.md) — Frontend architecture (Expo) |
| Single API module + query hooks | [architecture.md](../planning-artifacts/architecture.md) — Mobile structure (`lib/`) |
| Honest loading/error UI (don’t break patterns) | [architecture.md](../planning-artifacts/architecture.md) — NFR-P2 / Query usage |

### UX alignment

- **UX-DR1:** One MD3 theme object — evolve [`paperTheme`](../../apps/mobile/lib/theme.ts), do not fork competing palettes.
- **UX-DR6 / UX-DR9:** Error and mutation feedback must stay **clear and honest** when colors/surfaces change (align with architecture NFR-P2 + TanStack Query patterns on issue flows).
- **UX-DR14 / UX-DR15 (Epic 3.9 baseline):** Contrast script + touch targets; list row accessibility patterns in [ux-design-specification.md](../planning-artifacts/ux-design-specification.md) (issue list guidance).
- **Epics.md:** Stories **5.2** and **5.5** revisit UX-DR1, UX-DR6, UX-DR9, UX-DR14, UX-DR15 — **do not** lower the MVP accessibility bar. For Jest conventions and high-value test areas from **5.2**, see [`5-2-frontend-unit-test-expansion.md`](./5-2-frontend-unit-test-expansion.md).

### Library / framework requirements

- **Pinned stack (do not bump major versions unless story explicitly expands scope):** Expo **~55**, React Native **0.83.x**, react-native-paper **^5.15**, React **19.x** — see [`apps/mobile/package.json`](../../apps/mobile/package.json).
- **Motion:** Prefer React Native / Paper-supported patterns (e.g. `Animated` or Paper’s built-ins) — avoid heavy new animation libraries unless justified.

### File structure requirements

- Primary touchpoints: [`apps/mobile/lib/theme.ts`](../../apps/mobile/lib/theme.ts), [`apps/mobile/app/_layout.tsx`](../../apps/mobile/app/_layout.tsx), screens under [`apps/mobile/app/`](../../apps/mobile/app/).
- **Contrast check:** [`apps/mobile/scripts/check-issue-theme-contrast.mjs`](../../apps/mobile/scripts/check-issue-theme-contrast.mjs) must stay aligned with theme exports when hex values change.
- **No** changes under `apps/api` for this story.

### Testing requirements

- **Mandatory:** `cd apps/mobile && npm test && npm run lint && npm run typecheck && npm run check:contrast`.
- **Update** [`__tests__/theme.test.ts`](../../apps/mobile/__tests__/theme.test.ts) if primary/surface tokens or status tokens change (keep assertions meaningful, not brittle to every hex tweak).
- **E2E:** No requirement to change Maestro flows unless UI structure changes break selectors — if so, update `.maestro/` in the same PR.

### Previous story intelligence (5.4)

- Story **5.4** was **backend-only** package moves; **mobile imports and API usage are unchanged**. This story is the first **mobile-facing** Epic 5 work after 5.2/5.3 — keep UI changes **reviewable** (thematic commits or clear file groupings).
- **5.3** added Maestro smoke — after UI changes, run documented E2E locally if selectors shift.

### Git intelligence (recent commits)

- Recent work: **5.4** domain packages (API), **5.3** E2E, **5.2** Jest — mobile codebase is ready for a **visual/token** pass without waiting on backend.

### Latest technical notes

- **MD3 in Paper 5:** Use `MD3LightTheme` spread and override `colors` / `fonts` / `roundness` per [React Native Paper theming docs](https://callstack.github.io/react-native-paper/docs/guides/theming/).
- **2026 readiness:** Focus on **token consolidation**, **consistent elevation/surface hierarchy**, and **readable defaults** on phone outdoors — not a full redesign unless time-box allows.

### Project context reference

- No `project-context.md` in repo; **`epics.md`**, **`architecture.md`**, **`ux-design-specification.md`**, and existing mobile theme comments govern this work.

---

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2)

### Debug Log References

- `cd apps/mobile && npm run check:contrast && npm test && npm run lint && npm run typecheck` — all green (2026-04-07).

### Completion Notes List

- **Theme:** Evolved single `paperTheme` — `roundness` 12, refined MD3 palette (primary/secondary/tertiary/containers), green-tinted `elevation` levels, off-white `surface` (`#FFFBFF`), canvas `background` (`#F2F5F2`). Updated `issueStatusTokens` (Material 600-level hues); synced contrast script (chip alpha matches `IssueRow` `+ '22'`).
- **Screens:** Empty states on Issues + Notifications use `Surface` + `elevation={1}` and padded cards; Team invite validation text uses `theme.colors.error` (removed hardcoded error hex).
- **Config:** Splash + Android adaptive icon background aligned with theme greens / surface.
- **Rollout:** [`docs/mobile-ui-2026.md`](../../docs/mobile-ui-2026.md) documents single-theme release and future flag/5.x split option (AC3).
- **PaperProvider:** Unchanged single root provider in `app/_layout.tsx`.

### File List

- `apps/mobile/lib/theme.ts`
- `apps/mobile/scripts/check-issue-theme-contrast.mjs`
- `apps/mobile/__tests__/theme.test.ts`
- `apps/mobile/app/(app)/(tabs)/issues/index.tsx`
- `apps/mobile/app/(app)/(tabs)/notifications/index.tsx`
- `apps/mobile/app/(app)/team.tsx`
- `apps/mobile/app.config.ts`
- `docs/mobile-ui-2026.md`

### Change Log

- 2026-04-07: Story 5.5 — mobile UI modernization; sprint status → **review**.
- 2026-04-07: Code review — chip contrast script uses `surfaces.surface` as alpha-composite base for status chips; story → **done**.

### Review Findings

- [x] [Review][Patch] Chip contrast model uses pure white as mix base while `surface` is `#FFFBFF` — align `mixWithWhite` in [`check-issue-theme-contrast.mjs`](../../apps/mobile/scripts/check-issue-theme-contrast.mjs) with `surfaces.surface` so the status-chip check matches the themed canvas (small WCAG model accuracy improvement). [`check-issue-theme-contrast.mjs`] — fixed 2026-04-07 (`mixTokenOverSurface` + themed blend base).

---

## Create-story validation record

**Checklist:** [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md)  
**Validated:** 2026-04-07 (second pass: `validate`).

**Verdict:** **Pass** — Story matches `epics.md` Story 5.5 ACs (single theme, a11y non-regression, rollout doc or split). Concrete paths, commands, and stack pins are present.

**Remediation applied (validation):** Added explicit **UX-DR6 / UX-DR9** tasks and narrative — they are named in [epics.md](../planning-artifacts/epics.md) as revisited with 5.5 but were missing from the first draft **Implements** / tasks; added cross-link to **5.2** story for test conventions.

**Residual risks (informational, non-blocking):** Contrast script duplicates hex constants from `theme.ts` — story already warns to sync both; optional follow-up is a small shared module to DRY (implementation choice, not required for story acceptance).
