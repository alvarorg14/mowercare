# Story 1.2: Organizations and user membership data model

Status: done

<!-- Ultimate context engine analysis completed — comprehensive developer guide for implementation. -->

## Story

As a **system**,
I want **organizations and users stored with strict org scoping**,
so that **tenant isolation can be enforced on every data path (FR1, FR3)**.

**Implements:** FR1, FR3; NFR-S3 (foundation); Additional Requirements — shared-schema `organization_id`, UUID IDs for tenant-owned rows.

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.1** (monorepo + Liquibase baseline); **precedes** 1.3 (bootstrap first org/admin), 1.4 (auth API), 1.5 (JWT + tenant on routes). **Do not** add public REST endpoints for org/user management in this story unless needed for tests (prefer none — persistence + repository tests only).

## Acceptance Criteria

1. **Given** Liquibase changesets **when** migrations run **then** `organizations` and `users` tables exist with:
   - **UUID** primary keys (`id`) on both tables, **snake_case** columns, **plural** table names per architecture.
   - `users.organization_id` **NOT NULL** with foreign key to `organizations(id)` (ON DELETE behavior: **RESTRICT** or **NO ACTION** default — avoid CASCADE that could silently wipe users; document choice in Dev Notes).
   - **Timestamps:** `created_at` / `updated_at` on both tables (UTC, `timestamp with time zone` in PostgreSQL; map to `Instant` or `OffsetDateTime` in JPA per team convention — **pick one and use consistently**).
   - **Organization profile:** at minimum a **`name`** column on `organizations` (varchar, reasonable length e.g. 255) so **Story 1.3** and **FR2** have a field to populate without another migration — nullable only if epics explicitly required empty org; prefer **NOT NULL** with placeholder constraint or allow empty string per product — **recommend NOT NULL** once 1.3 creates org with a name.
2. **Given** the schema **when** constraints are applied **then** **exactly one organization per user row** is represented: each `users` row has exactly one `organization_id` (multi-org membership is impossible without an extra table — out of scope for v1).
3. **Given** integration tests with **Testcontainers** PostgreSQL (same pattern as **1.1**) **when** tests run **then** they prove:
   - A user can be persisted and loaded by id with **non-null** `organization_id`.
   - Invalid `organization_id` (nonexistent UUID) **fails** at insert (FK).
   - Optional: two users in the **same** organization persist successfully (sanity for multi-user orgs).
4. **Given** repository/service code loads a user by id **when** used in tests **then** the loaded entity always exposes **`organizationId`** for use in later authorization stories (no lazy omission — field mapped and populated).

## Tasks / Subtasks

- [x] **Liquibase** (AC: 1, 2)
  - [x] Add a new changelog file under `apps/api/src/main/resources/db/changelog/changes/` (e.g. `0002-organizations-and-users.yaml`) with id/author conventions matching **0001-baseline**.
  - [x] Include the new file from `db.changelog-master.yaml`.
  - [x] Create `organizations` and `users` tables + FK + indexes as needed (`idx_users_organization_id` per architecture index naming).
- [x] **JPA entities & repositories** (AC: 1, 3, 4)
  - [x] Add `Organization` and `User` entities under **`com.mowercare.model`**. Map to `organizations` / `users` with `@Column(name = "...")` for snake_case.
  - [x] Use **UUID** for ids (`@Id`); generation: **UUID v4** via Hibernate/JPA generation or DB default — prefer aligned with architecture “UUID primary keys for entities exposed to clients”.
  - [x] Add Spring Data JPA repositories (`OrganizationRepository`, `UserRepository`) with `findById` at minimum.
- [x] **Integration tests** (AC: 3, 4)
  - [x] `@DataJpaTest` + Testcontainers **or** `@SpringBootTest` + Testcontainers — follow **1.1** (`ApiApplicationTests` pattern with `@DynamicPropertySource`).
  - [x] Tests insert org + user(s), fetch user, assert `organizationId` present; assert FK violation for bad org id.

**Implementation note:** Integration tests live in `OrganizationMembershipPersistenceTest` (suffix `Test`) so Maven Surefire’s default `**/Test*.java` / `**/*Test.java` pattern picks them up; `*IT.java` is not included by default.

## Dev Notes

### Scope boundaries (do not overscope)

- **In this story:** Schema + Liquibase + JPA entities + repositories + integration tests. **No** JWT, **no** Spring Security changes beyond what already exists, **no** bootstrap script (1.3), **no** login API (1.4), **no** roles/RBAC columns (Epic 2) unless architecture mandates a placeholder — **defer role** to stories that define `Admin`/`Technician`.
- **No** REST controllers required for acceptance; **no** OpenAPI exposure required here (comes with auth/org API stories).

### Architecture compliance

- **Liquibase owns schema:** `spring.jpa.hibernate.ddl-auto` stays **`none`** (or `validate`) — **no** `update` in deployed profiles. [Source: `../planning-artifacts/architecture.md` — Data architecture, JPA vs migrations]
- **Naming:** Tables plural `snake_case`; columns `snake_case`; FK `organization_id`; indexes `idx_{table}_{columns}`. [Source: `../planning-artifacts/architecture.md` — Naming patterns → Database]
- **Multi-tenancy:** Shared schema + **`organization_id`** on tenant-owned rows; future entities follow same pattern. [Source: `../planning-artifacts/architecture.md` — Data architecture]
- **Layering:** Controllers are out of scope; repositories live in persistence layer; if a thin `OrganizationService`/`UserService` helps tests clarity, keep it minimal. [Source: `../planning-artifacts/architecture.md` — Structure patterns → Backend]
- **IDs:** UUID for primary keys. [Source: `../planning-artifacts/architecture.md` — Data architecture]
- **Package:** JPA types live in **`com.mowercare.model`**, Spring Data repos in **`com.mowercare.repository`** (not under a feature `organization` package). [Source: `../planning-artifacts/architecture.md` — Requirements → code mapping; layout adjusted for clarity.]

### Technical requirements

| Area | Requirement |
|------|-------------|
| API | Spring Boot **4.x**, **Java 25**, Maven (`mvn` on PATH, **no** wrapper) — consistent with **1.1** |
| DB | PostgreSQL; Liquibase changelogs under `db/changelog/` |
| Tests | Testcontainers PostgreSQL aligned with existing `ApiApplicationTests` setup |

### Library / framework requirements

- **Spring Data JPA:** repositories as standard interfaces extending `JpaRepository<..., UUID>` (or `Uuid` type — match entity id type).
- **Testcontainers:** reuse dependency versions from `pom.xml` from story **1.1**; avoid introducing duplicate container statics that fight parallel test execution — see **Previous story intelligence** if adding second test class.

### File structure requirements

Extend layout from **1.1** [Source: `../planning-artifacts/architecture.md` — Project structure]:

```
apps/api/src/main/java/com/mowercare/
  model/
    Organization.java
    User.java
  repository/
    OrganizationRepository.java
    UserRepository.java
  (optional thin *Service.java for tests)
apps/api/src/main/resources/db/changelog/
  db.changelog-master.yaml          # include new change file
  changes/
    0001-baseline.yaml
    0002-organizations-and-users.yaml  # new
apps/api/src/test/java/com/mowercare/persistence/
  OrganizationMembershipPersistenceTest.java
```

### Testing requirements

- **Integration tests** must run in CI (`mvn verify`) — same bar as **1.1**.
- Prefer **repository-level** or **slice + container** tests that prove FK and mappings without requiring HTTP.
- Do **not** weaken Docker requirement for API tests introduced in **1.1** (README already documents Docker for Testcontainers).

### Previous story intelligence

- **1.1** established: Spring Boot **4.0.5**, Java **25**, Liquibase master + baseline, `ddl-auto=none`, permissive `SecurityConfig`, Testcontainers + `@DynamicPropertySource` in `ApiApplicationTests`.
- **Deferred:** static `PostgreSQLContainer` field — if this story adds **another** test class with a container, watch for parallel lifecycle issues; consider shared abstract base test later.
- **pom.xml:** Initializr placeholder blocks deferred — do not “fix” unless blocking.

### Git intelligence

- Latest feature work: `feat: scaffold Expo and Spring Boot monorepo with CI` — only `ApiApplication`, `SecurityConfig`, baseline changelog. **No** domain entities yet — this story introduces first domain tables.

### Latest tech notes (verify at implementation time)

- **PostgreSQL FK:** `users.organization_id` references `organizations(id)` — create **organizations** before **users** in migrations (order of changesets).
- **Hibernate 6 / Boot 4:** use current recommended UUID mapping (`UUID` type with `@JdbcTypeCode(SqlTypes.UUID)` if needed for PostgreSQL UUID type consistency).

### Project context

- **`docs/project-context.md`** — not present; optional after more code exists (**Generate Project Context** skill).

### References

- `../planning-artifacts/epics.md` — Story 1.2 (acceptance criteria)
- `../planning-artifacts/architecture.md` — Naming, tenancy, layering, packages
- `../planning-artifacts/prd.md` — FR1, FR3, tenant isolation
- `1-1-initialize-monorepo-from-expo-and-spring-boot-starters.md` — scaffold constraints and test patterns

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent) — dev-story workflow

### Debug Log References

- FK `fk_users_organization` with `ON DELETE RESTRICT` in Liquibase; PostgreSQL enforces referential integrity on flush (`SQLState 23503` on invalid `organization_id`).

### Completion Notes List

- Added Liquibase `0002-organizations-and-users.yaml`: `organizations` and `users` with UUID PKs, `timestamptz` for `created_at`/`updated_at`, FK from `users.organization_id` to `organizations.id` (RESTRICT), index `idx_users_organization_id`.
- Implemented JPA `Organization` and `User` in `com.mowercare.model` with `GenerationType.UUID`, `@CreationTimestamp` / `@UpdateTimestamp` on `Instant`, and `User#getOrganizationId()` for stable tenant key access; repositories in `com.mowercare.repository`.
- Introduced `AbstractPostgresIntegrationTest` (shared static Testcontainers Postgres) and refactored `ApiApplicationTests` to use it; added `OrganizationMembershipPersistenceTest` covering load-by-id, FK failure for missing org, and two users in one org.
- Full `mvn verify` (4 tests) passes.

### File List

- `apps/api/src/main/resources/db/changelog/db.changelog-master.yaml`
- `apps/api/src/main/resources/db/changelog/changes/0002-organizations-and-users.yaml`
- `apps/api/src/main/java/com/mowercare/model/Organization.java`
- `apps/api/src/main/java/com/mowercare/model/User.java`
- `apps/api/src/main/java/com/mowercare/repository/OrganizationRepository.java`
- `apps/api/src/main/java/com/mowercare/repository/UserRepository.java`
- `apps/api/src/test/java/com/mowercare/testsupport/AbstractPostgresIntegrationTest.java`
- `apps/api/src/test/java/com/mowercare/ApiApplicationTests.java`
- `apps/api/src/test/java/com/mowercare/persistence/OrganizationMembershipPersistenceTest.java`

### Change Log

- 2026-03-27 — Story 1.2: Organizations/users schema (Liquibase), JPA entities and repositories, Testcontainers persistence tests, shared Postgres test base.
- 2026-03-27 — Package layout: JPA entities under **`com.mowercare.model`**, Spring Data repositories under **`com.mowercare.repository`**, integration tests under **`com.mowercare.persistence`**.

---

**Story completion status:** done — Code review complete; patch findings addressed.

### Review Findings

- [x] [Review][Patch] Maven compiler plugin redundantly sets `source`, `target`, and `release` to the same Java version — prefer `release` only for clarity and to match common Maven 3+ practice [`apps/api/pom.xml`]
- [x] [Review][Patch] Dev Agent Record change log still references old package paths (`organization.model` / `organization.repository`) while implementation uses `com.mowercare.model` / `com.mowercare.repository` — align text with actual layout [`1-2-organizations-and-user-membership-data-model.md`]
