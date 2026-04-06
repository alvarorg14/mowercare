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

## Deferred from: code review of 3-3-issue-list-direction-a-list-filters-row.md (2026-03-31)

- **`IssueStubController` naming debt:** GET list is live; rename/split controller when structure is next refactored (same as 3.2 note).
- **Probe query payload for empty-state detection:** Secondary `scope=all` query may return up to 200 rows only to infer “org has issues elsewhere”; story mentioned `limit=1` as an example — acceptable MVP; add `limit` query param or a tiny count endpoint if profiling warrants it.
- **List sort index:** Optional composite index on `(organization_id, updated_at desc)` per AC5; not added in this story; revisit under load.

## Deferred from: code review of 3-7-issue-activity-history-on-detail.md (2026-04-03)

- **Mobile change-events paging:** `listIssueChangeEvents` only loads the first page (`page=0`, `size=50`). Issues with more than 50 events do not show older activity until load-more or paging is implemented — acceptable MVP unless product requires full on-device history.

## Deferred from: code review of 3-9-mobile-accessibility-and-contrast-baseline-for-issue-flows.md (2026-04-03)

- **Contrast script duplicates `theme.ts` hex values:** The Node contrast gate keeps its own copy of palette tokens; if `paperTheme` or status tokens change, update both files or the script can give false confidence. Consider generating the script inputs from a single source (build step) when theme churn increases.
- **AC6 device/OS list vs pending manual checks:** Story acceptance criteria expect the Dev Agent Record to list devices used for manual a11y checks once those runs happen; current record states checks are recommended but not yet performed. Close the loop when iOS/Android spot checks are done.

## Deferred from: code review of 4-1-notification-records-and-issue-event-taxonomy.md (2026-04-06)

- **Mobile Jest + lockfile churn alongside 4.1 backend work:** `apps/mobile` test tooling and `package-lock.json` changes are not listed in the story file list; split chore PRs when possible for reviewability.
- **`event_type` without CHECK constraint:** Optional per AC2; consider a PostgreSQL CHECK or enum mapping if future stories require DB-enforced taxonomy invariants.

## Deferred from: code review of 4-2-notification-delivery-rules-by-role.md (2026-04-06)

- **No failure-injected integration test for transactional fan-out (AC3):** `NotificationEventRecorder` + fan-out share the issue transaction; rollback behavior on persistence failure is not proven by tests — acceptable unless product requires explicit regression harness.

## Deferred from: code review of 4-3-in-app-notification-list-rest-and-ui.md (2026-04-06)

- **Notification list first page only in mobile:** The screen always requests `page=0` and `size=50`; additional pages are not loaded. Acceptable for MVP; add infinite scroll or “load more” when backlog warrants.

## Deferred from: code review of 4-4-device-registration-and-push-delivery.md (2026-04-06)

- **EAS / store builds: confirm push entitlements and Android channels (AC8):** Plugin-only `app.config.ts` change may be enough for dev client; validate on first EAS iOS/Android release that entitlements and channels match Expo 55 docs.
- **Push runs inside same DB transaction as recipient insert:** `NotificationRecipientFanoutService` calls `dispatchForNewRecipient` before commit; slow or flaky FCM prolongs the transaction. Acceptable for MVP; revisit async/after-commit if latency or timeouts appear.