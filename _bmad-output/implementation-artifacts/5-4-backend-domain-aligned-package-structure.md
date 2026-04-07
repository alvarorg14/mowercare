# Story 5.4: Backend domain-aligned package structure

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **developer**,
I want **Java packages grouped by domain (issue, notification, organization, auth, user, etc.) instead of a single flat `controller` / `service` / `model` / `repository` layout**,
so that **navigation and code review stay tractable as the API grows (NFR-SC1)**.

**Implements:** NFR-SC1; aligns implementation with [architecture.md](../planning-artifacts/architecture.md) — **feature-first** packages under `com.mowercare`.

**Epic:** Epic 5 — Post-MVP quality, maintainability & UX polish.

**Scope:** **Move-only refactor** unless a trivial compile-time fix is unavoidable; **no** intentional behavior, HTTP, or JSON contract changes.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 5.4])

| Epics.md clause | Implementation intent |
|-----------------|------------------------|
| **Given** `apps/api` **When** packages reorganized by domain **Then** no intentional API or JSON contract changes; OpenAPI + mobile unchanged | Preserve all `@RequestMapping` paths, request/response DTO field names, status codes, and Problem Details bodies. Verify with `./mvnw -B verify` and spot-check `/v3/api-docs` if needed. |
| **Given** moved types **When** build + tests pass **Then** no import cycles (or documented); each domain folder has clear responsibility | Use architecture target layout; split cross-domain dependencies via existing services or small shared types in `common` — avoid new circular graphs. |
| **Given** Liquibase + runtime **When** migrations + startup exercised **Then** schema + runtime match baseline | No changelog edits for package moves; run full IT suite (includes Testcontainers startup). |

---

## Acceptance Criteria

1. **Contract stability**  
   **Given** the refactored `apps/api` sources  
   **When** the API is built and exercised  
   **Then** there are **no intentional** changes to REST paths, JSON field names, HTTP status semantics, or RFC 7807 error shapes consumed by the mobile app or OpenAPI.  
   **And** `./mvnw -B verify` (from `apps/api`) passes in CI-equivalent conditions (Docker available for Testcontainers).

2. **Domain-aligned layout**  
   **Given** the codebase after the refactor  
   **When** a developer opens `src/main/java/com/mowercare/`  
   **Then** types are grouped by **domain** per [architecture.md](../planning-artifacts/architecture.md) (see **Dev Notes → Target package map**), not solely by technical layer at the top level (flat `controller/` / `service/` / `repository/` / `model/` for all domains).  
   **And** within each domain, **Spring layering** remains clear: HTTP adapters → application/services → persistence (controllers do not access `EntityManager` directly).

3. **Tests and quality gates**  
   **Given** moved production sources  
   **When** tests are updated  
   **Then** `src/test/java` **mirrors** `src/main/java` package structure ([architecture.md](../planning-artifacts/architecture.md) — Tests row).  
   **And** all existing unit and integration tests pass with **no** weakening of tenant-isolation or RBAC assertions introduced in Stories 5.1 and prior epics.

4. **Liquibase / startup**  
   **Given** no schema change in this story  
   **When** integration tests run  
   **Then** Liquibase applies as before and the application context starts with the same datasource and changelog configuration as the pre-refactor baseline.

---

## Tasks / Subtasks

- [x] **Inventory + target map** (AC: 2)
  - [x] List all `com.mowercare.*` types in `main` + `test` and assign each to a target domain package (issue, notification, organization, auth, user, common, …).
  - [x] Resolve borderline types (e.g. `TenantScope*` → `organization`; `OrganizationUser*` → `user`; JWT/session → `auth`) and document one sentence each if ambiguous.
- [x] **Execute moves** (AC: 1, 2, 4)
  - [x] Move Java files in **coherent batches** (domain-by-domain or layer-within-domain) updating `package` declarations and imports; prefer IDE refactor moves to preserve history.
  - [x] Keep `ApiApplication` at `com.mowercare` (or document if renamed — not required).
  - [x] Confirm Spring component scanning still covers all `@Configuration`, `@RestController`, `@Service`, `@Repository` beans (default scan from `com.mowercare` downward).
- [x] **Regression verification** (AC: 1, 3)
  - [x] Run `cd apps/api && ./mvnw -B verify`.
  - [x] If any IT fails, fix **imports/wiring only** unless a pre-existing bug is proven (out of scope per epic).
- [x] **Optional hygiene** (non-blocking)
  - [x] Short note in [`docs/testing-backend.md`](../../docs/testing-backend.md) if test class paths in tables need updating after moves.

### Review Findings

- [x] [Review][Patch] Remove redundant same-package imports left after mechanical moves — `IssueListQueryParser.java`, `IssueService.java`, `OrganizationUserService.java`
- [x] [Review][Patch] Normalize `IssuePriority` enum member indentation (tabs → spaces) — [`IssuePriority.java`](../../apps/api/src/main/java/com/mowercare/issue/IssuePriority.java):4-7

- [x] [Review][Defer] `InvalidScopeException` Javadoc still describes issue-list `scope` semantics while the type lives under `common.exception` [`InvalidScopeException.java`] — deferred, pre-existing wording; clarify or split type if “common” naming confuses readers.
- [x] [Review][Defer] Cross-domain dependencies are more visible after moves (`issue` ↔ `notification` entities/events; `user` services ↔ `auth` token APIs) — deferred, pre-existing architecture; document cycles if tooling flags them.
- [x] [Review][Defer] `AuthController` imports invite DTOs from `user.request` while other bodies stay under `auth.request` — deferred, pre-existing DTO ownership split.
- [x] [Review][Defer] `ApiExceptionHandlerTest` may not exercise every exception type now explicitly imported by `ApiExceptionHandler` — deferred, optional coverage expansion.
- [x] [Review][Defer] `docs/testing-backend.md` exception inventory still lists unqualified class names after exceptions scattered across packages — deferred, doc drift risk.
- [x] [Review][Defer] `LastAdminDeactivationException` / `LastAdminRemovalException` appear as delete-under-old-path plus add-under-new-path in the diff — deferred, git blame/history only (not runtime).
- [x] [Review][Defer] Residual stale Javadoc `@link` or comments pointing at old `com.mowercare.model` / `service` packages — deferred, grep/IDE follow-up.
- [x] [Review][Defer] Possible string/config references to old package names (`Class.forName`, codegen) not visible in rename hunks — deferred, low likelihood; audit if something fails at runtime.

---

## Dev Notes

### Current state (pre-story)

- **Flat layers:** Most REST types live under `com.mowercare.controller`, `com.mowercare.service`, `com.mowercare.repository`, `com.mowercare.model` (plus `model.request` / `model.response`).
- **Partial domain clustering:** `com.mowercare.issue` already holds list/sort/spec helpers; `com.mowercare.common` has shared utilities; `config`, `security`, `exception` are top-level.
- **Tests:** Mix of `controller/*IT`, `service/*Test`, `issue/*Test`, etc., mirroring the flat layout — will move with production packages.

### Target package map (authoritative: [architecture.md](../planning-artifacts/architecture.md) § “Source”)

Align **main** code with the table below. Names in **bold** are architecture package roots; adjust subpackages (`web`, `service`, `persistence`, `model`) as needed while keeping layering obvious.

| Package root | Owns (from architecture) | Illustrative migration from current |
|--------------|---------------------------|-----------------------------------|
| `common/` | Shared utilities, cross-cutting helpers, global API error handling patterns | `common/*`, `exception/*` (e.g. `ApiExceptionHandler`, problem bodies), parts of `config` if you colocate cross-cutting config |
| `organization/` | Tenancy + org profile (FR1–FR3) | `Organization*`, org profile controller, `TenantScope*`, repositories/entities for `Organization` |
| `auth/` | Login, refresh, logout, JWT (FR4–FR6) | `AuthController`, `AuthService`, `JwtService`, `OpaqueTokenService`, refresh token model/repo, auth DTOs |
| `user/` | Users, roles, invites, deactivate (FR7–FR10, FR24–FR26) | `OrganizationUser*`, employee/invite flows, RBAC helpers that are user-management scoped |
| `issue/` | Issues, filters, assignment, history (FR11–FR19) | `Issue*` controllers/services/repos/entities, `issue/*` helpers already here — consolidate |
| `notification/` | In-app + device tokens + dispatch hooks (FR20–FR23) | `Notification*`, `DevicePushToken*`, push dispatch services, FCM adapter |

**Bootstrap:** `BootstrapController` / `BootstrapService` — place under **`organization`** (tenant creation) unless the team prefers a tiny `bootstrap` subpackage under `organization`; **document the choice** in Dev Agent Record.

**Config / security:** Architecture lists “config” under `common` in the narrative; the repo today uses `com.mowercare.config` and `com.mowercare.security`. Acceptable outcomes: (a) move to `common.config` + `common.security`, or (b) keep **`com.mowercare.config`** and **`com.mowercare.security`** at top level for minimal churn — **pick one** and keep it consistent. Spring Boot must still pick up `@Configuration` classes.

### Architecture compliance

| Topic | Source |
|-------|--------|
| Feature-first Java packages | [architecture.md](../planning-artifacts/architecture.md) — Java (backend) |
| Layering: controller → service → repository | [architecture.md](../planning-artifacts/architecture.md) — Structure patterns |
| Tests mirror `main` | [architecture.md](../planning-artifacts/architecture.md) — API tests |
| REST/JSON rules unchanged | [architecture.md](../planning-artifacts/architecture.md) — REST / JSON sections |

### Library / framework requirements

- **Spring Boot 4.x**, **Java 25** — unchanged ([`apps/api/pom.xml`](../../apps/api/pom.xml)).
- **No new dependencies** for this story unless required for a compile fix (unlikely).

### File structure requirements

- All moves stay under **`apps/api/src/main/java/com/mowercare/`** and **`apps/api/src/test/java/com/mowercare/`**.
- **Liquibase:** `src/main/resources/db/changelog/**` — **no** moves required for Java package refactor.
- **Do not** change `apps/mobile` or generated OpenAPI client in this story unless you discover an accidental contract change (should not happen).

### Testing requirements

- **Mandatory:** `cd apps/api && ./mvnw -B verify` — unit + integration (Testcontainers PostgreSQL per [docs/testing-backend.md](../../docs/testing-backend.md)).
- **Representative ITs** (paths will shift with packages): `TenantScopeIT`, `AuthIT`, `Issue*IT`, `NotificationInboxIT`, etc. — all must remain green.
- **Regression focus:** tenant denial, JWT auth, RBAC, issue mutations, notification hooks — already covered; this story must **not** delete or weaken tests.

### Previous story intelligence (5.3)

- Story 5.3 added Maestro E2E and docs; **API contract** is still validated primarily by **`mvn verify`** and backend ITs. After package moves, run the same command before merge.
- No requirement to change `.maestro/` or mobile for 5.4 unless a **wrong** API path was introduced (failure mode should be caught by ITs).

### Git intelligence (recent commits)

- Recent Epic 5 work: **5.1** tests/docs, **5.2** mobile Jest, **5.3** Maestro — backend **package layout** is the next maintainability step; keep diffs **mechanical** (move + import) to ease review.

### Latest technical notes

- **IDE-assisted move** (IntelliJ “Move class” / “Move package”) reduces missed references vs. manual copy-paste.
- **Circular dependencies:** If two domains must call each other, prefer **one direction** (e.g. issue → notification events) or extract a **narrow interface** in `common` — document any new cross-domain edge in Dev Agent Record.

### Project context reference

- No `project-context.md` in repo; **`epics.md`**, **`architecture.md`**, and **`docs/testing-backend.md`** govern this work.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `cd apps/api && mvn -B verify` — **PASS** (full unit + integration tests, 2026-04-07).

### Completion Notes List

- Reorganized `apps/api` Java sources from flat `controller` / `service` / `repository` / `model` / `exception` into **domain packages**: `auth`, `user`, `organization`, `issue`, `notification`, plus `common.exception` for shared API error handling and existing `common` utilities; left **`com.mowercare.config`** and **`com.mowercare.security`** unchanged at top level (story-allowed option).
- **Bootstrap** (`BootstrapController`, `BootstrapService`) lives under **`organization`** (tenant creation).
- Cross-package JPA relations and `ApiExceptionHandler` required **explicit imports** where types previously shared `com.mowercare.exception` without imports.
- Updated [`docs/testing-backend.md`](../../docs/testing-backend.md): IT links and DTO inventory line reflect new packages.

### File List

- `apps/api/src/main/java/com/mowercare/**` (domain package layout; all moved/updated `.java`)
- `apps/api/src/test/java/com/mowercare/**` (mirrored layout)
- `docs/testing-backend.md`

### Change Log

- 2026-04-07: Story 5.4 — domain-aligned package structure; `mvn -B verify` green; sprint status → **review**.

---

## Create-story validation record

**Checklist:** [`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md)  
**Verdict:** Pass — story maps epic ACs, architecture target packages, verification command, and regression focus; `project-context.md` absent noted.
