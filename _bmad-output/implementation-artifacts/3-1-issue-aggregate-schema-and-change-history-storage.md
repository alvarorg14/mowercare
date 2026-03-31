# Story 3.1: Issue aggregate schema and change-history storage

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. validate-create-story (checklist.md): passed 2026-03-31 — see Validation record at end. -->

## Story

As a **system**,
I want **issues and material change history persisted in PostgreSQL with Liquibase**,
so that **FR11–FR18 have a durable foundation without creating unrelated tables in advance**.

**Implements:** **FR18** (storage foundation); **NFR-R3** (durability of committed updates). The epic phrase “foundation for **FR11–FR18**” means **this story only lays down persistence** — **FR11–FR17** (create, list, update, assign, resolve, filters, etc.) are **not** implemented until **Stories 3.2–3.8**; do **not** ship REST or mobile issue flows here. **Explicitly out of scope:** notification persistence (**Epic 4**), mobile UI (**Story 3.2+**), public HTTP contract for real issue CRUD (**Story 3.2** replaces stubs).

**Epic:** Epic 3 — Issues — capture, triage, ownership & history — **first story**: persistence and auditable writes before APIs and screens.

### Epic traceability ([Source: `_bmad-output/planning-artifacts/epics.md` — Story 3.1])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** new changesets **When** migrations apply **Then** `issues` includes `organization_id`, UUID keys, timestamps; history table(s) capture actor and timestamp for material changes | **Liquibase** changelogs + PostgreSQL tables; **JPA** entities mapped with explicit `snake_case` columns; **indexes** for org-scoped access |
| **Given** JPA entities **When** issue is updated **Then** service layer writes history rows for defined material fields (status, assignment, priority, etc.) | **`com.mowercare.service.IssueService`** (transactional) performs mutations and **append-only** history rows — **no** direct entity mutation from controllers for material fields without history (when controllers arrive in later stories, they must call this service) |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.2** | Will add **create issue** HTTP + mobile; depends on **`issues`** schema and patterns from **3.1**. |
| **3.5–3.7** | Refine **state transitions**, **assign UI**, **history presentation**; **3.1** must not paint you into a corner — use **extensible** `change_type` / field keys and documented MVP **status** enum. |
| **4.x** | **No** `notifications` / device tables here. |

## Acceptance Criteria

1. **Liquibase — `issues` aggregate**  
   **Given** a new changelog included from [`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml)  
   **When** migrations apply against PostgreSQL  
   **Then** table **`issues`** exists with at least:
   - `id` **UUID** PK; `organization_id` **UUID** NOT NULL FK → `organizations(id)` (ON DELETE RESTRICT or product-consistent policy)
   - `organization_id` indexed (e.g. `idx_issues_organization_id`) — composite index with `status` optional but useful for future list queries ([Source: `architecture.md` — `idx_issues_organization_id_status`](../planning-artifacts/architecture.md))
   - `created_at`, `updated_at` **`timestamptz`** NOT NULL
   - MVP **core fields** aligned with later stories: **title** (text/varchar), **description** (text), **status** (varchar or enum mapped column), **priority** (varchar or enum), **assignee_user_id** UUID **nullable** FK → `users(id)` (nullable = unassigned)
   - **Customer/site context** columns for FR12 (minimal strings acceptable for v1, e.g. `customer_label`, `site_label` or a single `location_context` — **document chosen names** in code and changelog; **Story 3.2** will align OpenAPI names)
   - No cross-tenant uniqueness surprises: unique constraints only where product requires (e.g. optional per-org issue number **deferred** unless you already need it for UX)

2. **Liquibase — history / audit table**  
   **Given** the same changelog (or a follow-up changeset in the same file)  
   **When** migrations apply  
   **Then** a **history** table exists (name consistent with architecture: e.g. **`issue_change_events`** [Source: `architecture.md` — example `issue_change_events`](../planning-artifacts/architecture.md)) with at least:
   - `id` UUID PK  
   - `issue_id` UUID NOT NULL FK → `issues(id)` (ON DELETE CASCADE or RESTRICT — **document choice**; CASCADE simplifies org delete stories later, RESTRICT if issue rows must be retained)  
   - `organization_id` UUID NOT NULL FK → `organizations(id)` (denormalized for **tenant-scoped queries** and **NFR-S3** tests)  
   - `actor_user_id` UUID NOT NULL FK → `users(id)` (who made the change)  
   - `occurred_at` **timestamptz** NOT NULL (UTC)  
   - **What changed:** `change_type` (varchar, e.g. `STATUS_CHANGED`, `ASSIGNEE_CHANGED`, `PRIORITY_CHANGED`, `TITLE_CHANGED`, `DESCRIPTION_CHANGED`, `CUSTOMER_CONTEXT_CHANGED`, `CREATED`) **or** equivalent `field_key` + `old_value` / `new_value` text — pick one **consistent** model and stick to it for Epic 3  
   - Index suitable for detail/history reads: e.g. `(issue_id, occurred_at)`

3. **JPA — entities and repositories**  
   **Given** the new tables  
   **When** the API starts with `spring.jpa.hibernate.ddl-auto=none` ([Source: `application.yaml`](../../apps/api/src/main/resources/application.yaml))  
   **Then** the Spring context loads and JPA entity **column mappings align** with Liquibase-defined tables (with `ddl-auto: none`, Hibernate does **not** run automatic schema validation — mismatches surface as mapping or SQL errors at startup or first query; do **not** switch to `ddl-auto=update` in deployed environments)  
   **And** `Entity` classes live under **`com.mowercare.model`** (with **`@Column(name = "...")`** for snake_case) — same layer as `User`, `Organization`  
   **And** Spring Data repositories live under **`com.mowercare.repository`** — **`IssueRepository`**, **`IssueChangeEventRepository`**

4. **Service layer — history on material updates**  
   **Given** an issue persisted through the **domain service** (not raw repository calls from future controllers)  
   **When** a **material** field changes (status, assignee, priority, title, description, customer/site fields — **same set documented in code** as “material”)  
   **Then** within the **same transaction** the service inserts **one or more** history rows with correct **actor** and **timestamp**  
   **And** **initial create** records at least one **CREATED** (or equivalent) history event with the creating user as actor  
   **Note:** Implementation can expose **package-private or internal** service methods for **3.1** tests; **public REST** arrives in **3.2**.

5. **Tests**  
   **Given** existing integration test style ([`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java))  
   **When** tests run (`mvn test`)  
   **Then** at least one **integration test** proves: migrations apply; **create issue** via service + **history row**; **update** (e.g. status) produces **additional** history with correct `actor_user_id`  
   **And** a **tenant isolation** assertion at the **persistence** layer: e.g. `findByIdAndOrganizationId(issueId, wrongOrgId)` returns **empty** (or service rejects) even if `issueId` exists — mirror the **intent** of [`TenantScopeIT`](../../apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java) (JWT path vs org path) but for **repository/service** contracts (**3.1** does not need new MockMvc tests unless you find it easier)

6. **Stub controller scope**  
   **Given** [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java)  
   **When** this story completes  
   **Then** **stub** behavior may remain **unchanged** until **Story 3.2** replaces it with real create/list — **or** you may **wire** the stub to persistence **only if** you can do so without breaking RBAC tests and OpenAPI contract promises (prefer **minimal change**: keep stub, add persistence + tests **without** changing public HTTP responses until **3.2**)

## Tasks / Subtasks

- [x] **Liquibase** (AC: 1, 2)
  - [x] Add **`0008-issues-and-history.yaml`** — next file after [`0007-deactivated-by-fk-on-delete-set-null.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0007-deactivated-by-fk-on-delete-set-null.yaml) (adjust only if another changeset lands first)
  - [x] Include from [`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml)
  - [x] **Document** FK delete policies and enum/string choices in PR / commit message

- [x] **JPA entities + repositories** (AC: 3)
  - [x] `Issue`, `IssueChangeEvent` (or chosen names), enums for status/priority if used
  - [x] Repositories; consider `Optional` finders by `id` + `organizationId`

- [x] **Issue domain service** (AC: 4)
  - [x] `@Transactional` mutation methods: create + update paths for material fields
  - [x] **Single place** for history emission (avoid duplicated history logic later)

- [x] **Integration tests** (AC: 5)
  - [x] New `*IT` class using Testcontainers pattern; **reuse** org/user fixtures from existing tests if available (`BootstrapService`, test fixtures, or small builders)

- [x] **Stub/controller decision** (AC: 6)
  - [x] Default: **leave** `IssueStubController` as-is; optionally add **TODO** comment pointing to Story 3.2 replacement

- [x] **Docs** (light touch)
  - [x] Update [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) **only** if you need a one-line note that persistence landed **or** defer table update to **3.2** when routes become real (either is acceptable)

### Review Findings

- [x] [Review][Patch] Add defensive validation for non-null `title`, `status`, and `priority` in `IssueService.createIssue` (avoid opaque DB errors if internal callers pass null) — [`IssueService.java`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) (createIssue)

- [x] [Review][Patch] Add unit tests for `updateCustomerLabel` and `updateSiteLabel` mirroring other material-field tests — [`IssueServiceTest.java`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceTest.java)

- [x] [Review][Defer] Unrelated artifacts (`epic-2-retro-2026-03-30.md`, optional `docker-compose.yml`) bundled with Story 3.1 work — deferred, git/commit hygiene

- [x] [Review][Defer] Concurrent updates to the same issue have no optimistic locking (last write wins) — deferred, acceptable MVP; revisit if conflict detection is required later

## Dev Notes

### Scope boundaries

- **In scope:** PostgreSQL schema, Liquibase, JPA, transactional issue service with **append-only** history, integration tests, tenant scoping at persistence layer.
- **Out of scope:** Push/notifications tables, mobile, OpenAPI **create/list** DTOs for issues (3.2), list/detail UI, **domain event bus** to notification service (Epic 4).

### Java package layout (implementation)

Issue-related code uses **existing** top-level packages — **no** `com.mowercare.issue` package:

| Layer | Package | Types |
|-------|---------|--------|
| **Model** | `com.mowercare.model` | `Issue`, `IssueChangeEvent`, `IssueStatus`, `IssuePriority`, `IssueChangeType` |
| **Repository** | `com.mowercare.repository` | `IssueRepository`, `IssueChangeEventRepository` |
| **Service** | `com.mowercare.service` | `IssueService` |
| **Tests** | `com.mowercare.service` | `IssueServiceIT` (integration test next to service under test) |

Controllers remain in `com.mowercare.controller`; future Story 3.2 HTTP will call **`IssueService`** from there.

### Architecture compliance

| Topic | Source |
|-------|--------|
| Liquibase owns schema; `ddl-auto` none | [`application.yaml`](../../apps/api/src/main/resources/application.yaml), [`architecture.md`](../planning-artifacts/architecture.md) |
| Tables `issues`, `issue_change_events`; snake_case; UUIDs | [`architecture.md` — Naming patterns](../planning-artifacts/architecture.md) |
| Layering: controller → service → repository; entities in **model** | [`architecture.md` — Layering](../planning-artifacts/architecture.md) — aligned with this repo’s `model` / `service` / `repository` split (architecture’s `com.mowercare.issue` example is **not** used) |
| Domain events naming (future): `issue.created`, `issue.assigned`, … | [`architecture.md` — Communication patterns](../planning-artifacts/architecture.md) |
| JWT / org not required for pure DB tests; use test data with org + users | Existing [`User`](../../apps/api/src/main/java/com/mowercare/model/User.java), [`Organization`](../../apps/api/src/main/java/com/mowercare/model/Organization.java) |

### Existing code to reuse / respect

| Asset | Notes |
|-------|--------|
| [`IssueStubController`](../../apps/api/src/main/java/com/mowercare/controller/IssueStubController.java) | Stubs until Epic 3 HTTP; **tag** still says Epic 3 replaces |
| [`SecurityConfig`](../../apps/api/src/main/java/com/mowercare/config/SecurityConfig.java) | Issue routes already protected — **do not** weaken |
| [`TenantPathAuthorization`](../../apps/api/src/main/java/com/mowercare/security/TenantPathAuthorization.java) | Pattern for org match when HTTP is added later |
| [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) | Base for PostgreSQL ITs |
| Prior ITs (e.g. [`OrganizationUsersIT`](../../apps/api/src/test/java/com/mowercare/controller/OrganizationUsersIT.java), [`TenantScopeIT`](../../apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java)) | MockMvc + Problem Details for **HTTP** stories; **3.1** focuses on **service/repository** ITs — **prefer** `SpringBootTest` + Testcontainers like other ITs for Liquibase-on-real-Postgres fidelity |
| FK style reference | [`0006-user-deactivation-audit.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0006-user-deactivation-audit.yaml) shows `users` self-FK pattern — apply same rigor for `assignee_user_id` / `actor_user_id` |

### Technical requirements

| Area | Requirement |
|------|----------------|
| DB | PostgreSQL 16 (Testcontainers image aligns with [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java)) |
| IDs | UUID PKs for `issues` and history rows exposed to future API |
| Timestamps | `Instant` in Java; **`timestamptz`** in DB; UTC |
| Tenant isolation | **Every** issue and history row carries `organization_id`; **repository queries** must filter by org for production code paths |
| Material fields | Document the canonical set in **one** enum or constant class — **Stories 3.5–3.7** build on this |

### Library / framework requirements

| Layer | Notes |
|-------|--------|
| API | Spring Boot 3.x, Spring Data JPA, Hibernate, Liquibase — match [`pom.xml`](../../apps/api/pom.xml) |

### File structure requirements

| Area | Guidance |
|------|----------|
| Changelog | `apps/api/src/main/resources/db/changelog/changes/0008-*.yaml` (adjust number if conflict) |
| Entities + enums | `apps/api/src/main/java/com/mowercare/model/` — `Issue`, `IssueChangeEvent`, `IssueStatus`, `IssuePriority`, `IssueChangeType` |
| Repositories | `apps/api/src/main/java/com/mowercare/repository/` — `IssueRepository`, `IssueChangeEventRepository` |
| Service | `apps/api/src/main/java/com/mowercare/service/IssueService.java` |
| Tests | `apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java` (PostgreSQL + Liquibase integration); `IssueServiceTest.java` (Mockito **unit** tests, no Spring context) |

### Testing requirements

- **Required:** `mvn -q test` (or project-standard `mvn verify`) green for `apps/api`
- **Docker:** Testcontainers needs Docker available (same as other ITs)
- **Unit tests (`IssueServiceTest`):** Mock repositories; cover not-found paths (organization, actor, assignee, issue), no-op `updateStatus`, successful `createIssue` + CREATED event, `updateStatus` / `updateAssignee` / `updatePriority` / `updateTitle` event payloads; uses `ReflectionTestUtils` to set entity ids in memory (same pattern as other fast service tests).
- **Integration tests (`IssueServiceIT`):** Full stack through `IssueService` + real DB; extended with **ASSIGNEE_CHANGED**, **PRIORITY_CHANGED**, **TITLE_CHANGED** + **DESCRIPTION_CHANGED**, and **wrong `organizationId`** → `ResourceNotFoundException` on update.

### Previous story intelligence (2.6)

- **Employee-only** guardrails and **no customer signup** routes — **do not** add public issue creation without auth (future stories use JWT + tenant)
- **401 vs 404** security semantics — **not** central to **3.1** (no new public routes expected)

### Git intelligence (recent commits)

- **`08e8976`** — Story **2.6** employee-only guardrails — docs + `EmployeeOnlyAccessGuardrailsIT`
- **`a6e9bd4`** — **2.5** mobile Settings/Team
- Epic 2 established **RBAC**, **org-scoped** user APIs, **deactivation** audit patterns — **issue history** is a **parallel** audit concept for **issue** domain

### Latest tech notes

- No Spring Boot major upgrade required for this story; use **existing** BOM versions from `pom.xml`.

### Project context reference

- No `project-context.md` in repo — use this file + [`architecture.md`](../planning-artifacts/architecture.md) + [`epics.md`](../planning-artifacts/epics.md).

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

None.

### Completion Notes List

- **Liquibase `0008`:** Tables `issues` (org FK RESTRICT, assignee FK SET NULL), `issue_change_events` (issue FK CASCADE, org RESTRICT, actor RESTRICT); indexes `idx_issues_organization_id`, `idx_issues_organization_id_status`, `idx_issue_change_events_issue_occurred`. History model: `change_type` + `old_value` / `new_value` text.
- **Domain:** **`com.mowercare.model`** — `Issue`, `IssueChangeEvent`, `IssueStatus`, `IssuePriority`, `IssueChangeType`; **`com.mowercare.repository`** — `IssueRepository`, `IssueChangeEventRepository`; **`com.mowercare.service.IssueService`** — create + updates for all material fields; single `appendEvent` path. **No** `com.mowercare.issue` package — matches rest of API layout.
- **Tests:** [`IssueServiceIT`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java) — create + CREATED, status + STATUS_CHANGED, tenant isolation query, no-op status; **extended** — assignee change + ASSIGNEE_CHANGED, priority + PRIORITY_CHANGED, title/description + TITLE_CHANGED/DESCRIPTION_CHANGED, wrong-org update → `ResourceNotFoundException`. [`IssueServiceTest`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceTest.java) — Mockito unit tests for `IssueService` (failure paths, event captors, no duplicate saves on no-op).
- **Stub:** `IssueStubController` unchanged at HTTP level; TODO for Story 3.2.
- **Docs:** `docs/rbac-matrix.md` note that persistence exists while routes stay stubbed.
- **Verification:** `mvn test` (`apps/api`) green.

### File List

- `apps/api/src/main/resources/db/changelog/changes/0008-issues-and-history.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/mowercare/model/Issue.java`
- `apps/api/src/main/java/com/mowercare/model/IssueChangeEvent.java`
- `apps/api/src/main/java/com/mowercare/model/IssueChangeType.java`
- `apps/api/src/main/java/com/mowercare/model/IssuePriority.java`
- `apps/api/src/main/java/com/mowercare/model/IssueStatus.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/IssueChangeEventRepository.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java`
- `apps/api/src/test/java/com/mowercare/service/IssueServiceTest.java`
- `apps/api/src/main/java/com/mowercare/controller/IssueStubController.java`
- `docs/rbac-matrix.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Validation record (create-story / checklist.md)

**Validated:** 2026-03-31 against `checklist.md`, `epics.md` Story **3.1**, `architecture.md`, `application.yaml`, and Liquibase sequence through **0007**.

### Critical (addressed in file)

| Issue | Resolution |
|-------|------------|
| **Hibernate “validates” with `ddl-auto: none`** | Misleading — with **`none`**, Hibernate does not auto-validate schema. AC3 and Dev Notes now state mappings must align with Liquibase; errors appear at startup/query; **no** `ddl-auto=update` in deploy. |
| **FR11–FR18 wording** | Could imply implementing **FR11–FR17** in 3.1 — clarified: **persistence only**; functional FR11+ ship in **3.2+**. |
| **Tenant test vagueness** | “Pattern used elsewhere” was weak — now points to **`TenantScopeIT`** for intent + explicit **`findByIdAndOrganizationId`**-style persistence assertion. |

### Enhancements applied

| Addition | Benefit |
|----------|---------|
| **Next changelog `0008`** | Removes ambiguity (master currently ends at **0007**). |
| **`OrganizationUsersIT` / `TenantScopeIT` links** | Clearer test navigation for implementers. |
| **`0006` FK reference** | Consistent FK authoring for user references. |
| **`{{agent_model_name_version}}` placeholder** | Replaced with fill-on-completion line. |

### Checklist categories (summary)

| Category | Outcome |
|----------|---------|
| Reinvention / reuse | Pass — service owns history; reuse IT/Testcontainers patterns |
| Technical accuracy | Fixed — ddl-auto semantics |
| File locations | Pass — `com.mowercare.model` / `repository` / `service`, `db/changelog/changes/0008-*.yaml` |
| Regression / scope | Pass — stub optional; no Epic 4 tables |
| Previous story continuity | Pass — tenant + employee-only context preserved |

### Residual risks (acceptable for ready-for-dev)

- Exact **MVP field list** and **status enum** values may be **tweaked** in **3.5**; keep **migrations additive** (new changesets) for changes.
- **`IssueStubController`** duplication until **3.2** — acceptable.

## Change Log

- **2026-03-31:** Code review — `createIssue` validates non-null `title` / `status` / `priority`; `IssueServiceTest` covers null title + customer/site label updates; `mvn test` green; status → **done**.
- **2026-03-31:** Implemented Story 3.1 — Liquibase `issues` / `issue_change_events`, JPA + `IssueService`, `IssueServiceIT`, stub TODO + rbac-matrix note; `mvn test` green; status → **review**.
- **2026-03-31:** Refactor — moved issue types from **`com.mowercare.issue`** into **`com.mowercare.model`** (entities + enums), **`com.mowercare.repository`** (repos), **`com.mowercare.service`** (`IssueService`, `IssueServiceIT`). Public constructors/setters on entities where **`IssueService`** must mutate across packages; removed empty `issue` package.
- **2026-03-31:** QA — added **`IssueServiceTest`** (Mockito unit tests for `IssueService`); extended **`IssueServiceIT`** with assignee/priority/title/description history cases and wrong-org `ResourceNotFoundException`; `mvn test` green.

## Clarifications / questions saved for end

_None._
