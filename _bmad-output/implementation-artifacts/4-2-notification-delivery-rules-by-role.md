# Story 4.2: Notification delivery rules by role

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As a **system**,
I want **to deliver notifications to eligible employees per role and routing rules**,
so that **the right people are informed (FR21)**.

**Implements:** **FR21**.

**Epic:** Epic 4 — Awareness — in-app notifications & push.

### Epic traceability ([Source: `../planning-artifacts/epics.md` — Story 4.2])

| Epics.md | Implementation intent |
|----------|------------------------|
| **Given** an event **When** fan-out runs **Then** rules define recipients (e.g. assignee, admins, watchers — as per locked matrix) and are covered by tests | **Backend:** persist **per-recipient** rows linked to **`notification_events`**, evaluated in the **same transaction** as the domain event where practical. **Locked MVP matrix** is below (watchers/subscription tiers **out of scope** — no schema in repo). |
| **Given** a user should not receive an event **When** rules evaluate **Then** no spurious notification row is created for that user | Enforce **actor exclusion**, **inactive account exclusion**, **deduplication**, and **matrix eligibility** in code + tests. |

### Cross-story boundaries

| Story | Relationship |
|-------|--------------|
| **4.1** | **Depends on** canonical `notification_events` rows — fan-out **runs after** each persisted `NotificationEvent` (same transaction as issue mutation). |
| **4.3** | Will **read** per-user recipient rows for in-app feed; 4.2 must persist **org-scoped, user-keyed** rows queryable by `(organization_id, recipient_user_id)`. |
| **4.4–4.5** | Push uses recipient identity later; **no** device tokens or push sends in 4.2. |

---

## MVP locked routing matrix (authoritative for implementation)

Epics reference “assignee, admins, watchers — as per locked matrix.” **Watchers** and **subscription** preferences are **not** modeled in v1 data — treat as **future**. This table is the **FR21** routing contract for MVP:

| `event_type` (taxonomy string) | Eligible recipients | Notes |
|--------------------------------|-------------------|--------|
| `issue.created` | All **ACTIVE** users with role **ADMIN** in the org; plus **assignee** if `assignee` is non-null, **ACTIVE**, and not the actor | Technicians who are **not** assignee do **not** receive create events (role-aware triage). |
| `issue.assigned` | The **new assignee** if non-null, **ACTIVE**, and not the actor; plus all **ACTIVE ADMINs** | **Unassign** (new assignee **null**): only the **ADMIN** leg applies (still emit fan-out — no assignee candidate). Dedupe if assignee is also an admin (one row). |
| `issue.status_changed` | **Assignee** if non-null, **ACTIVE**, and not the actor; plus all **ACTIVE ADMINs** | Same dedupe rule. |

**Global rules (all event types):**

1. **Actor exclusion:** Never insert a recipient row for **`actor_user_id`** from the parent `notification_events` row (applies to **assignee** candidates and **admin** candidates — e.g. an **ADMIN** actor must not receive an admin-targeted row).
2. **Account status:** Only **ACTIVE** users ([`AccountStatus`](../../apps/api/src/main/java/com/mowercare/model/AccountStatus.java)) are eligible.
3. **Tenant scope:** All rows carry **`organization_id`** consistent with the issue and notification event.
4. **Uniqueness:** At most **one** recipient row per `(notification_event_id, recipient_user_id)`.

**Out of scope for 4.2:** “Watchers,” **per-user notification preferences**, **email/SMS**, **push delivery**, **HTTP APIs** for listing (Story **4.3**), **subscription tiers** (PRD language — no billing data; ignore for MVP).

---

## Acceptance Criteria

1. **Persistence — Liquibase + JPA**  
   **Given** PostgreSQL and the existing changelog chain ([`db.changelog-master.yaml`](../../apps/api/src/main/resources/db/changelog/db.changelog-master.yaml))  
   **When** migrations apply  
   **Then** a new table exists (e.g. **`notification_recipients`**) with at least:
   - `id` **UUID** PK  
   - `organization_id` **UUID** NOT NULL FK → `organizations(id)`  
   - `notification_event_id` **UUID** NOT NULL FK → `notification_events(id)` **ON DELETE CASCADE** (if the parent event is removed, recipients go too)  
   - `recipient_user_id` **UUID** NOT NULL FK → `users(id)`  
   - `created_at` **timestamptz** NOT NULL (UTC)  
   **And** `UNIQUE (notification_event_id, recipient_user_id)`  
   **And** indexes support Story **4.3** lookups: e.g. `(organization_id, recipient_user_id, created_at DESC)` and FK indexes as appropriate.

2. **Fan-out service**  
   **Given** a persisted [`NotificationEvent`](../../apps/api/src/main/java/com/mowercare/model/NotificationEvent.java) from **4.1**  
   **When** fan-out runs for that event  
   **Then** recipient rows are inserted **only** for users allowed by the **MVP matrix** and **global rules** above  
   **And** implementation is **unit-testable** (pure rule logic separated from persistence where practical).

3. **Transactional integration**  
   **Given** [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) / [`NotificationEventRecorder`](../../apps/api/src/main/java/com/mowercare/service/NotificationEventRecorder.java) already persist notification events inside issue transactions  
   **When** an MVP notification event is recorded  
   **Then** fan-out for that event runs in the **same transaction** (no partial state: issue + `notification_events` + `notification_recipients` commit together, or all roll back).

4. **No spurious recipients**  
   **Given** actor exclusion and role rules  
   **When** integration tests exercise create / assign / status flows  
   **Then** the **actor** never receives a recipient row; **DEACTIVATED** / **PENDING_INVITE** users never receive rows; duplicate user ids for one event cannot be persisted (unique constraint + application logic).

5. **Tests**  
   **Given** [`AbstractPostgresIntegrationTest`](../../apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java) patterns  
   **When** `mvn test` runs  
   **Then** tests cover: at least one scenario per **event type** in the matrix; **admin + assignee dedupe** where both apply; **actor exclusion**; **tenant** boundaries (recipients only for users in the same org as the issue).

6. **Scope**  
   **Given** this is **4.2**  
   **When** complete  
   **Then** there is **no** public REST API for notifications, **no** mobile changes, **no** push, **no** device tokens — those are **4.3+**.

---

## Tasks / Subtasks

- [x] **Schema** (AC: 1, 6)
  - [x] New Liquibase **`0011-...yaml`** (next after [`0010-notification-events.yaml`](../../apps/api/src/main/resources/db/changelog/changes/0010-notification-events.yaml)); include in master changelog.
  - [x] JPA entity + repository under existing packages ([`com.mowercare.model`](../../apps/api/src/main/java/com/mowercare/model/), [`com.mowercare.repository`](../../apps/api/src/main/java/com/mowercare/repository/)) unless you introduce `com.mowercare.notification` with a clear migration path (prefer consistency with **4.1** flat layout unless moving multiple types).

- [x] **User queries** (AC: 2, 4)
  - [x] Repository method(s) to list **ACTIVE** users by org + role (e.g. all **ADMIN**s) — add to [`UserRepository`](../../apps/api/src/main/java/com/mowercare/repository/UserRepository.java) if not already available without loading entire org and filtering in memory.

- [x] **Fan-out** (AC: 2, 3, 4)
  - [x] `NotificationRecipientFanout` / `NotificationDeliveryService` (name TBD): input = `NotificationEvent` + loaded `Issue` (for assignee); output = set of recipient `User` ids per matrix.
  - [x] Invoke from the same path as `NotificationEventRecorder` (after `save` of `NotificationEvent`, or inside recorder) — **do not** spawn async work for MVP.

- [x] **Tests** (AC: 5)
  - [x] Extend [`IssueServiceIT`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java) or add focused IT with multi-user fixtures (admin + technician + second admin if needed), asserting recipient counts and identities.

### Review Findings

- [x] [Review][Patch] Add integration assertions for `issue.assigned` fan-out (`notification_recipients`) — AC5 requires at least one scenario per matrix event type; `given_assigneeChange_whenUpdateAssignee_thenIssueAssignedNotification` asserts `notification_events` but not per-recipient rows for the assigned event (include unassign → admins-only if covered). [`IssueServiceIT.java`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java) (~263–291) — addressed: recipient assertions on assign + unassign-only-admins IT.

- [x] [Review][Patch] Add integration tests proving DEACTIVATED / PENDING_INVITE users never get recipient rows — AC4. [`IssueServiceIT.java`](../../apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java) — addressed: `PENDING_INVITE` admin excluded; deactivated assignee yields no rows.

- [x] [Review][Patch] Fix `sprint-status.yaml` `last_updated` regression (diff shows `2026-04-06T22:00:00Z` → `21:00:00Z` alongside status flip; likely accidental). [`sprint-status.yaml`](./sprint-status.yaml):~1-6 — addressed: restored `22:00:00Z`.

- [x] [Review][Defer] No integration test proving rollback when fan-out/persistence fails — AC3 atomicity is satisfied by code path but not failure-injected; deferred, pre-existing test-style gap. — deferred, pre-existing

---

## Dev Notes

### Current codebase anchors

| Area | Location |
|------|----------|
| Domain event rows | [`NotificationEvent`](../../apps/api/src/main/java/com/mowercare/model/NotificationEvent.java), [`NotificationEventRecorder`](../../apps/api/src/main/java/com/mowercare/service/NotificationEventRecorder.java) |
| Taxonomy | [`NotificationEventType`](../../apps/api/src/main/java/com/mowercare/model/NotificationEventType.java) |
| Issue context | [`Issue`](../../apps/api/src/main/java/com/mowercare/model/Issue.java) — `assignee`; [`IssueService`](../../apps/api/src/main/java/com/mowercare/service/IssueService.java) |
| Roles | [`UserRole`](../../apps/api/src/main/java/com/mowercare/model/UserRole.java) — **ADMIN**, **TECHNICIAN** |
| RBAC product rules | [`docs/rbac-matrix.md`](../../docs/rbac-matrix.md) — API matrix; **notification routing** is specified **in this story** (add a short pointer from docs **optional** — not required for AC). |

### Architecture compliance

| Topic | Source |
|-------|--------|
| Domain event naming + payload baseline | [`architecture.md`](../planning-artifacts/architecture.md) — Communication patterns |
| Multi-tenant scope | Every read/write filtered by **`organization_id`**; tests prove cross-tenant denial |
| `notification/` package (logical) | Architecture maps FR20–FR23 to notification area — code may stay under `service`/`model` until a package split ([Source: 4.1 dev notes](4-1-notification-records-and-issue-event-taxonomy.md)) |

### Library / framework

- **Spring Boot**, **JPA**, **Liquibase** — no new dependencies expected.
- **Hibernate `ddl-auto`:** remain **none** / **validate**; schema only via Liquibase.

### File structure (expected touchpoints)

| Path | Purpose |
|------|---------|
| `apps/api/src/main/resources/db/changelog/changes/0011-*.yaml` | `notification_recipients` table |
| `apps/api/.../model/NotificationRecipient.java` (name TBD) | Entity |
| `apps/api/.../repository/NotificationRecipientRepository.java` | Spring Data |
| `apps/api/.../service/*Fanout*.java` or extended recorder | Fan-out orchestration |

### Previous story intelligence (4.1)

- **`notification_events`** is **one row per material domain event** — recipients **must not** duplicate that row per user; use a **child** table.
- **`NotificationEventRecorder.recordIfMvp`** is the single hook after [`IssueChangeEvent`](../../apps/api/src/main/java/com/mowercare/model/IssueChangeEvent.java) persistence — extend or chain here for **4.2**.
- **4.1** file list and migration **`0010`** — follow same Liquibase and IT patterns; see [4.1 Dev Agent Record](4-1-notification-records-and-issue-event-taxonomy.md).

### Git intelligence (recent commits)

- **4.1** introduced `notification_events`, `NotificationEventRecorder`, and `IssueService` hooks — **extend** that path rather than parallel emission.

### Latest tech information

- No new runtime stack; use **Java streams** and a **deduplicated set of recipient user ids**; **Spring `@Transactional`** propagation unchanged on `IssueService` entry points.

### Project context reference

- No **`project-context.md`** in repo; rely on architecture + **this story** + linked code.

---

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

### Completion Notes List

- Liquibase **`0011-notification-recipients`**: `notification_recipients` with FK to `notification_events` **ON DELETE CASCADE**, unique `(notification_event_id, recipient_user_id)`, index `(organization_id, recipient_user_id, created_at DESC)`.
- **`NotificationRecipientRules`**: single pure resolver for MVP matrix (`issue.created` / `issue.assigned` / `issue.status_changed` share the same eligible set: ACTIVE admins + optional ACTIVE assignee, minus actor).
- **`NotificationRecipientFanoutService`** invoked from **`NotificationEventRecorder`** after persisting each `NotificationEvent` (same transaction).
- **`UserRepository.findByOrganization_IdAndRoleAndAccountStatus`** for ACTIVE admins only.
- **`NotificationEventType.fromTaxonomyValue`** for fan-out dispatch.
- Tests: **`NotificationRecipientRulesTest`**, extended **`IssueServiceIT`**, **`NotificationEventTypeTest`** for taxonomy round-trip.

### File List

- `apps/api/src/main/resources/db/changelog/changes/0011-notification-recipients.yaml`
- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/java/com/mowercare/model/NotificationRecipient.java`
- `apps/api/src/main/java/com/mowercare/model/NotificationEventType.java`
- `apps/api/src/main/java/com/mowercare/repository/NotificationRecipientRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationRecipientRules.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationRecipientFanoutService.java`
- `apps/api/src/main/java/com/mowercare/service/NotificationEventRecorder.java`
- `apps/api/src/test/java/com/mowercare/service/NotificationRecipientRulesTest.java`
- `apps/api/src/test/java/com/mowercare/service/IssueServiceIT.java`
- `apps/api/src/test/java/com/mowercare/model/NotificationEventTypeTest.java`

### Change Log

- **2026-04-06:** Story 4.2 — notification recipient fan-out (FR21), schema 0011, rules + fan-out service, IT/unit tests; `mvn test` green.

---

## Story completion status

- **Status:** done  
- **Note:** Code review patch items applied; defer item remains in `deferred-work.md`.

### validate-create-story (`.cursor/skills/bmad-create-story/checklist.md`)

**Validation run:** 2026-04-06 — checklist-driven pass against `epics.md`, `architecture.md`, `IssueService` / `NotificationEventRecorder`, and Story **4.1**.

| Layer | Result | Notes |
|-------|--------|--------|
| Epics **4.2** / FR21 / both epic ACs | **Pass** | Traceability table + locked matrix + tests AC cover “fan-out + rules + no spurious rows.” |
| Architecture | **Pass** | Tenant scope, domain taxonomy, transactional issue pipeline; no REST/push in scope. |
| Reinvention / reuse | **Pass** | Child table + hook after `NotificationEventRecorder`; `User` / `Issue` / `AccountStatus` / `UserRole`. |
| Integration reality | **Pass** | `appendEvent` → `recordIfMvp` is the single hook; `Issue` state matches assign/status after save for fan-out. |
| Edge: `issue.assigned` + null assignee | **Pass** (after edit) | **Unassign** path noted in matrix — 4.1 still emits `issue.assigned` for clear-assignee ([`NotificationEventType`](../../apps/api/src/main/java/com/mowercare/model/NotificationEventType.java) + `ASSIGNEE_CHANGED`). |
| Scope creep | **Pass** | AC6; watchers/subscriptions explicitly deferred. |
| Regression risk | **Pass** | AC4–5 require actor + inactive exclusion + IT coverage. |

**Enhancement applied during validation:** Clarified **unassign** recipient behavior and **actor exclusion** for **ADMIN** candidates (avoids mis-implementing “notify all admins” as including the actor).

**Critical issues:** None.

**Outcome:** **Ready for `bmad-dev-story`** — re-run validation after implementation or if `epics.md` / notification taxonomy changes.
