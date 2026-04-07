---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - epic-5-post-mvp-quality-addendum-2026-04-06
inputDocuments:
  - "_bmad-output/planning-artifacts/prd.md"
  - "_bmad-output/planning-artifacts/architecture.md"
  - "_bmad-output/planning-artifacts/ux-design-specification.md"
document_output_language: English
workflowStatus: step-04-validation-complete
epic5Note: "Post-MVP epic; FR1–FR29 unchanged — Epic 5 strengthens NFRs, testability, maintainability, and UX polish. Additional stories may be appended here and in sprint-status.yaml over time."
---

# mowercare - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for mowercare, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

```
FR1: The system can isolate all operational data by organization such that one organization's data is inaccessible to users of another organization.
FR2: An Org Admin can create or update organization profile fields supported in MVP (e.g. organization name).
FR3: A user can belong to exactly one organization in v1.
FR4: An employee can sign in to access the product using the authentication mechanism chosen for MVP.
FR5: An employee can sign out of the product.
FR6: The system can deny access to unauthenticated callers for protected capabilities.
FR7: An Org Admin can assign a role to each employee user from the roles supported in MVP (at minimum Admin and Technician).
FR8: The system can enforce role-based permissions for issue and administration actions (exact matrix defined in acceptance criteria).
FR9: A Technician can perform issue actions permitted for Technicians (e.g. create/update within policy).
FR10: An Admin can perform user-management and organization actions permitted for Admins.
FR11: A Technician or Admin can create an issue with the required MVP fields.
FR12: An issue can reference customer or site context using fields defined for MVP (without granting access to end customers).
FR13: A Technician or Admin can view a list of issues available to them within their organization.
FR14: A Technician or Admin can open an issue detail view for an issue they are permitted to see.
FR15: A Technician or Admin can update an issue's attributes permitted by MVP (including status and assignment where allowed).
FR16: A Technician or Admin can assign or reassign an issue to an employee (or self) per organization rules.
FR17: A Technician or Admin can resolve or close an issue according to the state model for MVP.
FR18: The system can retain a history of material changes to an issue (e.g. field changes, assignment, status) with actor and timestamp.
FR19: A Technician or Admin can filter and/or sort the issue list using criteria supported in MVP (e.g. status, priority, recency).
FR20: The system can generate notifications for meaningful issue events (e.g. new issue, assignment change, status change) as defined for MVP.
FR21: The system can deliver notifications to eligible employees according to role and subscription rules for MVP.
FR22: An employee can view a notification or activity surface in-app appropriate to MVP (e.g. inbox or feed).
FR23: The system can send push notifications to supported mobile devices where enabled and permitted.
FR24: An Org Admin can invite or create employee user accounts for their organization.
FR25: An Org Admin can deactivate or remove employee access for their organization.
FR26: The system can prevent deactivated users from signing in or using protected capabilities.
FR27: The system can restrict interactive use to employees of an organization; end customers do not receive accounts or client access in v1.
FR28: The system can support core flows without payment or subscription billing features in MVP (per free initial policy).
FR29: The system can support core flows without third-party product integrations in MVP.
```

### NonFunctional Requirements

```
NFR-P1: Interactive actions on mobile (open app, load issue list, open issue detail, save an update) complete within a target responsive window under typical field conditions; exact p95/p99 targets are set before release and validated on reference devices.
NFR-P2: Background operations (e.g. notification registration, sync after reconnect) do not block the UI from showing clear loading or saved/error states.
NFR-R1: Core services (auth, issue read/write, notification dispatch) meet a defined uptime target for production (e.g. monthly availability); exact SLA is chosen for launch tier.
NFR-R2: Push notification delivery achieves a high success rate under normal operating conditions; failure modes (token invalid, disabled permission) are detectable and communicated in UX.
NFR-R3: Data loss of committed issue updates is unacceptable; persistence and durability assumptions are documented in architecture.
NFR-S1: All client–server traffic uses encrypted transport (e.g. TLS).
NFR-S2: Sensitive data at rest (credentials, tokens, issue content) is protected using industry-standard encryption and key handling appropriate to the deployment model.
NFR-S3: Tenant isolation is enforced at the API and data layers; cross-tenant access is prevented by tests and review.
NFR-S4: Authentication and session handling resist common abuse (e.g. credential stuffing mitigations proportionate to risk).
NFR-S5: Administrative actions that affect access or roles are auditable to the extent required by MVP (see FR18 alignment).
NFR-PR1: Processing of personal data in issues and accounts follows the privacy posture in Domain-Specific Requirements (including region and retention decisions documented for production).
NFR-SC1: Architecture supports multiple organizations and tens to hundreds of active users in early production without redesign; load assumptions are documented.
NFR-SC2: Seasonal or peak-day usage (e.g. high issue volume) degrades gracefully (queue latency, not silent loss).
NFR-A1: Mobile UI follows baseline accessibility targets appropriate for internal B2B use (e.g. text scaling, contrast, touch targets); formal WCAG tiering is explicit if a customer requires it.
NFR-I1: MVP does not depend on external product integrations; non-functional expectations for future integrations are out of scope for v1.
```

### Additional Requirements

```
- Starter template (impacts first implementation stories): Expo — `npx create-expo-app@latest` with `blank-typescript` template; Spring Boot API via Initializr with web, data-jpa, postgresql, security, validation, liquibase; `spring.jpa.hibernate.ddl-auto=none` or `validate` — schema owned by Liquibase only.
- Monorepo layout: `apps/api` (Spring Boot), `apps/mobile` (Expo), `.github/workflows/`, `docs/`, root `.gitignore`, `.editorconfig`, `.env.example`.
- Multi-tenancy: shared schema with `organization_id` on tenant-owned rows; every protected read/write path scoped by org; cross-tenant denial tests (NFR-S3).
- Auth: self-hosted Spring Security; JWT access + refresh tokens; refresh storage, rotation, revocation aligned with FR26; tokens must not appear in logs.
- API: REST + JSON under `/api/v1/...`; OpenAPI as contract; RFC 7807 Problem Details for errors with stable `code` for clients; camelCase JSON; ISO-8601 UTC timestamps with `Z`.
- Database: PostgreSQL; plural `snake_case` tables; Liquibase changelogs under `db/changelog/`; UUID primary keys for entities exposed to clients.
- Domain events (past tense, dot-separated): e.g. `issue.created`, `issue.assigned`, `issue.status_changed`; payload baseline includes organizationId, issueId, actorUserId, occurredAt.
- Mobile: Expo Router; TanStack Query for server state; React Hook Form + Zod; expo-notifications for push; device tokens stored per user with org scope on API.
- Push: FCM/APNs via expo-notifications; in-app notification list via REST refresh for MVP (no WebSockets required for MVP).
- Observability: structured JSON logs; correlation ID per request; Spring Boot Actuator health/readiness where enabled; no secrets/PII in logs.
- CI/CD: GitHub Actions for API build/test and mobile lint/typecheck; EAS Build for device distribution; managed PostgreSQL + API on primary cloud.
- Gaps to close in implementation: RBAC matrix (admin vs technician issue visibility); refresh token table shape and rotation; notification event taxonomy and mapping to FR20.
```

### UX Design Requirements

```
UX-DR1: Root layout wraps the app with React Native Paper `Provider` alongside TanStack Query client and auth context; single MD3 theme object (primary, surfaces, semantic issue status colors, typography scales with outdoor-friendly minimums).
UX-DR2: Implement Direction A as default Issues home: list-first, segmented filters or tabs for queue scope (Open / All / Mine per RBAC), FAB "New issue", status chips on rows.
UX-DR3: Build custom IssueRow: issue id/title, site/customer line, status + priority chips, assignee initials/name, relative time; min row height ~56–72 dp; tap navigates to detail.
UX-DR4: Build IssueStatusChip and PriorityBadge using theme tokens — never color-only state; pair color with icon or label for accessibility.
UX-DR5: Build AssigneePicker (modal or bottom sheet) with searchable org member list (Paper List + Searchbar); loading, empty, and Problem Details error states.
UX-DR6: Build MutationFeedback (inline or banner) for TanStack Query mutations: idle, pending, success (brief), error with Retry — no silent failure; no optimistic "saved" before server ack.
UX-DR7: Build NotificationRow for in-app activity list: issue ref, event type, time, read/unread.
UX-DR8: Build EmptyState variants: first-run vs "filters hid everything" with clear recovery action.
UX-DR9: Map API errors to copy using Problem Details `title`/`detail`/`code`; Snackbar or inline error + Retry; avoid generic alerts only.
UX-DR10: Issues list: pull-to-refresh optional; must not hide TanStack Query error states; loading uses skeleton or ActivityIndicator with label.
UX-DR11: Navigation: Expo Router groups for (auth) vs (app); tabs for Issues vs Notifications vs Settings per IA; deep link from push to issue detail with invalid-id fallback.
UX-DR12: Button hierarchy: one primary per surface; FAB = primary create on Issues home; destructive actions use confirm Dialog.
UX-DR13: Forms: React Hook Form + Zod aligned with OpenAPI; required fields marked; validation on blur/submit per UX spec.
UX-DR14: Accessibility: touch targets ≥ 44×44 pt; OS font scaling supported; `accessibilityLabel` on icon-only controls; VoiceOver/TalkBack on core flows (create, list, detail, error+retry).
UX-DR15: Contrast: WCAG 2.1 AA minimum for body text and controls on surfaces; validate primary button, status chips, error text on light backgrounds (dark mode deferred unless pilot requires).
UX-DR16: Push: permission prompt and token registration flows consolidated (e.g. `lib/notifications.ts`); Banner when push disabled or token invalid per NFR-R2.
UX-DR17: Admin flows: Settings area for invite user, assign role Admin/Technician, deactivate; pending invite state visible; session blocked on next request after deactivate (FR26).
UX-DR18: Issue detail: sticky title/id when scrolling long history; bottom sheet optional for assign/status to reduce stack depth.
```

### FR Coverage Map

FR1: Epic 1 — Tenant isolation (org-scoped data, API and DB enforcement)

FR2: Epic 1 — Org profile create/update (Admin)

FR3: Epic 1 — One organization per user (membership model)

FR4: Epic 1 — Employee sign-in

FR5: Epic 1 — Sign-out

FR6: Epic 1 — Deny unauthenticated access to protected capabilities

FR7: Epic 2 — Admin assigns roles (Admin / Technician minimum)

FR8: Epic 2 — Enforce RBAC for issue and admin actions

FR9: Epic 3 — Technician issue actions within policy

FR10: Epic 2 — Admin user-management and org actions

FR11: Epic 3 — Create issue with required MVP fields

FR12: Epic 3 — Customer/site context on issue (employee-only)

FR13: Epic 3 — Issue list within organization per visibility rules

FR14: Epic 3 — Issue detail for permitted issues

FR15: Epic 3 — Update issue attributes (status, assignment, etc.)

FR16: Epic 3 — Assign / reassign issue

FR17: Epic 3 — Resolve / close issue (state model)

FR18: Epic 3 — Issue change history (actor, timestamp); aligns with audit expectations for admin actions (NFR-S5)

FR19: Epic 3 — Filter/sort issue list

FR20: Epic 4 — Generate notifications for meaningful issue events (taxonomy per MVP)

FR21: Epic 4 — Deliver notifications to eligible employees by role/rules

FR22: Epic 4 — In-app notification / activity surface

FR23: Epic 4 — Push to mobile devices when enabled

FR24: Epic 2 — Invite or create employee accounts

FR25: Epic 2 — Deactivate employee access

FR26: Epic 2 — Block deactivated users from sign-in and protected use

FR27: Epic 2 — Employees only; no end-customer accounts (product + UX enforcement)

FR28: Epic 1 — Core flows without payment/billing in MVP

FR29: Epic 1 — Core flows without third-party integrations in MVP

### Post-MVP quality themes (Epic 5)

Epic 5 does not introduce new PRD functional requirements; it **raises the quality bar** on existing capability. Map themes to stories as follows (illustrative; see Epic 5 stories for authoritative ACs):

| Theme | Primary NFR / basis | Epic 5 stories |
|-------|---------------------|----------------|
| Automated regression & confidence | NFR-R3, NFR-S3 (verification), NFR-P1/P2 (non-regression) | 5.1, 5.2, 5.3, 5.6 |
| Maintainable codebase | NFR-SC1 (evolvability), observability / clarity from Architecture | 5.4 |
| Contemporary UX | NFR-A1, UX-DR1 (theme and surfaces) | 5.5 |

## Epic List

### Epic 1: Bootstrap, tenancy & authentication

**Goal:** Deliver a runnable **monorepo** with **API + PostgreSQL + Liquibase** and **Expo mobile** shell so **employees can sign in and sign out**, and **all data access is organization-scoped** with **no cross-tenant leakage**. Establish **TLS**, **JWT access/refresh** baseline, **OpenAPI** and **Problem Details** patterns, and **CI skeleton** per architecture — without billing or external integrations (FR28–FR29).

**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR28, FR29

**Primary NFR / UX touchpoints:** NFR-S1, NFR-S2 (baseline), NFR-S3 (enforcement starts), NFR-P2 (honest loading states on auth flows), starter-template and repo layout from Architecture; UX-DR9–DR11 (auth screens, navigation shell).

---

### Epic 2: Organization admin, roles & access control

**Goal:** **Org admins** can **invite or create employees**, **assign Admin vs Technician**, and **deactivate** users; the system **enforces RBAC** and **blocks deactivated accounts**. The product remains **employee-only** (no client accounts) (FR27). Administrative actions needed for auditability are traceable (supports NFR-S5 with FR18 for security-relevant events).

**FRs covered:** FR7, FR8, FR9, FR10, FR24, FR25, FR26, FR27

**Primary NFR / UX touchpoints:** NFR-S4, NFR-S5; UX-DR17 (admin flows), UX-DR14 (accessible admin forms).

---

### Epic 3: Issues — capture, triage, ownership & history

**Goal:** **Technicians and admins** can **create, view, filter/sort, update, assign, resolve, and close** issues with **customer/site context** and **auditable history** of material changes — the **core system of record** for mower service coordination.

**FRs covered:** FR11, FR12, FR13, FR14, FR15, FR16, FR17, FR18, FR19

**Primary NFR / UX touchpoints:** NFR-P1, NFR-P2, NFR-R3, NFR-SC1–SC2, NFR-A1, NFR-PR1 (data in issues); UX-DR1–DR6, UX-DR8–DR10, UX-DR12–DR13, UX-DR15–DR16 (list/detail/create), Direction A.

---

### Epic 4: Awareness — in-app notifications & push

**Goal:** **Meaningful issue events** generate **notifications**, delivered **in-app** and via **push** to **eligible employees** per role/rules — so teams gain **near–real-time awareness** without replacing issue state as the source of truth.

**FRs covered:** FR20, FR21, FR22, FR23

**Primary NFR / UX touchpoints:** NFR-R1, NFR-R2 (push reliability UX), domain event naming from Architecture; UX-DR7, UX-DR11 (notification list / tabs), UX-DR16 (push permission and degraded modes).

---

### Epic 5: Post-MVP quality, maintainability & UX polish

**Goal:** After MVP delivery (Epics 1–4), **deepen test coverage**, **align backend code with domain boundaries**, **modernize the mobile UI** toward current patterns, and optionally **tighten CI and documentation** — so the product stays **safe to change**, **pleasant to use**, and **cheap to extend**. This epic is **open-ended**: you may **add more stories** to `epics.md` and regenerate `sprint-status.yaml` without renumbering completed MVP work.

**FRs covered:** None new (MVP FRs remain satisfied by Epics 1–4). **Quality / NFR alignment:** NFR-S3, NFR-R3, NFR-P1/P2, NFR-A1, NFR-SC1; UX-DR1 and related presentation standards where applicable.

**Primary NFR / UX touchpoints:** Test automation and integration tests (API and mobile), E2E where feasible, package clarity for reviewers, refreshed MD3 / 2026-adjacent mobile styling without breaking accessibility baselines from Epic 3.

**Suggested additional topics** (add as future stories when prioritized): dependency and security update cadence (Dependabot, audit pipeline), performance baselines / profiling on reference devices, expanded accessibility audit beyond MVP baseline, OpenAPI contract tests, load or soak tests for peak issue volume (NFR-SC2).

---

**Cross-cutting:** NFRs (performance targets, uptime SLA numerics, scalability testing) are verified across epics via acceptance criteria and non-functional checks where applicable. **RBAC matrix**, **notification taxonomy**, and **refresh token** details are specified in stories within Epic 2–4 as appropriate. **Epic 5** strengthens ongoing verification and maintainability **after** MVP scope is met.

## Epic 1: Bootstrap, tenancy & authentication — stories

### Story 1.1: Initialize monorepo from Expo and Spring Boot starters

As a **developer**,
I want **the repository scaffolded from the approved starters with baseline tooling**,
So that **all later work shares one layout, build, and migration discipline**.

**Implements:** Architecture starter template; FR28, FR29 (no billing, no third-party product integrations in scaffold).

**Acceptance Criteria:**

**Given** a clean repo root  
**When** the initializer runs per architecture (`create-expo-app` blank TypeScript under `apps/mobile`; Spring Boot with `web`, `data-jpa`, `postgresql`, `security`, `validation`, `liquibase` under `apps/api`)  
**Then** both apps build successfully locally and README documents how to run API and mobile dev servers  
**And** root contains `.gitignore`, `.editorconfig`, `.env.example` (names only), and `.github/workflows/` runs API tests/build and mobile lint/typecheck on PR

**Given** the API project  
**When** Liquibase is configured with `ddl-auto` none or validate and a minimal master changelog  
**Then** the application starts against PostgreSQL with migrations applied and no Hibernate-owned schema drift

---

### Story 1.2: Organizations and user membership data model

As a **system**,
I want **organizations and users stored with strict org scoping**,
So that **tenant isolation can be enforced on every data path (FR1, FR3)**.

**Implements:** FR1, FR3; NFR-S3 (foundation); Additional: shared-schema `organization_id`, UUID IDs.

**Acceptance Criteria:**

**Given** Liquibase changesets  
**When** `organizations` and `users` tables exist with `users.organization_id` → `organizations`, unique constraint enforcing **one organization per user**  
**Then** integration tests with Testcontainers prove no user can be linked to two orgs

**Given** repository queries for tenant-owned data  
**When** services load users by id  
**Then** `organization_id` is always present for authorization checks in later stories

---

### Story 1.3: Bootstrap first organization and admin user

As an **implementer / first operator**,
I want **a controlled way to create the first organization and admin credentials**,
So that **the system can be deployed without a public signup that violates employee-only scope (FR27 foundation)**.

**Implements:** FR2 (enables org record); FR27 direction (employee-only bootstrap, not customer signup).

**Acceptance Criteria:**

**Given** a secured bootstrap path (documented env flag, one-time token, or seed script — chosen in implementation)  
**When** bootstrap runs  
**Then** exactly one `Organization` and one `User` with role **Admin** exist and credentials are not committed to git

**Given** subsequent bootstrap attempts  
**When** an organization already exists  
**Then** bootstrap is rejected with a clear error (Problem Details)

---

### Story 1.4: Authentication API — login, refresh, logout

As an **employee**,
I want **to obtain and refresh a session via JWT access and refresh tokens**,
So that **I can use the API securely from the mobile app (FR4–FR6)**.

**Implements:** FR4, FR5, FR6; NFR-S1, NFR-S4 (baseline); Additional: Problem Details, OpenAPI, refresh storage.

**Acceptance Criteria:**

**Given** valid user credentials  
**When** `POST` login is called  
**Then** response returns access + refresh tokens and OpenAPI describes the contract; HTTP is TLS in deployed environments

**Given** a valid refresh token  
**When** `POST` refresh is called  
**Then** a new access token is issued and refresh rotation policy is documented in code

**Given** logout  
**When** `POST` logout is called  
**Then** refresh token(s) for that session are revoked or invalidated per documented policy

**Given** invalid credentials or revoked refresh  
**When** auth endpoints are used  
**Then** API returns `application/problem+json` with stable `code` and no token bodies in logs

---

### Story 1.5: Global API security and tenant context on protected routes

As a **system**,
I want **protected `/api/v1/**` routes to require a valid JWT and validated org membership**,
So that **unauthenticated access is denied (FR6) and cross-tenant path tricks fail (FR1, NFR-S3)**.

**Implements:** FR6, FR1; NFR-S3.

**Acceptance Criteria:**

**Given** a request without `Authorization`  
**When** a protected endpoint is called  
**Then** the response is `401` with Problem Details

**Given** a JWT whose org does not match the resource `organizationId`  
**When** the request is processed  
**Then** access is denied with `403` or `404` per documented policy and tests cover cross-tenant denial

---

### Story 1.6: Mobile app shell — Paper, Query, Router, auth placeholder

As an **employee**,
I want **a mobile shell with design system, server-state client, and route groups**,
So that **screens can be added consistently (UX-DR1, UX-DR11)**.

**Implements:** UX-DR1 (Paper `Provider`, TanStack Query); UX-DR11 (Expo Router `(auth)` / `(app)` groups); NFR-P2 baseline (loading states can be shown).

**Acceptance Criteria:**

**Given** the app starts  
**When** root layout renders  
**Then** React Native Paper `Provider` wraps the tree and TanStack Query `QueryClientProvider` is configured with retry defaults suited to flaky networks

**Given** routing  
**When** unauthenticated  
**Then** user sees auth routes only; when authenticated, `(app)` routes are reachable

**Given** configuration  
**When** API base URL is read from Expo config / constants  
**Then** no secrets are stored in the repo

---

### Story 1.7: Mobile sign-in and sign-out

As an **employee**,
I want **to sign in and sign out on my phone**,
So that **I can access MowerCare safely (FR4, FR5)**.

**Implements:** FR4, FR5; UX-DR6, UX-DR9, UX-DR12 (button hierarchy on auth); UX-DR14 (labels on controls).

**Acceptance Criteria:**

**Given** sign-in form  
**When** user submits valid credentials  
**Then** tokens are stored using secure storage for refresh and access handling per architecture; MutationFeedback shows pending → success or error with Retry on transient failure

**Given** signed-in session  
**When** user chooses sign out  
**Then** tokens are cleared client-side and logout API is called if required; user returns to auth flow

**Given** API errors  
**When** Problem Details are returned  
**Then** user-visible copy reflects `title`/`detail`/`code` where applicable, not generic “Error” only

---

### Story 1.8: Organization profile read/update for Admin

As an **Org Admin**,
I want **to view and edit my organization’s profile fields supported in MVP**,
So that **the team name is correct in the product (FR2)**.

**Implements:** FR2; FR8 partial (Admin-only rule documented here; full matrix in Epic 2).

**Acceptance Criteria:**

**Given** an authenticated **Admin** user  
**When** they `GET/PATCH` organization profile per OpenAPI  
**Then** supported fields (at minimum name) persist and responses use camelCase JSON

**Given** a **Technician**  
**When** they attempt profile update  
**Then** the API returns `403` Problem Details

---

## Epic 2: Organization admin, roles & access control — stories

### Story 2.1: Role model and RBAC enforcement hooks

As a **system**,
I want **Admin vs Technician permissions defined and enforced consistently on API and documented for mobile**,
So that **FR8–FR10 and FR9 are implementable without ambiguity**.

**Implements:** FR8, FR9, FR10 (baseline); closes architecture gap “RBAC matrix”.

**Acceptance Criteria:**

**Given** a documented permission matrix (e.g. table in repo or OpenAPI description)  
**When** issue and admin endpoints are implemented or stubbed  
**Then** Spring Security annotations and/or explicit checks enforce Admin vs Technician for each operation

**Given** unit/integration tests  
**When** a Technician calls an Admin-only endpoint  
**Then** `403` Problem Details; when Admin calls Technician-allowed issue endpoints, behavior matches matrix

---

### Story 2.2: Invite or create employee user

As an **Org Admin**,
I want **to invite or create employee accounts for my organization**,
So that **my team can sign in without customer-facing signup (FR24, FR27)**.

**Implements:** FR24; FR27; NFR-PR1 (minimal PII in invites — document fields).

**Acceptance Criteria:**

**Given** Admin session  
**When** Admin creates/invites a user with email and role Technician or Admin  
**Then** user record is created in org scope; invite flow state (pending/accepted) is visible in API

**Given** the invitation or account creation  
**When** completed  
**Then** no end-customer portal exists and copy remains employee-only

---

### Story 2.3: Assign roles to employees

As an **Org Admin**,
I want **to assign each employee a role (Admin or Technician)**,
So that **permissions match responsibilities (FR7)**.

**Implements:** FR7.

**Acceptance Criteria:**

**Given** Admin  
**When** they update a user’s role  
**Then** role persists and subsequent JWTs or server-side checks reflect the change within defined session policy

**Given** last Admin demotion is attempted  
**When** validation runs  
**Then** request fails with clear Problem Details (org must retain at least one Admin — documented rule)

---

### Story 2.4: Deactivate employee and block access

As an **Org Admin**,
I want **to deactivate an employee**,
So that **they can no longer access org data (FR25, FR26)**.

**Implements:** FR25, FR26; NFR-S5 (audit event for deactivation).

**Acceptance Criteria:**

**Given** Admin deactivates a user  
**When** the change commits  
**Then** user is marked inactive; refresh tokens are revoked; next API calls return `401`/`403` as documented

**Given** audit requirements  
**When** deactivation occurs  
**Then** an auditable record exists (who deactivated whom, when)

---

### Story 2.5: Admin settings UI (invites, roles, deactivate)

As an **Org Admin**,
I want **settings screens to manage users**,
So that **I can run the shop without developer help (UX-DR17)**.

**Implements:** UX-DR17; FR24–FR27 (UI side); UX-DR12–DR14 on admin flows.

**Acceptance Criteria:**

**Given** Admin navigates to Settings  
**When** they invite, change role, or deactivate  
**Then** flows use Paper components, confirm destructive actions with Dialog, and show MutationFeedback

**Given** Technician  
**When** they open Settings admin sections  
**Then** UI hides or disables with explanation per RBAC

---

### Story 2.6: Employee-only access guardrails

As a **product**,
I want **the client and API to reject non-employee use cases for interactive access**,
So that **v1 remains employee-only (FR27)**.

**Implements:** FR27.

**Acceptance Criteria:**

**Given** product documentation and API error messages  
**When** a flow might resemble “customer access”  
**Then** there is no customer registration or portal; acceptance tests assert public customer routes are absent

---

## Epic 3: Issues — capture, triage, ownership & history — stories

### Story 3.1: Issue aggregate schema and change-history storage

As a **system**,
I want **issues and material change history persisted in PostgreSQL with Liquibase**,
So that **FR11–FR18 have a durable foundation without creating unrelated tables in advance**.

**Implements:** FR18 (storage); NFR-R3; Additional: issue tables only — no notification tables until Epic 4.

**Acceptance Criteria:**

**Given** new changesets  
**When** migrations apply  
**Then** `issues` includes `organization_id`, UUID keys, timestamps; history table(s) capture actor and timestamp for material changes

**Given** JPA entities  
**When** issue is updated  
**Then** service layer writes history rows for defined material fields (status, assignment, priority, etc.)

---

### Story 3.2: Create issue (API + mobile)

As a **Technician or Admin**,
I want **to create an issue with required MVP fields and customer/site context**,
So that **work is captured in the field (FR11, FR12)**.

**Implements:** FR11, FR12; UX-DR2 (FAB entry), UX-DR13 (RHF+Zod), UX-DR6, UX-DR9.

**Acceptance Criteria:**

**Given** authenticated user with create permission  
**When** they submit create with required fields  
**Then** issue is persisted org-scoped; Problem Details on validation errors; mobile shows honest pending/success/failure

**Given** OpenAPI  
**When** contract is generated or hand-written  
**Then** Zod schemas align with create payload

---

### Story 3.3: Issue list — Direction A (list + filters + row)

As a **Technician or Admin**,
I want **a scannable issue queue with scope filters and informative rows**,
So that **I can triage quickly (FR13, FR19 partial; UX-DR2–DR4, DR8, DR10)**.

**Implements:** FR13; UX-DR2, UX-DR3, UX-DR4, UX-DR8, UX-DR10; NFR-P1 baseline.

**Acceptance Criteria:**

**Given** issues exist  
**When** user opens Issues home  
**Then** Direction A layout applies: list-first, tabs or segmented **Open / All / Mine** per RBAC, FAB **New issue**, rows show title/id, site/customer line, status and priority chips, assignee, relative time

**Given** empty or filtered-empty  
**When** list renders  
**Then** EmptyState distinguishes “no issues” vs “no matches” with recovery action

**Given** pull-to-refresh if implemented  
**When** errors occur  
**Then** error state remains visible per TanStack Query (not swallowed)

---

### Story 3.4: Issue detail view

As a **Technician or Admin**,
I want **to open an issue and see full context**,
So that **I can act on the right job (FR14)**.

**Implements:** FR14; UX-DR18 (sticky title when scrolling long content).

**Acceptance Criteria:**

**Given** user has visibility  
**When** they open detail  
**Then** fields match API; long content scrolls with sticky header context

**Given** user lacks visibility  
**When** they deep link or guess UUID  
**Then** API returns `403`/`404` per policy with Problem Details

---

### Story 3.5: Update issue fields, assignment, status, resolve/close

As a **Technician or Admin**,
I want **to update issue attributes including status and assignment**,
So that **ownership and state stay current (FR15–FR17)**.

**Implements:** FR15, FR16, FR17; UX-DR5 (assign UI can defer to Story 3.6 if split), UX-DR6, UX-DR12.

**Acceptance Criteria:**

**Given** permission to edit  
**When** user changes status, assignment, or permitted fields  
**Then** server validates transitions; history records material changes; MutationFeedback covers mutation lifecycle

**Given** resolve/close  
**When** user completes workflow  
**Then** state model for MVP is enforced and visible on detail

---

### Story 3.6: AssigneePicker component and integration

As a **Technician or Admin**,
I want **a searchable picker to assign or reassign issues to team members**,
So that **handoffs are explicit and fast (FR16; UX-DR5)**.

**Implements:** FR16; UX-DR5.

**Acceptance Criteria:**

**Given** assign action  
**When** picker opens  
**Then** org member list loads with search; loading/empty/error states follow Problem Details

**Given** selection  
**When** user confirms  
**Then** assignment persists and list/detail reflect new assignee

---

### Story 3.7: Issue activity / history on detail

As a **Technician or Admin**,
I want **to see who changed what and when**,
So that **we maintain trust in the shared system of record (FR18)**.

**Implements:** FR18; supports NFR-S5 for issue-relevant audit presentation.

**Acceptance Criteria:**

**Given** prior changes  
**When** user views detail  
**Then** timeline shows material changes with actor and timestamp in ISO-8601 UTC in UI

**Given** API  
**When** history is requested  
**Then** responses are org-scoped and paginated if needed

---

### Story 3.8: Filter and sort issue list (MVP criteria)

As a **Technician or Admin**,
I want **to filter and sort by MVP criteria such as status, priority, recency**,
So that **I can find the right work quickly (FR19)**.

**Implements:** FR19.

**Acceptance Criteria:**

**Given** list endpoint  
**When** query params are used per OpenAPI (`status`, `sort`, etc.)  
**Then** results match contract; mobile UI exposes the supported filters without hidden broken options

**Given** RBAC  
**When** filters imply “all org issues”  
**Then** behavior matches matrix for Technician vs Admin

---

### Story 3.9: Mobile accessibility and contrast baseline for issue flows

As an **employee**,
I want **issue flows to meet baseline accessibility on phones**,
So that **we meet internal B2B bar (NFR-A1; UX-DR14, UX-DR15)**.

**Implements:** NFR-A1; UX-DR14, UX-DR15 (focused on Issues screens).

**Acceptance Criteria:**

**Given** core issue flows (list, create, detail, error+retry)  
**When** tested with large font and VoiceOver/TalkBack spot checks  
**Then** touch targets ≥ 44pt, critical text/controls meet contrast intent for WCAG 2.1 AA on light theme; icon-only controls have labels

---

## Epic 4: Awareness — in-app notifications & push — stories

### Story 4.1: Notification records and issue event taxonomy

As a **system**,
I want **domain events from issues to map to a defined notification taxonomy**,
So that **only meaningful events generate notifications (FR20)**.

**Implements:** FR20; Additional: `issue.created`, `issue.assigned`, `issue.status_changed` alignment.

**Acceptance Criteria:**

**Given** issue mutations from Epic 3  
**When** material events occur  
**Then** notification records (or outbox rows) are created with organizationId, issueId, actorUserId, occurredAt

**Given** taxonomy documentation  
**When** product reviews  
**Then** event list matches MVP scope (no noise from every keystroke)

---

### Story 4.2: Notification delivery rules by role

As a **system**,
I want **to deliver notifications to eligible employees per role and routing rules**,
So that **the right people are informed (FR21)**.

**Implements:** FR21.

**Acceptance Criteria:**

**Given** an event  
**When** fan-out runs  
**Then** rules define recipients (e.g. assignee, admins, watchers — as per locked matrix) and are covered by tests

**Given** a user should not receive an event  
**When** rules evaluate  
**Then** no spurious notification row is created for that user

---

### Story 4.3: In-app notification list (REST) and UI

As an **employee**,
I want **an in-app feed of notifications**,
So that **I can see activity even when push is off (FR22; UX-DR7)**.

**Implements:** FR22; UX-DR7; UX-DR11 (Notifications tab).

**Acceptance Criteria:**

**Given** authenticated user  
**When** they open Notifications  
**Then** list shows issue ref, event type, time, read/unread; TanStack Query handles refresh

**Given** empty feed  
**When** no notifications  
**Then** EmptyState explains the situation

---

### Story 4.4: Device registration and push delivery

As an **employee**,
I want **push notifications on my device when enabled**,
So that **I get timely awareness in the field (FR23; NFR-R2; UX-DR16)**.

**Implements:** FR23; NFR-R2; UX-DR16; Additional: FCM/APNs via expo-notifications, token storage API.

**Acceptance Criteria:**

**Given** user grants permission  
**When** app registers device token with API  
**Then** token is stored per user with org scope; invalid tokens are handled without crashing

**Given** notification dispatch  
**When** push sends  
**Then** failures are logged per observability rules; user sees Banner or settings hint if push disabled

---

### Story 4.5: Deep link from push to issue detail

As an **employee**,
I want **tapping a push to open the relevant issue**,
So that **I can respond quickly (UX-DR11)**.

**Implements:** UX-DR11 (deep link); integrates FR22/FR23.

**Acceptance Criteria:**

**Given** push payload with issue id  
**When** user taps notification  
**Then** app navigates to issue detail when permitted

**Given** invalid or foreign-org issue id  
**When** opened  
**Then** user sees error/empty state with path back to list

---

## Epic 5: Post-MVP quality, maintainability & UX polish — stories

### Story 5.1: Backend unit and integration test expansion

As a **developer**,
I want **broader JUnit coverage and integration tests (e.g. Testcontainers) for critical API and domain paths**,
So that **refactors and new work do not regress tenant isolation, auth, issues, or notifications (NFR-S3, NFR-R3)**.

**Implements:** NFR-S3 (verification); NFR-R3; Additional: Spring Boot testing idioms, no secrets in test logs.

**Acceptance Criteria:**

**Given** the API module  
**When** unit tests run (`./mvnw test` or project-standard command)  
**Then** critical services and security paths (org scope, RBAC, issue mutations, notification dispatch hooks) have **meaningful** unit coverage; gaps are documented in code comments or a short `docs/` note if intentionally deferred

**Given** integration tests with PostgreSQL (Testcontainers or CI-provided DB)  
**When** the integration suite runs  
**Then** representative flows prove **cross-tenant denial**, auth on protected routes, and at least one **end-to-end API path** each for issues and notifications per existing OpenAPI contracts

**Given** CI  
**When** a pull request is opened  
**Then** backend test job remains green and failures are actionable (no flaky sleeps without justification)

---

### Story 5.2: Frontend unit test expansion

As a **developer**,
I want **more unit and component tests on the mobile app (hooks, navigation helpers, form validation, critical UI)**,
So that **UI refactors stay safe and regressions are caught early (NFR-P2 honest states)**.

**Implements:** NFR-P2 (predictable UI behavior); UX-DR6, UX-DR9 (error and mutation feedback patterns tested where applicable).

**Acceptance Criteria:**

**Given** the mobile package (e.g. `apps/mobile`)  
**When** `npm test` / `yarn test` (or project-standard) runs in CI  
**Then** tests exist for **high-value** units: auth/session helpers, issue list/detail behaviors that are pure or easily mocked, notification registration/deep-link parsing utilities, and Zod/schema validation aligned with OpenAPI

**Given** new tests  
**When** they run locally and in CI  
**Then** they do not require manual device interaction; mocks for TanStack Query / Expo modules are stable and documented in one place if shared

**Given** coverage  
**When** reviewed  
**Then** the team agrees the **next risk hotspots** are either covered or listed as follow-ups (optional: coverage report artifact in CI)

---

### Story 5.3: End-to-end tests for critical flows

As a **team**,
I want **automated E2E coverage of the main user journeys across API + mobile where feasible**,
So that **releases catch broken flows that unit tests miss (NFR-P1 baseline)**.

**Implements:** NFR-P1 (smoke-level confidence); cross-cutting validation of FR4–FR6, FR11–FR14, FR22–FR23 on a **smoke** subset.

**Acceptance Criteria:**

**Given** the monorepo  
**When** E2E strategy is chosen and documented (e.g. Detox, Maestro, Appium, or API-only smoke plus mobile UI driver — **one** primary approach)  
**Then** `docs/` or README describes how to run E2E locally and in CI, including required env (API URL, test org bootstrap or fixtures)

**Given** the chosen toolchain  
**When** the E2E suite runs  
**Then** at minimum **one** path covers: sign-in → issues list → open issue detail; and **one** path covers notification list or push-related surface **if** runnable without Apple/Google secrets in CI (otherwise document manual/device lab gap)

**Given** CI constraints  
**When** full device E2E is not available in GitHub Actions  
**Then** the story documents the fallback (e.g. scheduled job, local-only target, or API contract smoke) and the team accepts the trade-off explicitly

---

### Story 5.4: Backend domain-aligned package structure

As a **developer**,
I want **packages under existing roots (e.g. `model`, `service`, `web`) grouped by domain such as `issue`, `notification`, `organization`**,
So that **navigation and reviews stay easy as the codebase grows (NFR-SC1)**.

**Implements:** NFR-SC1; Additional: Architecture package conventions — **move-only** refactor with no behavior change unless fixing a discovered bug (out of scope unless trivial).

**Acceptance Criteria:**

**Given** `apps/api` source  
**When** packages are reorganized by domain  
**Then** **no intentional API or JSON contract changes**; OpenAPI and mobile clients work unchanged

**Given** moved types  
**When** the application builds and all tests pass  
**Then** import cycles are avoided or documented; each domain folder has a clear responsibility (entities, services, controllers as already structured by Spring conventions)

**Given** Liquibase and runtime  
**When** migrations and app start are exercised  
**Then** schema and runtime behavior match pre-refactor baseline (integration or smoke test proves startup)

---

### Story 5.5: Mobile UI modernization (2026-ready patterns)

As an **employee**,
I want **a fresher, more contemporary visual and interaction baseline (spacing, typography, surfaces, motion where appropriate)**,
So that **the app feels current and professional without sacrificing usability (NFR-A1)**.

**Implements:** NFR-A1; UX-DR1 (single MD3 theme object — **evolve** tokens, not duplicate themes); UX-DR14–DR15 baselines **maintained or improved**.

**Acceptance Criteria:**

**Given** the app shell and core screens (Issues, Notifications, Settings, auth)  
**When** theme tokens are updated (colors, elevation, shape, motion)  
**Then** **one** canonical Paper/MD3 theme remains the source of truth; dark mode remains **optional** unless explicitly in scope for this story

**Given** primary flows  
**When** reviewed on a reference device  
**Then** touch targets and contrast **do not regress** below Epic 3 Story 3.9 baselines; icon-only controls retain `accessibilityLabel`

**Given** rollout risk  
**When** changes are large  
**Then** introduce a **feature flag or phased rollout** is documented OR changes are split into follow-up stories in Epic 5

---

### Story 5.6: CI quality signals and developer test documentation

As a **developer**,
I want **clear CI jobs and documentation for how tests map to quality (and optional non-blocking coverage reporting)**,
So that **contributors know what to run before PRs and CI failures are interpretable**.

**Implements:** NFR-S3 (ongoing verification); Additional: repo hygiene.

**Acceptance Criteria:**

**Given** GitHub Actions (or project CI)  
**When** a PR runs  
**Then** backend unit/integration, mobile unit, and lint/typecheck jobs are **named consistently** and link to logs that identify the failing step

**Given** documentation  
**When** a new contributor opens README or `docs/testing.md`  
**Then** they find commands for unit, integration, and E2E (if any), plus prerequisites (Java, Node, Docker for Testcontainers)

**Given** optional coverage thresholds  
**When** enabled  
**Then** thresholds are **incremental** (not blocking the first merge of Epic 5) or reported as informational only — document the policy

---

### UX Design Requirements coverage (verify in development)

| UX-DR | Where addressed |
|-------|-----------------|
| UX-DR1 | Story 1.6 |
| UX-DR2–DR4, DR8, DR10 | Story 3.3 |
| UX-DR5 | Story 3.6 |
| UX-DR6 | Stories 1.7, 3.2, 3.5 |
| UX-DR7 | Story 4.3 |
| UX-DR9 | Stories 1.7, 3.2 |
| UX-DR11 | Stories 1.6, 4.3, 4.5 |
| UX-DR12 | Stories 1.7, 3.5 |
| UX-DR13 | Story 3.2 |
| UX-DR14 | Stories 1.7, 2.5, 3.9 |
| UX-DR15 | Story 3.9 |
| UX-DR16 | Story 4.4 |
| UX-DR17 | Story 2.5 |
| UX-DR18 | Story 3.4 |

**Epic 5 (post-MVP):** Stories **5.2** and **5.5** intentionally **revisit** UX-DR1, UX-DR6, UX-DR9, UX-DR14, and UX-DR15 to deepen tests and refresh presentation without lowering the MVP accessibility bar.

## Final validation (Step 4)

**Date:** 2026-03-26

### 1. FR coverage

| FR | Covered in |
|----|------------|
| FR1–FR3 | Stories 1.2 (FR1, FR3), 1.5 (FR1 enforcement), 1.8 (FR2) |
| FR4–FR6 | Stories 1.4, 1.5, 1.7 |
| FR7–FR10 | Stories 2.1–2.3, 2.5 |
| FR11–FR19 | Stories 3.1–3.8 |
| FR20–FR23 | Stories 4.1–4.5 |
| FR24–FR27 | Stories 2.2–2.6 |
| FR28–FR29 | Stories 1.1, 1.4 (scaffold / no billing or external integrations) |

**Result:** All **29** FRs map to at least one story with acceptance criteria.

### 2. Architecture compliance

- **Starter template:** **Pass** — Story **1.1** initializes from **Expo** (`create-expo-app` blank TypeScript) and **Spring Boot** Initializr with required dependencies and Liquibase baseline.
- **Incremental schema:** **Pass** — `organizations` / `users` land in **1.2**; issues/history in **3.1**; notification persistence in **4.x** — not all tables in Story 1.1.

### 3. Story quality and dependencies

- **Sizing:** Stories are scoped to a single dev agent with Given/When/Then ACs.
- **Epic order:** Epic 2 (users/RBAC) before Epic 3 (issues) is required; Epic 4 follows issue event sources — **acceptable** cross-epic ordering.
- **Within-epic order:** Each story builds on earlier numbered stories; **3.5** explicitly allows assign UI detail in **3.6** without forward dependency violation.

### 4. Epic structure

- Epics are **user-outcome** oriented (access, admin, issues, awareness), not “database epic / API epic” splits.

### 5. UX-DR coverage

- Table **UX Design Requirements coverage** maps **UX-DR1–UX-DR18** to stories — **complete**.

**Overall:** **READY FOR DEVELOPMENT** — pending your confirmation below.

### Epic 5 addendum (2026-04-06)

- **Scope:** Epic 5 is **additive post-MVP**; FR1–FR29 coverage above is unchanged.
- **Story order:** Stories **5.1–5.6** are **sequentially independent** — each can ship without waiting for a later 5.x; choose implementation order by risk (e.g. 5.4 before heavy new tests if package moves would otherwise churn tests twice).
- **Open epic:** Additional maintenance stories may be appended after **5.6** with the next available **5.x** number; run **sprint planning** to sync `sprint-status.yaml`.

---

**Workflow menu (Step 4):** All validations complete — reply **[C]** to mark the **Create Epics and Stories** workflow finished and use `epics.md` as the implementation backlog source of truth.
