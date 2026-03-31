# Deferred work (from code reviews)

## Deferred from: code review of 1-1-initialize-monorepo-from-expo-and-spring-boot-starters.md (2026-03-27)

- **Initializr placeholder metadata in `pom.xml`:** Empty `license`, `developers`, and `scm` blocks are common Initializr noise; fill in when publishing or open-sourcing the API module.
- **Static Testcontainers container in tests:** A single static `PostgreSQLContainer` is fine for one test class; if the suite grows and JUnit parallelizes multiple classes using containers, revisit lifecycle (per-class containers, singleton reuse pattern, or dedicated base test).

## Deferred from: code review of 1-3-bootstrap-first-organization-and-admin-user.md (2026-03-29)

- **Permissive `SecurityConfig` (permitAll, CSRF off):** Intentional until Story 1.5 global API security; bootstrap remains protected by `X-Bootstrap-Token` at the application layer per story scope.

## Deferred from: code review of 1-4-authentication-api-login-refresh-logout.md (2026-03-30)

- **Concurrent refresh / multiple valid refresh rows:** Same opaque token presented concurrently could theoretically create overlapping rotation attempts; less observed in production or tests.
MVP acceptable per story scope.
- **Unique `token_hash` violation on insert:** Astronomically rare hash collision would surface as a generic persistence error; defer dedicated handling un

## Deferred from: code review of 1-5-global-api-security-and-tenant-context-on-protected-routes.md (2026-03-30)

- **Duplicate `requestInstance` for Problem `instance` URI:** `ApiAuthenticationEntryPoint` and `ApiExceptionHandler` both implement identical `requestInstance(HttpServletRequest)` helpers; consolidate into a shared utility when error handling is next refactored.

## Deferred from: code review of 1-7-mobile-sign-in-and-sign-out.md (2026-03-30)

- **No request timeout on `fetch` in `lib/http.ts`:** Calls can hang indefinitely on bad networks; add `AbortSignal`/timeout when the app adopts a global HTTP policy.
- **Empty `catch` on logout and session restore in `auth-context.tsx`:** Swallows errors for idempotent UX; consider structured logging or error reporting when observability is added.

## Deferred from: code review of 1-8-organization-profile-read-update-for-admin.md (2026-03-30)

- **`IllegalStateException` when organization row is missing after tenant authorization:** After JWT/path alignment, `OrganizationRepository.findById` should always hit the tenant org; if the row were deleted or inconsistent, the API would return **500** instead of a Problem Detail. Revisit if hardening data consistency or admin-delete flows.

## Deferred from: code review of 2-1-role-model-and-rbac-enforcement-hooks.md (2026-03-30)

- **Sprint-status churn bundled with feature work:** `sprint-status.yaml` updates Epic 1 completion and retrospective alongside Story 2-1 state; smaller commits can make history and cherry-picks clearer.
- **RBAC matrix relative links:** Paths from `docs/rbac-matrix.md` into `_bmad-output` assume repository layout; validate or replace when documentation is published to a standalone site.

## Deferred from: code review of 2-2-invite-or-create-employee-user.md (2026-03-30)

- **AC8 OpenAPI automated verification:** Story acceptance criteria allow optional manual or light smoke for `/v3/api-docs`; no CI check was added. Add an automated smoke test later if API contract regressions become a risk.

## Deferred from: code review of 2-3-assign-roles-to-employees.md (2026-03-30)

- **`lockByOrganizationIdAndRole` pessimistic lock scope:** Loads all users with `ADMIN` role in the org for locking; fine for typical org sizes; revisit if very large admin sets or lock contention become an issue.

## Deferred from: code review of 2-4-deactivate-employee-and-block-access.md (2026-03-30)

- **Per-request DB lookup for deactivated account status:** Story accepts MVP tradeoff; consider caching or session claims if profiling shows hot-path cost.

## Deferred from: code review of 2-5-admin-settings-ui-invites-roles-deactivate.md (2026-03-30)

- **No automated mobile tests for Settings/Team routes:** Story allows a manual checklist; add Maestro/E2E or integration tests when the repo adopts a runner pattern for RN.

## Deferred from: code review of 3-1-issue-aggregate-schema-and-change-history-storage.md (2026-03-31)

- **Unrelated artifacts in the same working tree:** `epic-2-retro-2026-03-30.md` and optional `apps/api/docker-compose.yml` are not part of Story 3.1; split commits or separate PRs for clearer history.
- **Concurrent issue updates without optimistic locking:** Last-write-wins on overlapping edits; acceptable for MVP persistence story; add `@Version` or conflict handling when product requires it.

## Deferred from: code review of 3-2-create-issue-api-mobile.md (2026-03-31)

- **`IssueStubController` class name vs real POST create:** Optional rename/split per story; OpenAPI tag already says “Issues”; rename when touching controller structure next.