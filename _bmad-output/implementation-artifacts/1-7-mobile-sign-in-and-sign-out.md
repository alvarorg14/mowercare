# Story 1.7: Mobile sign-in and sign-out

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As an **employee**,
I want **to sign in and sign out on my phone**,
so that **I can access MowerCare safely (FR4, FR5)**.

**Implements:** FR4, FR5; UX-DR6, UX-DR9, UX-DR12, UX-DR14 (per epics mapping — auth flows: primary actions, honest errors, MutationFeedback, accessible labels).

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.6** (Router shell, Paper, Query, placeholder auth); **precedes** **1.8** (organization profile API from authenticated session).

## Acceptance Criteria

1. **Given** sign-in form  
   **When** user submits **valid** credentials for an existing org user  
   **Then** the app calls **`POST /api/v1/auth/login`** with **`organizationId`**, **`email`**, **`password`** (camelCase JSON per API); on success, **access token** is held **in memory** (auth context / module state — not persisted to disk) and **refresh token** is stored with **secure storage** (`expo-secure-store` or equivalent Expo-supported secure persistence per architecture). **TanStack Query** **`useMutation`** drives submit: show **pending** (disabled primary + loading affordance), then **success** (navigation to `(app)`) or **error** with **Retry** where the failure is recoverable (network).  
   **And** **MutationFeedback** pattern applies: user always sees **honest** pending vs outcome (NFR-P2 / UX MutationFeedback — [Source: `ux-design-specification.md` — Component Strategy → MutationFeedback; Feedback Patterns]).

2. **Given** a **signed-in** session  
   **When** user chooses **sign out**  
   **Then** the app calls **`POST /api/v1/auth/logout`** with the current **refresh token** in body (**`LogoutRequest`**: `{ "refreshToken": "..." }`**) when possible; **clears** secure-stored refresh + in-memory access; **clears TanStack Query cache** (avoid stale user/org data); user returns to **`(auth)`** flow (same redirect behavior as today via root/`router.replace`).

3. **Given** API errors  
   **When** the API returns **Problem Details** (`Content-Type: application/problem+json`)  
   **Then** user-visible copy reflects **`title`**, **`detail`**, and stable **`code`** where helpful — **not** a single generic “Error” with no context. Map **field validation (400)** to inputs when the API exposes field constraints; otherwise show inline/banner message + optional **Retry** for transient cases.

4. **Given** a **stored refresh token** (e.g. after previous login)  
   **When** the app **cold-starts**  
   **Then** the app **attempts session restore**: read refresh from SecureStore → **`POST /api/v1/auth/refresh`** → on success, store new tokens and treat user as **signed in**; on **401** (`AUTH_REFRESH_INVALID`), **clear** storage and show **`(auth)`** — no silent stuck state.

5. **Given** an **authenticated** client (access token in memory)  
   **When** a **Bearer** request returns **401** (`AUTH_INVALID_TOKEN` from a protected route)  
   **Then** the client performs **at most one** **`POST /api/v1/auth/refresh`** (bounded backoff optional), **retries the original request once** with the new access token; if refresh fails, **clear** session and route to sign-in with clear copy [Source: `architecture.md` — Process patterns → Auth]. **Concrete probe (recommended in 1.7):** after successful login, call **`GET /api/v1/organizations/{organizationId}/tenant-scope`** with **`Authorization: Bearer <accessToken>`** and **`organizationId`** matching the signed-in org (see **`TenantScopeController`**) — **200** confirms JWT + tenant boundary; forces **`lib/api`** to implement Bearer + 401→refresh before **1.8**.

## Tasks / Subtasks

- [x] **Dependencies** (AC: 1, 2, 4, 5)
  - [x] Add **`expo-secure-store`** via `npx expo install expo-secure-store` (align with SDK 55).
  - [x] Add **`react-hook-form`**, **`@hookform/resolvers`**, **`zod`** for validated login form (architecture: RHF + Zod; OpenAPI-aligned shapes).
- [x] **`lib/api` — HTTP + Problem Details** (AC: 1, 3, 4, 5)
  - [x] Create **`lib/api.ts`** (or `lib/api/client.ts` + `lib/api/errors.ts`): **base URL** from existing **`lib/config.ts`**; **`fetch`** wrapper that sets **`Content-Type: application/json`**, parses JSON, and on **non-OK** responses parses **Problem Details** — **`code` is a top-level JSON field** (`$.code`, same as Spring **`AuthIT`** assertions), not nested under `properties` — map `type`, `title`, `status`, `detail`, `instance`, **`code`** into a **typed error** for UI — **never** log tokens or passwords.
  - [x] **Bearer + 401 retry:** central helper that attaches **`Authorization: Bearer <accessToken>`** using **`tokenType`** from **`TokenResponse`** (expect **`Bearer`**) and implements **401 → refresh once → retry once** for protected calls (AC **5**). Login/refresh/logout calls themselves run **without** Bearer.
  - [x] **Probe:** after login (and optionally after session restore), call **`GET /api/v1/organizations/{organizationId}/tenant-scope`** to validate end-to-end JWT usage — [Source: `TenantScopeController.java`].
- [x] **Token storage module** (AC: 1, 2)
  - [x] **`lib/auth-storage.ts`** (or under `lib/auth/`): **get/set/delete** refresh token in **SecureStore**; access token **only** in React state or a small in-memory module behind **`AuthProvider`**.
  - [x] Define **stable SecureStore keys** (namespaced, e.g. `mowercare.refreshToken`).
- [x] **Auth API functions** (AC: 1, 2)
  - [x] **`login`**: `POST /api/v1/auth/login` → map response to **`TokenResponse`**: `accessToken`, `refreshToken`, `tokenType`, `expiresIn` (camelCase — matches `com.mowercare.model.response.TokenResponse`).
  - [x] **`refresh`**: `POST /api/v1/auth/refresh` with `{ "refreshToken" }`.
  - [x] **`logout`**: `POST /api/v1/auth/logout` with `{ "refreshToken" }` — expect **204** on success; handle **401** as “already signed out” **per product tolerance** (idempotent UX: still clear local session).
- [x] **Replace placeholder `AuthProvider`** (AC: 1, 2, 4, 5)
  - [x] Remove **`signInPlaceholder` / `signOutPlaceholder`**; expose **`signIn`**, **`signOut`**, **`isAuthenticated`**, and **`isRestoringSession`** while reading SecureStore on cold start.
  - [x] On app launch: if refresh token exists, **call `POST /api/v1/auth/refresh`** per **AC4** (required). If refresh fails with **`AUTH_REFRESH_INVALID`**, clear storage and show auth UI.
- [x] **`app/(auth)/index.tsx` — real sign-in UI** (AC: 1, 3)
  - [x] **Inputs:** **`organizationId`** (UUID text — validate format with Zod), **`email`**, **`password`** — required for **`LoginRequest`** [Source: `apps/api/.../LoginRequest.java`]. **Password** field: **`secureTextEntry`** (or Paper equivalent) — **never** plain text in logs.
  - [x] **Primary button:** `Button` **`mode="contained"`** — single primary (Button Hierarchy — [Source: `ux-design-specification.md` — UX Consistency → Button Hierarchy]).
  - [x] **Labels:** Paper **`TextInput`** with **`label`** / **`accessibilityLabel`** for screen readers (UX-DR14).
  - [x] **Submit:** `useMutation` with **`mutationFn`** calling login API; **`isPending`** disables duplicate submit and shows loading; **`isError`** shows **Problem Details**-driven message + **Retry** that **resets** or **retries** last mutation.
- [x] **`(app)` sign-out control** (AC: 2)
  - [x] Replace placeholder **sign out** on **`app/(app)/index.tsx`** (or app layout) with **`signOut`** that runs **logout** mutation or sequential **logout API + clear state**.
- [x] **Query cache** (AC: 2)
  - [x] On sign-out: **`queryClient.clear()`** or targeted **`removeQueries`** — use **`useQueryClient`** from **`@tanstack/react-query`** (obtain client from existing **`queryClient.ts`** / provider).
- [x] **Regression — 1.6 route guards** (AC: 1–5)
  - [x] Keep **`(auth)/_layout.tsx`** and **`(app)/_layout.tsx`** **`Redirect`** behavior when **`isAuthenticated`** mismatches route group — do not remove [Source: **1.6** story / current `apps/mobile`].
- [x] **CI** (AC: 1–5)
  - [x] **`npm run lint`** and **`npm run typecheck`** pass in **`apps/mobile`**.

### Review Findings

- [x] [Review][Patch] Clear refresh token and in-memory session if `verifyTenantScope` fails after a successful login — `setRefreshToken` and `setSession` run before the probe; on failure the mutation surfaces an error but SecureStore and the module session can remain populated while `isAuthenticated` stays false. — [auth-context.tsx:64-74] — fixed 2026-03-30
- [x] [Review][Patch] Avoid unhandled promise rejections when the sign-in button calls `submit()` — `handleSubmit` can reject if `mutateAsync` throws; attach `.catch` or use `void submit().catch(...)` on the press handler. — [app/(auth)/index.tsx:~119] — fixed 2026-03-30
- [x] [Review][Patch] Single source of truth for the default API base URL — `app.config.ts` duplicates the `DEFAULT_API_BASE_URL` literal already defined in `lib/config.ts`; drift risk if one is updated without the other. — [app.config.ts] — fixed 2026-03-30 (`default-api-base-url.js` + `lib/config.ts` import)
- [x] [Review][Patch] After login, set session `organizationId` from the access token via `getOrganizationIdFromAccessToken` (with validation) so it matches cold-start restore and the tenant-scope URL uses the same source as the JWT. — [auth-context.tsx:71] — fixed 2026-03-30
- [x] [Review][Defer] No request timeout on `fetch` in `lib/http.ts` — hung calls on bad networks; broader architecture concern. — [http.ts] — deferred, pre-existing
- [x] [Review][Defer] Empty `catch` blocks on logout and session restore — no structured logging or user-visible diagnostics for unexpected failures. — [auth-context.tsx] — deferred, pre-existing product choice

## Dev Notes

### Scope boundaries

- **In scope:** Real login/logout/refresh wiring; secure refresh storage; in-memory access; Problem Details surfaced in UI; RHF+Zod form; `lib/api` foundation; session restore policy (documented).
- **Out of scope:** **Biometrics** (Face ID) — optional later; **deep link** auth; **org picker** beyond a single **organizationId** field; **full** global **`fetch`** interception for every API — acceptable to implement minimal refresh in **`lib/api`** if it keeps **1.8** unblocked.

### Architecture compliance

- **Auth:** Access token **in memory**; refresh in **secure storage**; refresh on **401** once with **backoff**; logout clears storage + query cache [Source: `architecture.md` — Process patterns → Auth].
- **Network:** All HTTP through **`lib/api`** + TanStack Query hooks — **no** ad-hoc `fetch` in screens except via the client [Source: `architecture.md` — Structure patterns → Mobile].
- **Env:** **Public** API base URL only — **no secrets** in repo [Source: `architecture.md` — Structure patterns → Mobile — Env].
- **Errors:** RFC 7807 Problem Details — align UI with **`code`** for branching [Source: `architecture.md` — Format patterns → Errors].

### API contract (verified in codebase)

| Endpoint | Request body | Success |
|----------|----------------|---------|
| `POST /api/v1/auth/login` | `{ "organizationId": "<uuid>", "email": "...", "password": "..." }` | **200** + `TokenResponse` |
| `POST /api/v1/auth/refresh` | `{ "refreshToken": "..." }` | **200** + `TokenResponse` |
| `POST /api/v1/auth/logout` | `{ "refreshToken": "..." }` | **204** |

[Source: `AuthController.java`, `LoginRequest.java`, `TokenResponse.java`]

### Problem Details JSON (verified — `AuthIT`)

Spring serializes **`code` as a top-level JSON property** (`jsonPath("$.code")` in tests). Parse failures must read **`code`**, **`title`**, **`detail`** (validation errors embed field hints in **`detail`** for **400**).

| Status | `code` | When |
|--------|--------|------|
| 401 | `AUTH_INVALID_CREDENTIALS` | Login — wrong org/email/password |
| 401 | `AUTH_REFRESH_INVALID` | Refresh/logout with invalid, revoked, or expired refresh |
| 401 | `AUTH_INVALID_TOKEN` | Protected route — bad/expired access JWT |
| 403 | `TENANT_ACCESS_DENIED` | Wrong org in path vs JWT |
| 400 | `VALIDATION_ERROR` | `MethodArgumentNotValidException` — use **`detail`** |

### UX-DR mapping (epics → UX)

| ID | Intent for this story |
|----|------------------------|
| UX-DR6 / UX-DR12 | Primary **Sign in** action; secondary actions de-emphasized — [Source: `ux-design-specification.md` — Button Hierarchy] |
| UX-DR9 | Errors tied to **Problem Details** + retry — [Source: `ux-design-specification.md` — Feedback Patterns; Component Strategy → MutationFeedback] |
| UX-DR14 | **Labels** and **accessibility** on auth inputs |

### Technical requirements

| Area | Requirement |
|------|-------------|
| Expo | SDK **~55** — `expo install` for native modules |
| API base | **`getApiBaseUrl()`** from **`lib/config.ts`** — trailing slash policy: **no** trailing slash on base, paths start with **`/api/v1/...`** |
| Secure storage | **`expo-secure-store`** for refresh token only |
| Forms | **react-hook-form** + **zod** — Zod schema mirrors login request (UUID + email + password min length **8** to match **`@Size(min = 8)`** on API) |

### Library / framework requirements

| Package | Role |
|---------|------|
| `expo-secure-store` | Persist refresh token |
| `react-hook-form` + `@hookform/resolvers` + `zod` | Login form validation |
| `@tanstack/react-query` | Login/logout mutations (already in project) |
| `react-native-paper` | TextInput, Button, HelperText, theme |

### File structure requirements (target)

Align with [Source: `architecture.md` — Mobile `apps/mobile`]:

```
apps/mobile/
  app/
    (auth)/index.tsx       # Real sign-in form + mutation
    (app)/index.tsx        # Sign out wired to API + clear session
  lib/
    api.ts                 # fetch + Problem Details parse + optional Bearer
    auth-storage.ts        # SecureStore helpers
    auth-context.tsx       # Real session state, replace placeholders
    config.ts              # (existing)
    queryClient.ts         # (existing)
```

Optional: `lib/auth/schemas.ts` for Zod; `components/MutationFeedback.tsx` if extracted for reuse.

### Testing requirements

- **Manual:** Wrong password → **401** Problem Details copy visible; success → **`(app)`**; sign out → **`(auth)`** and re-login works.
- **Manual:** Kill app with session → relaunch → **session restores** via **refresh** (AC **4**).
- **Manual:** After login, **`GET`** tenant-scope probe returns **200** (AC **5**).
- **CI:** Lint + typecheck green; no new secrets in repo.

### Previous story intelligence

- **1.6** established **`AuthProvider`**, **`(auth)`/`(app)`** group layouts with **Redirect** on mismatch, **`router.replace('/')`** after auth actions, **`useTheme()`** backgrounds, **`createQueryClient`** retry defaults — **preserve** these patterns.
- **1.6** explicitly left **no** `lib/api` — **1.7** introduces it.
- **1.4**/**1.5** API: login returns JWT with **`organizationId`** claim for tenant scope — mobile will use Bearer on protected routes in **1.8+**.

### Git intelligence

- Latest mobile work: **6141722** — Expo Router shell, Paper, Query, placeholder auth — **1.7** replaces placeholder with API-backed session.

### Latest tech notes (verify at implementation time)

- **expo-secure-store** — use **async** get/set; handle **web** fallback if Expo web is in scope (may be limited — document if **native-only**).
- **TanStack Query v5** — `useMutation` **`isPending`** (not `isLoading`).

### Project context reference

- **`docs/project-context.md`** — not present; optional **Generate Project Context** after **1.7** stabilizes auth.

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.7
- `_bmad-output/planning-artifacts/architecture.md` — Auth, mobile structure, Problem Details
- `_bmad-output/planning-artifacts/ux-design-specification.md` — MutationFeedback, Button hierarchy, Feedback patterns
- `_bmad-output/implementation-artifacts/1-4-authentication-api-login-refresh-logout.md` — API behavior
- `_bmad-output/implementation-artifacts/1-6-mobile-app-shell-paper-query-router-auth-placeholder.md` — Shell + placeholder constraints

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — `bmad-dev-story` workflow

### Debug Log References

### Completion Notes List

- Implemented **`expo-secure-store`**, **react-hook-form**, **@hookform/resolvers**, **zod**; **`lib/http.ts`** (`ApiProblemError`, `fetchWithoutAuth`); **`lib/auth-api.ts`**; **`lib/auth/session.ts`** (in-memory access + org id); **`lib/auth-storage.ts`**; **`lib/jwt-org.ts`** (JWT `organizationId` claim); **`lib/api.ts`** (`refreshSession`, `authenticatedFetchJson` with 401→refresh, `verifyTenantScope`); **`lib/auth/login-schema.ts`**; **`AuthProvider`** with cold-start refresh + **`verifyTenantScope`** after login/restore; **`queryClient`** singleton for **`clear()`** on sign-out; sign-in screen with **RHF** + **`useMutation`**; route groups show **ActivityIndicator** while **`isRestoringSession`**; **`npm run lint`** and **`npm run typecheck`** pass. Automated mobile unit tests not added (project has no Jest/Vitest script; story CI gate is lint + typecheck).

### File List

- `apps/mobile/package.json`
- `apps/mobile/package-lock.json`
- `apps/mobile/app/_layout.tsx`
- `apps/mobile/app/index.tsx`
- `apps/mobile/app/(auth)/_layout.tsx`
- `apps/mobile/app/(auth)/index.tsx`
- `apps/mobile/app/(app)/_layout.tsx`
- `apps/mobile/app/(app)/index.tsx`
- `apps/mobile/lib/queryClient.ts`
- `apps/mobile/lib/auth-context.tsx`
- `apps/mobile/lib/http.ts`
- `apps/mobile/lib/auth-api.ts`
- `apps/mobile/lib/api.ts`
- `apps/mobile/lib/auth-storage.ts`
- `apps/mobile/lib/auth/session.ts`
- `apps/mobile/lib/auth/login-schema.ts`
- `apps/mobile/lib/jwt-org.ts`
- `apps/mobile/default-api-base-url.js`
- `apps/mobile/default-api-base-url.d.ts`

### Change Log

- 2026-03-30 — Story 1.7: real sign-in/sign-out, SecureStore refresh token, `lib/api` Bearer + tenant-scope probe, session restore, RHF+Zod form.
- 2026-03-30 — Code review: rollback partial auth on tenant-scope failure; JWT org for session; shared `default-api-base-url.js`; `submit` promise handling.

## Story completion status

- **Status:** done  
- **Note:** Code review complete; review patches applied 2026-03-30.

---

## Story validation report (`validate`)

**Date:** 2026-03-30  
**Validator:** `bmad-create-story` checklist (systematic source re-check)

| Category | Result |
|----------|--------|
| **Critical gaps** | Addressed: Problem Details **`$.code`** shape (verified **`AuthIT`**); `LogoutRequest`/`RefreshRequest` field **`refreshToken`**; AC **4**/**5** split (cold-start refresh vs Bearer 401→retry); **`tenant-scope`** probe; **`secureTextEntry`**; **1.6** layout regression guard |
| **Enhancements** | Applied: problem-details `code` table; **`tokenType`** for Bearer; session restore **mandatory** |
| **Risk** | Low — story now specifies concrete API paths and JSON fields aligned with `apps/api` tests |

**Outcome:** **Pass** — story updated in-place; ready for `dev-story`.
