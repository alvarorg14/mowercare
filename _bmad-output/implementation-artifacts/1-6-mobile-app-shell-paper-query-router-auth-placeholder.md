# Story 1.6: Mobile app shell — Paper, Query, Router, auth placeholder

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **a mobile shell with design system, server-state client, and route groups**,
so that **screens can be added consistently (UX-DR1, UX-DR11)** and loading states can be honest (NFR-P2 baseline).

**Implements:** UX-DR1 (React Native Paper `Provider`, TanStack Query, single MD3 theme alongside auth context); UX-DR11 (Expo Router `(auth)` / `(app)` groups, tab-ready structure per IA); NFR-P2 baseline (Query + UI can show pending/error).

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.5** (API JWT + tenant scope; mobile will attach Bearer tokens in **1.7**); **precedes** **1.7** (real sign-in, secure token storage, API calls).

## Acceptance Criteria

1. **Given** the app starts  
   **When** the root layout renders  
   **Then** **React Native Paper** `PaperProvider` wraps the tree, **TanStack Query** `QueryClientProvider` wraps the tree, and a **single MD3** theme object is applied (primary, surfaces, baseline typography scales with outdoor-friendly minimums; stub **semantic issue status** colors on the theme or a small token map so later stories do not invent ad hoc hex).  
   **And** an **auth context** (placeholder) wraps routes so navigation can branch on “signed in” vs not — **no** real login API, token storage, or refresh logic in this story (**1.7**).

2. **Given** **TanStack Query** is configured  
   **When** defaults are set  
   **Then** **query** retries are suited to flaky field networks (e.g. **2–3** retries with bounded exponential backoff — document chosen values in code). **Mutations** may use defaults or explicit per-call options later; do not leave infinite retry loops.

3. **Given** **Expo Router** file-based routing  
   **When** the user is **unauthenticated** (placeholder)  
   **Then** only **`(auth)`** routes are reachable (e.g. a minimal placeholder sign-in / welcome screen).  
   **When** the user is **authenticated** (placeholder — e.g. explicit “Continue (placeholder)” control that sets context, **not** a real credential check)  
   **Then** **`(app)`** routes are reachable (e.g. a minimal **home** or **Issues** placeholder screen saying shell is ready).

4. **Given** API configuration  
   **When** the app reads the API base URL  
   **Then** it comes from **Expo config** / **Constants** (e.g. `expo-constants` + `extra` in `app.config.ts` or env-driven `EXPO_PUBLIC_*` for dev). **No secrets** in the repo; document in **`.env.example`** only **names** of public vars if you introduce them.

5. **Given** CI and local dev  
   **When** `npm run lint` and `npm run typecheck` run from `apps/mobile`  
   **Then** they pass after adding Router, Paper, and Query dependencies.

## Tasks / Subtasks

- [x] **Dependencies & entry** (AC: 1, 2, 5)
  - [x] Add **`expo-router`** (Expo SDK **~55** — use `npx expo install expo-router` to align native deps). Set package **`main`** to **`expo-router/entry`** per Expo Router docs; remove or bypass the **`registerRootComponent(App)`** entry in **`index.ts`** / adopt the Router entry pattern documented for your SDK.
  - [x] Add **`react-native-paper`**, **`react-native-safe-area-context`**, **`react-native-screens`** (Paper + Router peers — `expo install` where applicable).
  - [x] Add **`@tanstack/react-query`** (v5 line compatible with React 19 / RN 0.83).
  - [x] Add **`expo-constants`** if not already present for config surface (often pulled transitively; ensure explicit use in `lib/config.ts`).
- [x] **Theme & providers** (AC: 1)
  - [x] Create **`lib/theme.ts`** (or `theme/paperTheme.ts`): export one **`MD3LightTheme`** (or `adaptNavigationTheme` if you wire navigation theme later) **extended** with custom colors for **issue status** placeholders (Open / In progress / … — enough to satisfy UX-DR1 “semantic colors” stub).
  - [x] Root **`app/_layout.tsx`**: nest **`PaperProvider`** → **`QueryClientProvider`** → **`AuthProvider`** (placeholder) → **`Slot`** / **`Stack`** as required by Expo Router; pass **`theme`** to Paper.
  - [x] Instantiate **`QueryClient`** once (module scope or `useState` in root layout) so it is not recreated every render.
- [x] **Query defaults** (AC: 2)
  - [x] Configure **`defaultOptions.queries`**: **`retry`**, **`retryDelay`** (e.g. exponential with cap); optionally **`staleTime`** conservative for field use — document rationale in a short comment.
- [x] **Route groups & placeholder auth** (AC: 3)
  - [x] **`app/(auth)/_layout.tsx`** + **`app/(auth)/index.tsx`** — placeholder UI (Paper `Text`, `Button`): e.g. “MowerCare” + **“Continue (placeholder)”** that sets **`isAuthenticated = true`** via context (or router-driven state). **No** calls to `/api/v1/auth/login`.
  - [x] **`app/(app)/_layout.tsx`** — minimal stack or tabs shell (tabs optional if you only add one screen now; UX-DR11 mentions Issues / Notifications / Settings — **stub** one tab or a single screen for “App home” to prove `(app)` works).
  - [x] **`app/(app)/index.tsx`** (or `issues/index.tsx` if you mirror IA early) — placeholder content proving MD3 + Query provider reach (e.g. static text; optional **`useQuery`** with **`queryFn` disabled** or a **`queryKey`** that is never fetched — avoid requiring a running API for CI).
  - [x] Root layout or **`app/index.tsx`**: **redirect** unauthenticated users to **`/(auth)`** and authenticated users to **`/(app)`** using **`Redirect`** / **`router.replace`** from **`expo-router`** — follow current Expo Router 5.x patterns for SDK 55.
- [x] **API base URL module** (AC: 4)
  - [x] **`lib/config.ts`**: read **`Constants.expoConfig?.extra?.apiBaseUrl`** (and/or **`process.env.EXPO_PUBLIC_API_BASE_URL`**) with a **dev-safe default** like `http://localhost:8080` or empty string — **document** that real devices need machine LAN IP / tunnel; **no** secrets.
  - [x] Update **`app.config.ts`** (migrate from **`app.json`** if needed for `extra`) or use **`app.json` + `expo.extra`** — keep **`apps/mobile`** buildable in CI.
  - [x] Extend **root** **`.env.example`** with optional **`EXPO_PUBLIC_API_BASE_URL=`** name only if you introduce it.
- [x] **Cleanup** (AC: 5)
  - [x] Remove obsolete **`App.tsx`** default screen if fully replaced by **`app/`** tree, or leave a thin unused file only if required — prefer **delete** + Router-only entry to avoid dual entry confusion.
  - [x] **`npm run lint`** + **`npm run typecheck`** green; fix **`eslint`** config if Router/Paper need ignores (minimize — prefer correct `tsconfig` paths).

### Review Findings

- [x] [Review][Patch] Enforce auth on route groups — `app/(app)/_layout.tsx` and `app/(auth)/_layout.tsx` do not check `isAuthenticated`; only `app/index.tsx` redirects. Deep links or navigation to `/(app)` while signed out (or `/(auth)` while signed in) can bypass the entry gate. **AC3** — add `useAuth` + `<Redirect href="…"/>` (or equivalent) in each group layout so unauthenticated users cannot reach `(app)` and authenticated users are not left on `(auth)` without going through `/`. [`app/(app)/_layout.tsx`, `app/(auth)/_layout.tsx`] — fixed: group layouts redirect on auth mismatch.

- [x] [Review][Patch] Surface query error state on shell demo — `(app)/index.tsx` `shellQuery` shows pending/data only; `isError` / `error` not displayed. **NFR-P2** baseline for honest loading/error — add a minimal error line or retry affordance if `isError`. [`apps/mobile/app/(app)/index.tsx`] — fixed: main line shows `Error: …` when `isError`.

- [x] [Review][Patch] Align screen backgrounds with MD3 theme — `(auth)/index.tsx` and `(app)/index.tsx` hardcode `#F5F5F5` while `paperTheme.colors.background` is the same value; use `useTheme()` from Paper (or import `paperTheme`) so screens stay tied to the single theme object. **UX-DR1** [`apps/mobile/app/(auth)/index.tsx`, `apps/mobile/app/(app)/index.tsx`] — fixed: `useTheme().colors.background` on `SafeAreaView`.

- [x] [Review][Patch] Deduplicate default API base URL string — `http://localhost:8080` appears in both `app.config.ts` (`apiBaseUrl` const) and `lib/config.ts` fallback; extract one exported default (e.g. from `lib/config.ts` or `lib/theme.ts`-adjacent constants) and import in `app.config.ts` if feasible, or document single owner. **AC4** [`apps/mobile/app.config.ts`, `apps/mobile/lib/config.ts`] — fixed: `DEFAULT_API_BASE_URL` in `lib/config.ts`, imported by `app.config.ts`.

## Dev Notes

### Scope boundaries

- **In scope:** Expo Router shell; Paper + single theme; TanStack Query client + retries; **`(auth)` / `(app)`** groups; **placeholder** auth state only; public API base URL wiring surface; NFR-P2-friendly loading capability via Query defaults.
- **Out of scope:** Real JWT storage, login/logout API, **`lib/api`** Bearer injection, **`TenantScope`** calls, secure storage, refresh rotation, CORS (API concern when mobile hits device/simulator — document localhost vs LAN only), push, React Hook Form + Zod (later), full tab IA with Issues/Notifications/Settings content.

### Architecture compliance

- **Navigation:** **Expo Router**; route groups **`(auth)/`** vs **`(app)/`**. [Source: `architecture.md` — Frontend architecture (Expo), Project Structure `app/`]
- **Server state:** **TanStack Query**; retries for flaky networks. [Source: `architecture.md` — Frontend architecture; Process patterns — Loading & errors]
- **API base URL:** **`app.config` / `expo-constants`**; **no secrets in repo**. [Source: `architecture.md` — Structure patterns — Mobile — Env]
- **Network boundary later:** All HTTP through **`lib/api`** + Query — for **1.6**, only add **`lib/config.ts`** (and optionally a **`queryClient.ts`**); **do not** duplicate fetch wrappers unnecessarily.
- **UX:** Root layout wraps Paper + Query + auth context; single MD3 theme; semantic issue colors stub. [Source: `ux-design-specification.md` — Primary UI foundation; Theming]

### Technical requirements

| Area | Requirement |
|------|-------------|
| Expo | **SDK ~55** (existing `package.json`); align Router/Paper versions with `expo install` |
| React | **19.x** / RN **0.83.x** per current `apps/mobile` |
| Paper | **MD3** theme; `PaperProvider` at root |
| Query | **@tanstack/react-query** v5; `QueryClient` with explicit defaults |
| Auth | **Placeholder only** — React context or lightweight store; **1.7** replaces with secure storage + API |

### Library / framework requirements

| Package | Role |
|---------|------|
| `expo-router` | File routes, `Redirect`, groups `(auth)` / `(app)` |
| `react-native-paper` | MD3 components + theme |
| `@tanstack/react-query` | Server state, retries |
| `expo-constants` | Read `extra` / env for API base URL |
| `react-native-safe-area-context` | Layout for Paper/Router |

Use **`npx expo install <pkg>`** for Expo-managed packages to match SDK.

### File structure requirements (target)

Align with [Source: `architecture.md` — Mobile application `apps/mobile`]:

```
apps/mobile/
  app/
    _layout.tsx              # Root: Paper + Query + Auth + navigation container
    index.tsx                # Redirect to (auth) or (app)
    (auth)/
      _layout.tsx
      index.tsx              # Placeholder sign-in / continue
    (app)/
      _layout.tsx            # Stack or tabs
      index.tsx              # Placeholder home / issues entry
  lib/
    config.ts                # apiBaseUrl from Constants / env
    theme.ts                 # MD3 + semantic stubs
    auth-context.tsx         # Placeholder AuthProvider (or under lib/auth/)
  app.config.ts              # optional: extra.apiBaseUrl — if keeping app.json, use "expo.extra"
```

### Testing requirements

- **Manual:** `npx expo start` — cold start shows **`(auth)`**; after placeholder continue, **`(app)`** shows.
- **CI:** Existing workflow runs **`npm ci`**, **`npm run lint`**, **`npm run typecheck`** — must remain green.
- **No** new mobile E2E gate required for **1.6** unless you already have Detox/Maestro (you do not in **1.1**).

### Previous story intelligence

- **1.5** delivers **JWT** on **`Authorization: Bearer`** for **`/api/v1/**`** (except auth/bootstrap/docs). Mobile does **not** call protected routes in **1.6**; **1.7** will attach tokens and handle Problem Details (**UX-DR9**).
- **1.5** **`TenantScopeController`** path pattern: **`/api/v1/organizations/{organizationId}/...`** — mobile screens will use **`organizationId`** from token/session later; **not** in shell story.

### Git intelligence

- Recent work is **API-heavy** (1.3–1.5); **`apps/mobile`** is still **1.1 blank template** (`App.tsx` only). **1.6** is the **first** structural migration to Router + Paper + Query — expect a larger **`package.json`** / lockfile diff and possible **`app.json` → `app.config.ts`** migration.

### Latest tech notes (verify at implementation time)

- **Expo SDK 55** + **Expo Router 5.x**: follow [Expo Router docs](https://docs.expo.dev/router/introduction/) for **`plugin: "expo-router"`** in app config, **scheme**, and **typed routes** optional.
- **React Native Paper** with **React 19**: use a current **7.x** Paper line compatible with your SDK (`expo install react-native-paper`).
- **TanStack Query v5**: `QueryClient` default **`throwOnError`** / **`gcTime`** (formerly `cacheTime`) — use current v5 API names in TypeScript.

### Project context reference

- **`docs/project-context.md`** — not present; optional **Generate Project Context** skill after mobile patterns stabilize.

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.6 (acceptance criteria)
- `_bmad-output/planning-artifacts/architecture.md` — § Frontend architecture (Expo), § Mobile `app/` layout, § Env
- `_bmad-output/planning-artifacts/ux-design-specification.md` — Paper, theme, UX-DR1 / UX-DR11 mapping in Requirements Inventory
- `_bmad-output/implementation-artifacts/1-1-initialize-monorepo-from-expo-and-spring-boot-starters.md` — blank template baseline
- `_bmad-output/implementation-artifacts/1-5-global-api-security-and-tenant-context-on-protected-routes.md` — JWT + tenant scope for later mobile integration

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — bmad-dev-story workflow

### Debug Log References

- **React** bumped to **19.2.4** so `react-dom` peers pulled by **expo-router** (web stack) resolve cleanly with npm.
- **Entry:** `package.json` **`main`**: **`expo-router/entry`**; removed **`index.ts`** / **`App.tsx`**.

### Completion Notes List

- Added **Expo Router** (`(auth)` / `(app)`), **Paper** (`PaperProvider` + MD3 theme + **`issueStatusTokens`**), **TanStack Query** (`createQueryClient` with 3 retries, capped exponential backoff, `staleTime` 60s, mutations `retry: 0`).
- Placeholder **auth** via **`lib/auth-context.tsx`**; **Continue** / **Sign out** use **`router.replace('/')`** so **`app/index.tsx`** **`Redirect`** picks the correct group.
- **`lib/config.ts`** + **`app.config.ts`** `extra.apiBaseUrl`; **`EXPO_PUBLIC_API_BASE_URL`** in root **`.env.example`**.
- **`npm run lint`** and **`npm run typecheck`** pass; **`mvn verify`** (API) pass for regression.
- Automated mobile unit tests not added (story defers E2E; no Jest task in spec).

### File List

- `README.md`
- `.env.example`
- `apps/mobile/package.json`
- `apps/mobile/package-lock.json`
- `apps/mobile/app.config.ts`
- `apps/mobile/app/_layout.tsx`
- `apps/mobile/app/index.tsx`
- `apps/mobile/app/(auth)/_layout.tsx`
- `apps/mobile/app/(auth)/index.tsx`
- `apps/mobile/app/(app)/_layout.tsx`
- `apps/mobile/app/(app)/index.tsx`
- `apps/mobile/lib/auth-context.tsx`
- `apps/mobile/lib/config.ts`
- `apps/mobile/lib/queryClient.ts`
- `apps/mobile/lib/theme.ts`
- `apps/mobile/App.tsx` (deleted)
- `apps/mobile/index.ts` (deleted)
- `apps/mobile/app.json` (deleted)

### Change Log

- 2026-03-30 — Story 1.6: Expo Router shell, Paper MD3 theme, TanStack Query defaults, placeholder auth, `app.config` API base URL, remove legacy entry files.

## Story completion status

- **Status:** done  
- **Note:** Implementation complete; code review patches applied (group auth redirects, query error line, theme backgrounds, `DEFAULT_API_BASE_URL`); mobile lint + typecheck green.
