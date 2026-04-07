# MowerCare — system architecture

This document describes how the **mobile app**, **HTTP API**, and **PostgreSQL** database fit together. For product-level decisions and pattern rules, see also [`_bmad-output/planning-artifacts/architecture.md`](../_bmad-output/planning-artifacts/architecture.md) (internal planning artifact).

## System overview

MowerCare serves **field service organizations** (admins and technicians). Each **organization** is a tenant: data is isolated with `organization_id` on tenant-owned rows. Clients talk to the API over **HTTPS** with **JWT** bearer tokens; the mobile app uses **Expo** and **React Native**.

## System context

```mermaid
flowchart TB
  subgraph actors [People]
    Admin[Org Admin]
    Tech[Technician]
  end
  subgraph mobile [apps/mobile]
    App[Expo React Native app]
  end
  subgraph backend [apps/api]
    API[Spring Boot REST API]
  end
  DB[(PostgreSQL 16)]
  FCM[Firebase Cloud Messaging]
  Admin --> App
  Tech --> App
  App -->|REST JSON /api/v1| API
  API --> DB
  API -.->|optional push| FCM
  FCM -.->|notifications| App
```

## API request flow

A typical authenticated request passes through Spring Security (JWT resource server), then domain authorization (tenant path + role), then controllers → services → JPA repositories.

```mermaid
flowchart LR
  HTTP[HTTP request]
  RS[OAuth2 Resource Server JWT]
  ASF[AccountStatusVerificationFilter]
  C[Controller]
  S[Service]
  R[JpaRepository]
  PG[(PostgreSQL)]
  HTTP --> RS --> ASF --> C --> S --> R --> PG
```

- **JWT** validates the access token and builds authentication.
- **AccountStatusVerificationFilter** rejects requests for **deactivated** users (after JWT auth).
- **TenantPathAuthorization** ensures path `organizationId` matches the JWT `organizationId` claim.
- **RoleAuthorization** enforces Admin vs Technician rules per endpoint.

## Mobile navigation (high level)

```mermaid
flowchart TB
  Root[Root Stack _layout]
  Idx[index.tsx auth gate]
  AuthGroup["(auth) login"]
  AppGroup["(app) signed-in"]
  Tabs["(tabs) Issues | Notifications | Settings"]
  Stack["Stack: org, team, issue detail, create"]
  Root --> Idx
  Idx --> AuthGroup
  Idx --> AppGroup
  AppGroup --> Tabs
  AppGroup --> Stack
```

Routes live under `apps/mobile/app/` (Expo Router). The app uses **TanStack Query** for server state and an **AuthProvider** for session lifecycle.

## Data model (entity relationships)

Schema truth is **Liquibase** (`apps/api/src/main/resources/db/changelog/`). Hibernate does not apply DDL in deployed environments (`ddl-auto: none`).

```mermaid
erDiagram
  organizations ||--o{ users : has
  organizations ||--o{ issues : has
  organizations ||--o{ issue_change_events : has
  organizations ||--o{ notification_events : has
  organizations ||--o{ notification_recipients : has
  organizations ||--o{ device_push_tokens : has
  users ||--o{ refresh_tokens : has
  users ||--o{ issues : assigns
  users ||--o{ issue_change_events : acts
  users ||--o{ notification_events : acts
  users ||--o{ notification_recipients : receives
  users ||--o{ device_push_tokens : registers
  issues ||--o{ issue_change_events : history
  issues ||--o{ notification_events : triggers
  issue_change_events o|--o| notification_events : optional_source
  notification_events ||--o{ notification_recipients : fans_out
```

See [database-schema.md](database-schema.md) for column-level detail.

## Notification pipeline

Issue mutations can emit **notification events**; recipients are materialized per org rules; **FCM** sends push when Firebase is enabled.

```mermaid
flowchart LR
  IS[IssueService]
  NER[NotificationEventRecorder]
  RF[Recipient fan-out]
  NE[notification_events rows]
  NR[notification_recipients rows]
  PD[Push dispatcher]
  IS --> NER --> RF --> NE
  RF --> NR
  NE --> PD
```

In-app delivery is via **GET** notification list APIs; push is optional (`MOWERCARE_FIREBASE_ENABLED`).

## API layer structure

Java packages under `com.mowercare` are **feature-oriented** (e.g. `auth`, `user`, `issue`, `notification`, `organization`, `security`, `common`). HTTP controllers call **services**; services use **repositories** — no DB access from controllers.

Errors use **RFC 7807 Problem Details** (`application/problem+json`) with stable `code` values. See [api-reference.md](api-reference.md).

## Mobile layer structure

- **`app/`** — Expo Router screens and layouts.
- **`components/`** — reusable UI (lists, timelines, pickers).
- **`lib/`** — API client (`api.ts`, `http.ts`), auth (`auth-context.tsx`, `session.ts`, `auth-storage.ts`), domain modules (`issue-api.ts`, …), theme, push helpers.

## Cross-cutting concerns

| Concern | Approach |
|---------|----------|
| **Tenant isolation** | JWT `organizationId` + path checks; integration tests for cross-tenant denial |
| **RBAC** | JWT `role` + explicit checks in controllers/services; [rbac-matrix.md](rbac-matrix.md) |
| **Schema migrations** | Liquibase only — no Hibernate `ddl-auto` updates in prod |
| **API errors** | Problem Details with `urn:mowercare:problem:*` types |
| **Observability** | Structured logging; deepen with Actuator/APM as needed ([deployment.md](deployment.md)) |

## Related documents

- [api-reference.md](api-reference.md) — endpoints and error codes  
- [authentication.md](authentication.md) — tokens, filters, bootstrap  
- [mobile-architecture.md](mobile-architecture.md) — navigation and client layers  
- [developer-guide.md](developer-guide.md) — local development  
