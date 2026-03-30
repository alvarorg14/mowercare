# RBAC matrix (MVP)

Authoritative product intent: [`_bmad-output/planning-artifacts/prd.md`](../_bmad-output/planning-artifacts/prd.md) — **RBAC matrix (product requirements level)**.

This document is the **implementation-facing** matrix for API routes. Update it when new endpoints ship or when Epic 3 issue rules refine visibility (“Mine” vs “All”).

## Roles

| Role | JWT `role` claim (`UserRole.name()`) |
|------|--------------------------------------|
| Admin | `ADMIN` |
| Technician | `TECHNICIAN` |

## Organization & admin operations

| Operation | Admin | Technician | Notes |
|-----------|-------|------------|-------|
| `GET /api/v1/organizations/{organizationId}/profile` | Allow | Allow | Read org name |
| `PATCH /api/v1/organizations/{organizationId}/profile` | Allow | Deny | `403` `FORBIDDEN_ROLE` |
| `GET /api/v1/organizations/{organizationId}/users` | Allow | Deny | List employee users (email, role, account status) |
| `GET /api/v1/organizations/{organizationId}/users/{userId}` | Allow | Deny | Get one employee user |
| `POST /api/v1/organizations/{organizationId}/users` | Allow | Deny | Create active user or invite (`PENDING_INVITE`); `409` `USER_EMAIL_CONFLICT` on duplicate email |
| `POST /api/v1/auth/accept-invite` | N/A | N/A | **Unauthenticated** — complete invite with token + password; not a role-gated route |
| `PATCH /api/v1/organizations/{organizationId}/users/{userId}` (role) | Allow | Deny | Update employee role (`ADMIN` / `TECHNICIAN`); `409` `LAST_ADMIN_REMOVAL` if removing last Admin |
| `POST /api/v1/organizations/{organizationId}/users/{userId}/deactivate` | Allow | Deny | Deactivate employee; `409` `LAST_ADMIN_DEACTIVATION` if last active Admin |

## Issue operations (stubs until Epic 3)

| Operation | Admin | Technician | Notes |
|-----------|-------|------------|-------|
| `GET /api/v1/organizations/{organizationId}/issues` | Allow | Allow | Stub list — Epic 3 replaces with real data |
| `POST /api/v1/organizations/{organizationId}/issues` | Allow | Allow | Stub create — aligns with FR11 direction |
| `POST /api/v1/organizations/{organizationId}/issues/_admin/reassign` | Allow | Deny | Stub admin-only action (`403` `FORBIDDEN_ROLE`) |

## Tenant boundary (all roles)

If path `organizationId` ≠ JWT `organizationId`, the API returns **`403`** `TENANT_ACCESS_DENIED` (before role checks).

## Mobile

The app reads the JWT **`role`** claim (and caches it with session refresh) to gate UI such as org profile edit vs read-only. **Settings** (`/settings`) links to **Organization** and **Team**; **Team** lists, invites, changes roles, and deactivates users only when `role === ADMIN` — technicians see an explanation instead of admin actions. **No secrets** in the client; use the same Problem Details **`code`** values for messaging. See [`_bmad-output/planning-artifacts/architecture.md`](../_bmad-output/planning-artifacts/architecture.md).
