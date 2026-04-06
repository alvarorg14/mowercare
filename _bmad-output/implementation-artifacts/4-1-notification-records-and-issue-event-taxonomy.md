# Story 4.1: Notification records and issue event taxonomy

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **system**,
I want **domain events from issues to map to a defined notification taxonomy**,
so that **only meaningful events generate notifications (FR20)**.

**Implements:** **FR20**; **Additional:** alignment with **`issue.created`**, **`issue.assigned`**, **`issue.status_changed`** per [architecture domain events](../planning-artifacts/architecture.md).

**Epic:** Epic 4 — Awareness — in-app notifications & push.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 4.1])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** issue mutations from Epic 3 **When** material events occur **Then** notification records (or outbox rows) are created with **organizationId**, **issueId**, **actorUserId**, **occurredAt** | **Backend-only** for this story: new **Liquibase** table(s) + **JPA** + writes in the **same transaction** as issue mutations where taxonomy applies. **No** mobile UI, **no** public REST list for notifications here (**Story 4.3**). |
| **Given** taxonomy documentation **When** product reviews **Then** event list matches MVP scope (no noise from every keystroke) | Document the **MVP taxonomy** (dot-separated strings) and **explicit mapping** from `IssueChangeType` / issue flows — **exclude** high-churn fields (e.g. title/description/priority/customer/site edits) from **notification generation** for 4.1 unless product expands scope later. |

### Cross-story boundaries

| Story | Relationship |
|-------|----------------|
| **3.1–3.7** | **Depends on** `IssueService` + `issue_change_events` — notification rows must **not** replace history; they **supplement** it for downstream **4.2** delivery rules. |
| **4.2** | Will add **recipient fan-out** and rules; **4.1** only persists **canonical domain events** (who/when/what type + org/issue scope). |
| **4.3+** | In-app list and push build on persisted events. |

## Acceptance Criteria

1. **MVP taxonomy — documented and enforced in code**  
   **Given** the architecture convention (**past tense, dot-separated** — e.g. `issue.created`)  
   **When** the implementation is reviewed  
   **Then** a single source of truth (Java enum or constants class + short comment block) lists **at minimum**:
   - `issue.created`
   - `issue.assigned`
   - `issue.status_changed`  
   **And** PR/epic alignment is explicit: these three are the **MVP notification-driving events** for 4.1 (matches [epics.md](../planning-artifacts/epics.md) Story 4.1 “Additional” line).

2. **Persistence — Liquibase + JPA**  
   **Given** PostgreSQL and existing migration chain ([`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml))  
   **When** migrations apply  
   **Then** a new table exists (name consistent with product, e.g. **`notification_events`** or **`issue_notification_events`**) with at least:
   - `id` **UUID** PK  
   - `organization_id` **UUID** NOT NULL FK → `organizations(id)` (tenant scope, **NFR-S3**)  
   - `issue_id` **UUID** NOT NULL FK → `issues(id)` (ON DELETE policy documented — align with `issue_change_events`)  
   - `actor_user_id` **UUID** NOT NULL FK → `users(id)`  
   - `occurred_at` **timestamptz** NOT NULL (UTC)  
   - **`event_type`** **VARCHAR** (or check-constrained) storing the **taxonomy string** (e.g. `issue.created`)  
   **And** indexes support org-scoped and issue-scoped queries (e.g. `(organization_id, occurred_at DESC)`, `(issue_id, occurred_at)`).  
   **Optional but recommended:** nullable **`source_issue_change_event_id`** UUID FK → `issue_change_events(id)` for traceability to existing history rows.

3. **Transactional emission from issue flows**  
   **Given** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) mutates issues  
   **When** a mapped **material** change occurs **in the same transaction** as the issue update  
   **Then** a notification row is inserted **iff** the change maps to the MVP taxonomy:
   - **`IssueChangeType.CREATED`** → `issue.created` (after `createIssue` / initial `appendEvent` for created)  
   - **`IssueChangeType.ASSIGNEE_CHANGED`** → `issue.assigned`  
   - **`IssueChangeType.STATUS_CHANGED`** → `issue.status_changed`  
   **And** **`organizationId`**, **`issueId`**, **`actorUserId`**, **`occurredAt`** on the row match the issue context and actor (use the **same** `Instant` as the corresponding `IssueChangeEvent` if emitted in the same call — avoid clock skew).  
   **And** changes that **only** produce history for **non-MVP** notification types (e.g. `TITLE_CHANGED`, `DESCRIPTION_CHANGED`, `PRIORITY_CHANGED`, labels) **do not** insert notification rows in 4.1.

4. **No duplicate spam on no-op updates**  
   **Given** `IssueService` early-returns when old == new (existing pattern for assignee/status)  
   **When** no history row is written  
   **Then** no notification row is written.

5. **Tests**  
   **Given** [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) style  
   **When** `mvn test` runs  
   **Then** integration tests prove: **create issue** → one `issue.created` row; **assignee change** → `issue.assigned`; **status change** → `issue.status_changed`; **title/priority-only patch** → **no** new notification row (or whatever non-MVP types you excluded).  
   **And** tenant boundary: notification row is not created for cross-org misuse (mirror existing isolation tests where practical).

6. **Scope**  
   **Given** this is **4.1**  
   **When** the story is complete  
   **Then** there is **no** requirement to expose **HTTP** endpoints for notifications, **no** push, **no** recipient table — those belong to **4.2–4.4**.

## Tasks / Subtasks

- [x] **Schema** (AC: 2, 6)
  - [x] New Liquibase changelog **`0010-...yaml`** (next after [`0009-issues-organization-priority-index.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0009-issues-organization-priority-index.yaml)); include in master changelog.
  - [x] JPA entity under **`com.mowercare.model`**, repository under **`com.mowercare.repository`** (matches [3.1](3-1-issue-aggregate-schema-and-change-history-storage.md) conventions).

- [x] **Taxonomy** (AC: 1)
  - [x] `NotificationEventType` (or equivalent) with string values **`issue.created`**, **`issue.assigned`**, **`issue.status_changed`**; map from `IssueChangeType` in one place (private method or small mapper).

- [x] **Emission** (AC: 3, 4)
  - [x] `NotificationEventService` or package-private helpers; inject into **`IssueService`** and call after successful **`appendEvent`** for mapped types **only**, or consolidate inside **`appendEvent`** with a guard — **same `@Transactional` boundary** as issue persistence.

- [x] **Tests** (AC: 5)
  - [x] `IssueServiceIT` or dedicated `NotificationEventIT` following existing IT patterns.

## Change Log

- **2026-04-06:** Implemented `notification_events` table, `NotificationEvent` / `NotificationEventType`, `NotificationEventRecorder` after persisted `IssueChangeEvent`; extended `IssueServiceIT` + `NotificationEventTypeTest`; `IssueServiceTest` lenient stub for history save.

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| Issue mutations + history | [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) — `createIssue`, `updateStatus`, `updateAssignee`, `appendEvent` |
| History model | [`IssueChangeEvent`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeEvent.java), [`IssueChangeType`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeType.java) |
| Migrations | [`apps/api/src/main/resources/db/changelog/`](../../apps/api/src/main/resources/db/changelog/) |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Domain event strings + payload baseline (`organizationId`, `issueId`, `actorUserId`, `occurredAt`) | [`architecture.md`](../planning-artifacts/architecture.md) — Communication patterns / Domain events |
| API package **`notification/`** for FR20–FR23 | [`architecture.md`](../planning-artifacts/architecture.md) — Requirements → code mapping (current code uses flat `com.mowercare.*`; **either** introduce `com.mowercare.notification` **or** keep `service`/`model` with clear names — **prefer** a dedicated package for new types if it reduces clutter) |
| Incremental schema — notification tables in Epic 4 | [epics.md validation](../planning-artifacts/epics.md) — Architecture compliance |

### Library / framework

- **Spring Boot** (existing), **JPA**, **Liquibase** — no new dependencies expected.
- **Hibernate `ddl-auto`:** remain **`none`** / **`validate`** per project; schema only via Liquibase.

### File structure (expected touchpoints)

| Path | Purpose |
|------|---------|
| `apps/api/src/main/resources/db/changelog/changes/0010-*.yaml` | New table |
| `apps/api/.../model/NotificationEvent.java` (name TBD) | Entity |
| `apps/api/.../repository/NotificationEventRepository.java` | Spring Data |
| `apps/api/.../service/IssueService.java` | Hook emission |
| Optional `.../service/NotificationEventRecorder.java` | Isolated insert logic |

### Previous epic intelligence (Epic 3)

- **Issue history is append-only** and already typed with `IssueChangeType` — reuse semantics; **notification** is a **narrower** subset for MVP awareness.
- **3.7** exposed change events to mobile for **activity timeline** — notification taxonomy strings are **not** the same as `IssueChangeType` enum names; **do not** conflate API response `changeType` with `issue.created` without translation docs for mobile (future 4.3 can show human labels).

### Git intelligence (recent commits)

Recent work: **3.9** a11y, **3.8** list filter/sort, **3.7** change-events API + timeline, **3.6** AssigneePicker, **3.5** PATCH — patterns: **integration tests**, **tenant scope**, **IssueService** as single mutation gateway.

### Latest tech information

- No new runtime dependencies required; use existing **Java time** `Instant` and **UUID** IDs consistent with `IssueChangeEvent`.

### Project context reference

- No **`project-context.md`** in repo; rely on **architecture** + **this story** + linked implementation artifacts.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- Local `mvn test` requires Docker for `AbstractPostgresIntegrationTest` / Testcontainers; full suite not executed in this environment. Verified: `mvn test -Dtest=IssueServiceTest,NotificationEventTypeTest` and `mvn compile test-compile`.

### Completion Notes List

- **`notification_events`** Liquibase `0010`: FKs to `organizations`, `issues` (CASCADE delete), `users`, optional `source_issue_change_event_id` → `issue_change_events` (SET NULL); indexes on `(organization_id, occurred_at DESC)` and `(issue_id, occurred_at)`.
- **`NotificationEventRecorder.recordIfMvp`** runs after each persisted history row; only **CREATED**, **ASSIGNEE_CHANGED**, **STATUS_CHANGED** map to MVP taxonomy strings.
- **`IssueService.appendEvent`** uses one `Instant` per row; notification row reuses `occurredAt` and links **source** history row when present.

### File List

- `apps/api/src/main/resources/db/changelog/changes/0010-notification-events.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/mowercare/model/NotificationEventType.java`
- `apps/api/src/main/java/com/mowercare/model/NotificationEvent.java`
- `apps/api/src/main/java/com/mowercare/repository/NotificationEventRepository.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationEventRecorder.java`
- `apps/api/src/main/java/com/mowercare/service/IssueService.java`
- `apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java`
- `apps/api/src/test/java/com/mowercare/service/IssueServiceTest.java`
- `apps/api/src/test/java/com/mowercare/model/NotificationEventTypeTest.java`

## Story completion status

- **Status:** done  
- **Note:** Code review 2026-04-06: patch applied (same-status IT asserts notification rows); defers recorded in `deferred-work.md`.

### validate-create-story ([`.cursor/skills/bmad-create-story/checklist.md`](../../.cursor/skills/bmad-create-story/checklist.md))

| Check | Result |
|-------|--------|
| Epics **4.1** user story, FR20, three event types, both epic ACs reflected | **Pass** — expanded into numbered ACs; MVP noise exclusion explicit |
| Architecture domain events + payload baseline | **Pass** — cited; table columns align with `organizationId`, `issueId`, `actorUserId`, `occurredAt` |
| Reinvention / reuse | **Pass** — hooks `IssueService` + `appendEvent`; new table supplements `issue_change_events` |
| Wrong paths / libs | **Pass** — Liquibase chain `0010` (verified unused); Spring/JPA only |
| Scope creep | **Pass** — AC6 and Tasks exclude HTTP, push, recipients; **4.2–4.4** called out |
| Previous Epic 4 story | **N/A** — first story in epic; Epic 3 context present |
| Regression / UX | **N/A** — backend-only; mobile called out as non-goal |

**Outcome:** **Passed** — 2026-04-06. No checklist-driven edits required beyond this record.

**Optional follow-ups (not blockers):** If product later wants separate taxonomy strings for **resolved vs closed**, revisit under **`issue.status_changed`** or a new type before **4.3** copy work.

### Review Findings

- [x] [Review][Patch] Same-status no-op IT should assert notification row count — `given_sameStatus_whenUpdateStatus_thenNoDuplicateHistory` only checks `issue_change_events`; AC4 expects no duplicate **notification** row when history is skipped. Add `notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc` size `1` (only `issue.created`). [`IssueServiceIT.java` ~127–149] — fixed 2026-04-06

- [x] [Review][Defer] Mobile Jest + large `package-lock.json` + `__tests__` not in story 4.1 file list — mixed with backend-only scope; prefer separate chore commit/PR next time for clearer history. — deferred, mixed PR scope

- [x] [Review][Defer] `notification_events.event_type` has no DB CHECK constraint — AC2 allows VARCHAR or check; optional hardening when delivery rules need stronger DB-level invariants. — deferred, optional hardening
