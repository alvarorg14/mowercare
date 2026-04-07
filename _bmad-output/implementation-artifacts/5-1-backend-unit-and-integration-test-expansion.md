# Story 5.1: Backend unit and integration test expansion

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **developer**,
I want **JUnit unit tests for every `apps/api` production class that contains real logic, plus integration tests (e.g. Testcontainers) for critical paths**,
so that **refactors and new work do not regress tenant isolation, auth, issues, or notifications (NFR-S3, NFR-R3)**.

**Implements:** NFR-S3 (verification); NFR-R3; Additional: Spring Boot testing idioms, no secrets in test logs; **BDD-style test names** (`given` / `when` / `then` in method names) and **`@DisplayName`** on every test method.

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Backlog note:** [`epics.md`](../planning-artifacts/epics.md) Story 5.1 asks for **meaningful** unit coverage on critical paths; **this story file** tightens scope to **all logic-bearing classes**, BDD naming (AC6), and explicit exemption tracking — treat **this file** as the implementation source of truth for dev.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 5.1])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** the API module **When** unit tests run **Then** every **logic-bearing** class has a corresponding unit test; only **no-logic** types are exempt (see AC1) | For each exempt type, record it in **`docs/testing-backend.md`** **or** add a **short class-level comment** (e.g. why it has no logic) so omissions are **explicit**. |
| **Given** integration tests with PostgreSQL **When** suite runs **Then** cross-tenant denial, auth on protected routes, and at least one E2E API path each for **issues** and **notifications** per OpenAPI | Extend **`AbstractPostgresIntegrationTest`** + `MockMvc` ITs; align request/response shapes with **OpenAPI**; reuse **`NoOpPushNotificationSenderConfig`** so ITs never call FCM. |
| **Given** CI **When** PR opened **Then** backend test job green; failures actionable; no flaky sleeps without justification | **`mvn -B verify`** in `apps/api` must pass locally and in [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml); use **`Awaitility` only if already on classpath** — prefer deterministic waits; avoid `Thread.sleep` except with comment. |

### Cross-epic boundaries

| Source | Relationship |
|--------|----------------|
| [Epic 5 addendum in epics.md](../planning-artifacts/epics.md) | Stories **5.1–5.6** are **sequentially independent**. If domain package reorganization (**Story 5.4** in epics) would churn imports in many tests, consider doing **5.4 before** large new test suites — optional product order, not a hard dependency. |
| Epic 4 (notifications) | ITs already cover inbox, device tokens, push boundaries with no-op sender — **extend** those patterns; do not require real Firebase in CI. |

---

## Acceptance Criteria

1. **Unit coverage (all logic-bearing classes)**  
   **Given** `apps/api/src/main/java`  
   **When** `./mvnw test` (or `mvn -B test`) runs  
   **Then** every **top-level type** (class, enum, interface with default/static behavior if any) that **contains logic** has a matching unit test under `src/test/java` mirroring the package (e.g. `Foo` → `FooTest`, or focused tests grouped by feature as long as **every** logic path is covered).  
   **And** **logic** means: branching, loops, calculations, validation, mapping beyond trivial getters, security decisions, transaction boundaries, custom repository logic, exception handling, HTTP/security wiring worth asserting — not merely data holding.  
   **And** types **without** logic (see below) are **exempt** from unit tests but must be **listed once** in `docs/testing-backend.md` (or a dedicated subsection) **or** use a **short class-level comment** (why the type has no testable logic) so reviewers see the exemption is intentional.  

   **Exempt — no standalone unit test required (document or comment):**  
   - JPA **entities** / simple **records** / DTOs that are only fields + accessors (no invariants, no factories, no methods with behavior).  
   - **Marker interfaces**, package-private **constants-only** holders, and **generated** code.  
   - **Spring `@Configuration` / `@Bean`** classes that only register beans with no custom logic (wiring-only); if a config computes URLs, secrets, or conditional branches, it **does** need tests.  
   - **`Application` main** entrypoint (context load covered by integration tests).  

   **Not exempt (must have unit tests if not already present):** services, domain validators/parsers, security (`Jwt*`, filters, `*Authorization*`), `ApiExceptionHandler`, mappers with rules, `NotificationRecipientRules`-style utilities, repository **custom** implementations, and **controllers** — use **`@WebMvcTest`** + `MockMvc` for request mapping, validation, and security on the slice; if a controller is pure delegation with **no** branchy behavior, still prefer a minimal WebMvcTest **or** justify exemption in the inventory as “no logic beyond delegation” and rely on ITs **only if** that justification is recorded.

2. **Integration coverage (PostgreSQL + app)**  
   **Given** Testcontainers PostgreSQL via [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java)  
   **When** integration tests run  
   **Then** there is explicit coverage of: **cross-tenant denial** (user/org A cannot read or mutate org B’s data), **authenticated access** on representative **protected** routes  
   **And** at least **one** full HTTP path for **issues** (create, patch, or list+detail consistent with OpenAPI) and **one** for **notifications** (e.g. inbox list or mark-read) **matching** published contracts.  
   **And** if existing `*IT` classes (e.g. [`TenantScopeIT`](../../apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java), issue controller ITs, [`NotificationInboxIT`](../../apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java)) already satisfy this, **record their names** in completion notes and **avoid duplicate** scenarios unless closing a proven gap.

3. **No secrets / safe logs**  
   **Given** tests log or print on failure  
   **When** CI runs  
   **Then** no real **passwords**, **bootstrap tokens**, or **JWT signing secrets** from production-style config appear in output; test fixtures use **obvious test-only** values (existing pattern: JWT secret in dynamic properties for IT base class).

4. **CI contract**  
   **Given** GitHub Actions `api` job  
   **When** a PR is opened  
   **Then** `cd apps/api && mvn -B verify` remains **green**  
   **And** new tests are **deterministic** on CI (Docker available on `ubuntu-latest` for Testcontainers).

5. **Flake policy**  
   **Given** async or timing-sensitive code  
   **When** tests assert on eventual state  
   **Then** prefer synchronous APIs, `MockMvc` result assertions, or test doubles; **avoid** fixed `Thread.sleep` unless documented why no alternative exists.

6. **Test naming and `@DisplayName`**  
   **Given** new or updated `@Test` methods in `apps/api/src/test`  
   **When** tests are written or refactored for this story  
   **Then** every test method **name** encodes **Given / When / Then** using a **`given...`** prefix and includes **`when`** and **`then`** segments where they add clarity (e.g. `givenAdminAndTech_whenResolve_thenExcludesActor`, or `givenNoAssignee_whenResolveRecipients_thenOnlyNonActorAdmins`). Underscore-separated segments are acceptable if the class already uses that style; otherwise use lowerCamelCase with `given` / `when` / `then` as distinct parts.  
   **And** every `@Test` method has an **`@DisplayName`** whose value is a **readable Given–When–Then** phrasing (plain English, same scenario as the method name), e.g. `@DisplayName("given actor is admin when resolving recipients then actor is excluded and other admin and assignee remain")`.  
   **And** apply the same rules to **new** integration test methods in `*IT` classes touched by this story; align existing tests in edited files when practical without a repo-wide mechanical rename unless time allows.

---

## Tasks / Subtasks

- [x] **Inventory: main vs test** (AC: 1)
  - [x] Enumerate all types under `apps/api/src/main/java/com/mowercare`; label each **logic** vs **no-logic** (per AC1); for **no-logic**, add the class comment **or** a single table row in `docs/testing-backend.md`.
- [x] **Unit tests — full pass** (AC: 1, 3, 5, 6)
  - [x] For **every logic class** without an adequate existing test, add `*Test` (pure JUnit + mocks) or **`@WebMvcTest`** / **`@DataJpaTest`** only where appropriate — prefer **fast** unit tests; use **integration** (`*IT`) for DB-heavy paths already covered by story AC2.
  - [x] Reuse **AssertJ** + **JUnit 5** from existing tests (e.g. [`NotificationRecipientRulesTest`](../../apps/api/src/test/java/com/mowercare/service/NotificationRecipientRulesTest.java)).
  - [x] Use **`given` / `when` / `then` in method names** and **`@DisplayName`** on every new `@Test` (AC6); update touched tests in the same files for consistency where reasonable.
- [x] **Integration tests** (AC: 2, 3, 4, 6)
  - [x] Extend or add `*IT` classes under `controller` / `service` following [`NotificationInboxIT`](../../apps/api/src/test/java/com/mowercare/controller/NotificationInboxIT.java), [`TenantScopeIT`](../../apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java), [`IssuePatchIT`](../../apps/api/src/test/java/com/mowercare/controller/IssuePatchIT.java) patterns.
  - [x] Ensure push-capable code paths use **`@Import` / test config** so [`NoOpPushNotificationSenderConfig`](../../apps/api/src/test/java/com/mowercare/testsupport/NoOpPushNotificationSenderConfig.java) prevents outbound FCM.
- [x] **Documentation** (AC: 1, 6)
  - [x] Add or update `docs/testing-backend.md`: Maven commands, Docker/Testcontainers note, **inventory of no-logic exempt types** (or pointer that exemptions are commented in source), how ITs complement unit tests, and a **short subsection** on `given`/`when`/`then` method names + `@DisplayName` (link to AC6).
- [x] **Verification** (AC: 4)
  - [x] Run `mvn -B verify` in `apps/api` locally before PR.

### Review Findings

- [x] [Review][Decision] AC1 requires unit coverage for every logic-bearing class, but completion notes still list follow-up controllers (`OrganizationUsersController`, `IssueStubController`, …). **Resolved (2026-04-07):** Initially **in progress** until follow-up slices; **closure (2026-04-07):** Story marked **done** by explicit decision — optional additional `@WebMvcTest` / controller slices remain follow-up work in backlog or a later story, not blocking 5.1 completion.

- [x] [Review][Patch] `OpaqueTokenServiceTest` uses `@RepeatedTest` with a single `@DisplayName`; AC6 expects per-invocation readable names in reports — add a `name = "..."` template on `@RepeatedTest` (or equivalent) so each repetition is distinct in CI output. [`apps/api/src/test/java/com/mowercare/service/OpaqueTokenServiceTest.java`](../../apps/api/src/test/java/com/mowercare/service/OpaqueTokenServiceTest.java):18 — **Fixed (2026-04-07):** `name = "{displayName} — repetition {currentRepetition}/{totalRepetitions}"`.

- [x] [Review][Patch] `AccountStatusVerificationFilter` continues the chain when `userRepository.findById` yields empty (`user == null`); add a test that locks this behavior in (differs from deactivated user). [`apps/api/src/test/java/com/mowercare/security/AccountStatusVerificationFilterTest.java`](../../apps/api/src/test/java/com/mowercare/security/AccountStatusVerificationFilterTest.java) — **Fixed (2026-04-07):** `givenUserIdNotFound_whenFilterOnProtectedPath_thenChainContinues`.

- [x] [Review][Patch] `IssueSortsTest` mostly checks substrings / `toString()`; assert the concrete `Sort` order (properties + directions, including tie-break) so refactors cannot slip past weak matchers. [`apps/api/src/test/java/com/mowercare/issue/IssueSortsTest.java`](../../apps/api/src/test/java/com/mowercare/issue/IssueSortsTest.java) — **Fixed (2026-04-07):** explicit `Order` property/direction assertions for updated/created and priority CASE + id tie-break.

- [x] [Review][Defer] Same PR bundles Epic 4 retrospective, large `epics.md` addendum, and Story 5.1 tests — harder review and noisier history; prefer splitting next time. — deferred, process hygiene

- [x] [Review][Defer] `epic-4-retro-2026-04-06.md` narrative says Epic 5 was not yet in `epics.md`; planning has since added Epic 5 — treat as point-in-time retro unless you add a one-line “superseded context” note. — deferred, documentation hygiene

- [x] [Review][Defer] `DataIntegrityViolationsTest` does not cover a deeply nested `ConstraintViolationException` cause chain — only add if production has seen wrapped integrity errors that bypass current detection. — deferred, low likelihood

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| IT base + Postgres 16 container | [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) |
| No-op FCM for ITs | [`NoOpPushNotificationSenderConfig`](../../apps/api/src/test/java/com/mowercare/testsupport/NoOpPushNotificationSenderConfig.java) |
| Example controller ITs | `*IT.java` under [`controller/`](../../apps/api/src/test/java/com/mowercare/controller/) |
| Example pure unit tests | [`IssueStatusTransitionValidatorTest`](../../apps/api/src/test/java/com/mowercare/issue/IssueStatusTransitionValidatorTest.java), [`AuthServiceTest`](../../apps/api/src/test/java/com/mowercare/service/AuthServiceTest.java), [`RoleAuthorizationTest`](../../apps/api/src/test/java/com/mowercare/security/RoleAuthorizationTest.java) |
| API module build | [`apps/api/pom.xml`](../../apps/api/pom.xml) — Spring Boot **4.0.5**, Java **25**, Testcontainers **2.0.2** (`testcontainers-postgresql`) |
| CI | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) — `mvn -B verify` |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Tests mirror `main` layout; Testcontainers for valuable integration coverage | [`architecture.md`](../planning-artifacts/architecture.md) — Tests section |
| Tenant isolation must be enforced and **tested** | Same — Multi-tenancy & NFR-S3 |
| REST + Problem Details for errors | Existing [`ApiExceptionHandlerTest`](../../apps/api/src/test/java/com/mowercare/exception/ApiExceptionHandlerTest.java) pattern |

### Library / framework requirements

- **JUnit 5** (`org.junit.jupiter.api.Test`, **`org.junit.jupiter.api.DisplayName`**), **Spring Boot Test**, **MockMvc** + **`springSecurity()`** for secured endpoints — match existing ITs.
- **Testcontainers** PostgreSQL module already on classpath; do not add a second DB technology without team agreement.
- Do **not** add **real** Firebase or network calls in tests; keep **`PushNotificationSender`** mocked or no-op in ITs.

### File structure requirements

- New tests under `apps/api/src/test/java/com/mowercare/...` mirroring production packages.
- Optional doc: `docs/testing-backend.md` at repo root (if created, keep it short and command-oriented).

### Testing requirements

- **Scope:** Unit tests target **all logic-bearing** classes; **no-logic** types are explicitly exempt (see AC1) and must be **documented or commented** so nothing is silently skipped.
- **Meaningful** tests assert real behavior (inputs → outputs, thrown exceptions, security denials), not empty `@Test` methods.
- **Naming (AC6):** Test **method** names must include a leading **`given`** segment and use **`when`** / **`then`** in the name when they clarify the scenario (matches BDD readability in IDEs and failure output). Every `@Test` must have **`@DisplayName("...")`** with a full **Given … when … then …** sentence (lowercase “given/when/then” in prose is fine). For **`@ParameterizedTest`**, use a **`name`** template or per-invocation display text so each case reads as Given–When–Then in reports.
- **Controllers:** Prefer `@WebMvcTest` for servlet/API behavior; use `@MockBean` for services — aligns with “unit” for web layer without full context.
- Integration tests should **clean or isolate data** using existing repository `deleteAll` patterns in `@BeforeEach` where multi-test interference is a risk (see `NotificationInboxIT` setup).

### Previous epic intelligence (Epic 4 → 5)

- Notification and issue flows were implemented with **IT coverage** in Epic 4 stories; this story **expands** confidence and fills **unit** gaps — prefer **extending** established IT fixtures over duplicating bootstrap logic in every class (extract shared helpers if duplication grows).

### Git intelligence (recent commits)

- Recent work: push deep link (4.5), device tokens / FCM dispatch (4.4), notification inbox (4.3), recipient rules (4.2), domain events (4.1). Tests should **protect** these paths against regression when adding new cases.

### Latest technical notes

- **Spring Boot 4.0.x** / **Java 25** — follow existing `pom.xml` parent; no version bumps in this story unless required for test dependencies (avoid scope creep).

### Project context reference

- No `project-context.md` in repo; this file + `architecture.md` + `epics.md` are the implementation sources.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `mvn -B verify` in `apps/api` (2026-04-07): **PASS**.

### Completion Notes List

- Added [`docs/testing-backend.md`](../../docs/testing-backend.md): commands, Docker/Testcontainers, **AC6** naming note, **no-logic** inventory (DTOs, entities, repos, wiring config, exceptions), and **AC2 mapping** to existing `TenantScopeIT`, `AuthIT`, `Issue*IT`, `NotificationInboxIT` (no duplicate IT scenarios).
- New unit tests (AC6 `given_*_when_*_then_*` + `@DisplayName`): `EmailNormalizationTest`, `DataIntegrityViolationsTest`, `ApiProblemBodiesTest`, `TenantPathAuthorizationTest`, `IssueSortsTest`, `OpaqueTokenServiceTest`, `JwtServiceTest`, `ApiAuthenticationEntryPointTest`, `AccountStatusVerificationFilterTest`, `DevicePushTokenServiceTest`, `NotificationRecipientFanoutServiceTest`, `NotificationEventRecorderTest`, `NotificationPushDispatcherTest`, `NotificationInboxServiceTest`, `AuthControllerTest` (standalone `MockMvc` + `ApiExceptionHandler`, matching existing controller test style).
- **AC2:** Confirmed existing integration tests satisfy cross-tenant, auth, issues, and notifications paths; documented in `docs/testing-backend.md`.
- **Follow-up (optional):** Additional `@WebMvcTest` or standalone slices for other controllers (`OrganizationUsersController`, `IssueStubController`, …) and deeper `OrganizationUserService` unit tests if you want parity with every remaining logic-heavy class in one pass.

### File List

- `docs/testing-backend.md`
- `apps/api/src/test/java/com/mowercare/common/EmailNormalizationTest.java`
- `apps/api/src/test/java/com/mowercare/common/persistence/DataIntegrityViolationsTest.java`
- `apps/api/src/test/java/com/mowercare/exception/ApiProblemBodiesTest.java`
- `apps/api/src/test/java/com/mowercare/security/TenantPathAuthorizationTest.java`
- `apps/api/src/test/java/com/mowercare/issue/IssueSortsTest.java`
- `apps/api/src/test/java/com/mowercare/service/OpaqueTokenServiceTest.java`
- `apps/api/src/test/java/com/mowercare/service/JwtServiceTest.java`
- `apps/api/src/test/java/com/mowercare/config/ApiAuthenticationEntryPointTest.java`
- `apps/api/src/test/java/com/mowercare/security/AccountStatusVerificationFilterTest.java`
- `apps/api/src/test/java/com/mowercare/service/DevicePushTokenServiceTest.java`
- `apps/api/src/test/java/com/mowercare/service/NotificationRecipientFanoutServiceTest.java`
- `apps/api/src/test/java/com/mowercare/service/NotificationEventRecorderTest.java`
- `apps/api/src/test/java/com/mowercare/service/NotificationPushDispatcherTest.java`
- `apps/api/src/test/java/com/mowercare/service/NotificationInboxServiceTest.java`
- `apps/api/src/test/java/com/mowercare/controller/AuthControllerTest.java`

### Change Log

- 2026-04-07: Story 5.1 — backend test documentation, new unit tests for utilities/security/services/AuthController, `mvn verify` green.
- 2026-04-07: Status **done** — code review patches applied; sprint tracking synced.

---

**Completion status:** Story **done** (2026-04-07). Optional controller test slices from completion notes may be picked up later.

---

## Create-story validation record

**Run:** `validate` (checklist per [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md))  
**Date:** 2026-04-07  
**Target:** `5-1-backend-unit-and-integration-test-expansion.md`  
**Sprint:** `5-1-backend-unit-and-integration-test-expansion` → `ready-for-dev` (see [`sprint-status.yaml`](./sprint-status.yaml))

### Verdict: **Pass** (ready for `dev-story`)

| Check | Result |
|-------|--------|
| Epics / PRD alignment | **Pass** — Implements NFR-S3, NFR-R3, epics integration/CI clauses; story **extends** epics with stricter unit scope (documented under Backlog note). |
| Architecture anchors | **Pass** — Testcontainers base, no-op push, CI command, package mirror, Problem Details / tenant testing called out. |
| Anti–reinvent-the-wheel | **Pass** — Points to existing `*IT`, `AbstractPostgresIntegrationTest`, `NoOpPushNotificationSenderConfig`, example `*Test` classes. |
| File locations / stack | **Pass** — `apps/api`, Java 25 / Spring Boot 4.0.5 from `pom.xml`. |
| Regression / scope risk | **Mitigated** — AC2 now says confirm existing ITs before duplicating; epics “meaningful” vs “all logic” called out. |
| Gaps / disasters | **None blocking** — `@ParameterizedTest` display guidance added in Dev Notes. |

### Findings (non-blocking)

- **Enhancement (optional):** Several existing tests (e.g. `NotificationRecipientRulesTest`) predate AC6 naming; story already limits renames to touched files — acceptable.
- **Optimization:** Inventory of `src/main` types + `docs/testing-backend.md` is large; implementer may do it incrementally per package if completion notes track progress.

### Checklist execution

Systematic re-analysis against `epics.md`, `architecture.md` testing sections, and repo layout completed; **critical** issues: **0**; **enhancements** applied in-file (AC2 duplicate-IT guard, epics precedence note, parameterized test naming, exemption wording).
