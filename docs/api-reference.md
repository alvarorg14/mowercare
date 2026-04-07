# MowerCare API reference

Base URL pattern: **`/api/v1/...`**

- **JSON** request and response bodies use **camelCase** property names.
- **Authentication:** protected routes expect `Authorization: Bearer <access_jwt>` unless noted.
- **Errors:** [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) Problem Details with `Content-Type: application/problem+json` and a stable machine-readable **`code`** (see [Problem Details codes](#problem-details-codes)).

Interactive docs: when the API is running, OpenAPI is served by springdoc (`/v3/api-docs`, Swagger UI under `/swagger-ui` — exact paths match `apps/api` configuration).

---

## Authentication — `/api/v1/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/login` | No | Email/password login for an organization |
| POST | `/api/v1/auth/refresh` | No | Exchange refresh token for new access + refresh pair |
| POST | `/api/v1/auth/logout` | No | Revoke refresh token |
| POST | `/api/v1/auth/accept-invite` | No | Complete invite with token + new password |

### `POST /api/v1/auth/login`

**Request body** (`LoginRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `organizationId` | UUID | Required |
| `email` | string | Required, email format |
| `password` | string | Required, 8–255 chars |

**Response** `200` — `TokenResponse`

| Field | Type |
|-------|------|
| `accessToken` | string |
| `refreshToken` | string |
| `tokenType` | string (e.g. `Bearer`) |
| `expiresIn` | number (seconds) |

### `POST /api/v1/auth/refresh`

**Request body** (`RefreshRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `refreshToken` | string | Required |

**Response** `200` — `TokenResponse` (same shape as login).

### `POST /api/v1/auth/logout`

**Request body** (`LogoutRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `refreshToken` | string | Required |

**Response** `204` — no body.

### `POST /api/v1/auth/accept-invite`

**Request body** (`AcceptInviteRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `token` | string | Required |
| `password` | string | Required, 8–255 chars |

**Response** `204` — no body.

---

## Bootstrap — `/api/v1/bootstrap`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/bootstrap/organization` | `X-Bootstrap-Token` | Create first organization + admin (empty DB only) |

### `POST /api/v1/bootstrap/organization`

**Headers:** `X-Bootstrap-Token` must match `MOWERCARE_BOOTSTRAP_TOKEN`.

**Request body** (`BootstrapOrganizationRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `organizationName` | string | Required, max 255 |
| `adminEmail` | string | Required, email |
| `adminPassword` | string | Required, 8–128 chars |

**Response** `201` — `BootstrapOrganizationResponse`

| Field | Type |
|-------|------|
| `organizationId` | UUID |
| `userId` | UUID |

---

## Organizations — `/api/v1/organizations/{organizationId}/...`

All paths below require **Bearer JWT** unless stated. If `{organizationId}` ≠ JWT `organizationId`, the API returns **`403`** with `TENANT_ACCESS_DENIED` before role checks.

### Tenant scope

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/api/v1/organizations/{organizationId}/tenant-scope` | Admin, Technician | Verify JWT aligns with org (probe) |

**Response** `200` — `TenantScopeResponse`

| Field | Type |
|-------|------|
| `organizationId` | UUID |
| `userId` | UUID |
| `role` | string (`ADMIN` \| `TECHNICIAN`) |

### Organization profile

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/api/v1/organizations/{organizationId}/profile` | Admin, Technician | Read org name |
| PATCH | `/api/v1/organizations/{organizationId}/profile` | Admin only | Update org name |

**PATCH body** (`OrganizationProfilePatchRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, max 255 |

**Response** `200` — `OrganizationProfileResponse`

| Field | Type |
|-------|------|
| `id` | UUID |
| `name` | string |
| `createdAt` | string (ISO-8601 instant) |
| `updatedAt` | string (ISO-8601 instant) |

---

## Users — `/api/v1/organizations/{organizationId}/...`

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `.../users` | Admin | List employee users |
| GET | `.../assignable-users` | Admin, Technician | Active employees for assignee picker |
| GET | `.../users/{userId}` | Admin | Get one user |
| POST | `.../users` | Admin | Create user or invite |
| PATCH | `.../users/{userId}` | Admin | Update role |
| POST | `.../users/{userId}/deactivate` | Admin | Deactivate user |

### `GET .../users`

**Response** `200` — JSON array of `EmployeeUserResponse`

| Field | Type |
|-------|------|
| `id` | UUID |
| `email` | string |
| `role` | `ADMIN` \| `TECHNICIAN` |
| `accountStatus` | `PENDING_INVITE` \| `ACTIVE` \| `DEACTIVATED` |
| `createdAt` | string (ISO-8601) |

### `GET .../assignable-users`

**Response** `200` — JSON array of `AssignableUserResponse` (`id`, `email`, `role`, `accountStatus`).

### `GET .../users/{userId}`

**Response** `200` — `EmployeeUserResponse`.

### `POST .../users`

**Request body** (`CreateEmployeeUserRequest`)

| Field | Type | Notes |
|-------|------|--------|
| `email` | string | Required, email |
| `role` | `ADMIN` \| `TECHNICIAN` | Required |
| `initialPassword` | string | Optional; if omitted, invite flow may apply |

**Response** `201` — `CreateEmployeeUserResponse` (includes `inviteToken` when applicable; may be omitted when null).

### `PATCH .../users/{userId}`

**Request body** (`UpdateEmployeeUserRoleRequest`)

| Field | Type |
|-------|------|
| `role` | `ADMIN` \| `TECHNICIAN` |

**Response** `200` — `EmployeeUserResponse`.

### `POST .../users/{userId}/deactivate`

**Response** `200` — `EmployeeUserResponse`.

---

## Issues — `/api/v1/organizations/{organizationId}/issues`

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `.../issues` | Admin, Technician | List issues (filters, scope, sort) |
| POST | `.../issues` | Admin, Technician | Create issue |
| GET | `.../issues/{issueId}` | Admin, Technician | Issue detail |
| PATCH | `.../issues/{issueId}` | Admin, Technician | Partial update |
| GET | `.../issues/{issueId}/change-events` | Admin, Technician | Paginated history |
| POST | `.../issues/_admin/reassign` | Admin | Stub (returns **403** for Technician) |

### `GET .../issues`

**Query parameters**

| Param | Type | Notes |
|-------|------|--------|
| `scope` | string | `open` (default), `all`, `mine` |
| `status` | string[] | Repeat param; enum names |
| `priority` | string[] | Repeat param; enum names |
| `sort` | string | e.g. `updatedAt`, `createdAt`, `priority` |
| `direction` | string | `asc` \| `desc` |

**Response** `200` — `IssueListResponse`

| Field | Type |
|-------|------|
| `items` | `IssueListItemResponse[]` |

List items include id, title, status, priority, labels, assignee id/label, timestamps (see OpenAPI / Java types for full fields).

### `POST .../issues`

**Request body** (`IssueCreateRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `title` | string | Required, max 500 |
| `description` | string | Optional, max 20000 |
| `status` | enum | Required — `OPEN`, `IN_PROGRESS`, `WAITING`, `RESOLVED`, `CLOSED` |
| `priority` | enum | Required — `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `assigneeUserId` | UUID | Optional |
| `customerLabel` | string | Optional, max 500 |
| `siteLabel` | string | Optional, max 500 |

**Response** `201` — `IssueCreatedResponse`.

### `GET .../issues/{issueId}`

**Response** `200` — `IssueDetailResponse`.

### `PATCH .../issues/{issueId}`

**Body:** JSON object parsed server-side as a **partial patch** (unknown fields rejected; empty object rejected). OpenAPI may document an `IssueUpdateRequest` for documentation; runtime uses flexible patch semantics.

**Response** `200` — `IssueDetailResponse`.

### `GET .../issues/{issueId}/change-events`

**Pagination:** Spring `page` and `size` (0-based page).

**Response** `200` — `IssueChangeEventsResponse` with `items` and page metadata (`totalElements`, `totalPages`, `number`, `size`).

---

## Notifications — `/api/v1/organizations/{organizationId}/notifications`

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `.../notifications` | Admin, Technician | Inbox (paginated) |
| PATCH | `.../notifications/{recipientId}/read` | Admin, Technician | Mark one as read |

### `GET .../notifications`

**Pagination:** `page`, `size`.

**Response** `200` — `NotificationListResponse`

| Field | Type |
|-------|------|
| `items` | `NotificationItemResponse[]` |
| `totalElements` | number |
| `totalPages` | number |
| `number` | number |
| `size` | number |

`NotificationItemResponse` includes `id`, `issueId`, `issueTitle`, `eventType`, `occurredAt`, `read`.

### `PATCH .../notifications/{recipientId}/read`

**Response** `204`.

---

## Device push tokens — `/api/v1/organizations/{organizationId}/device-push-tokens`

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| PUT | `.../device-push-tokens` | Admin, Technician | Register or update token |
| DELETE | `.../device-push-tokens` | Admin, Technician | Remove token |

### `PUT .../device-push-tokens`

**Request body** (`DevicePushTokenPutRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `token` | string | Required, 10–4096 chars |
| `platform` | enum | Required — `IOS`, `ANDROID`, `UNKNOWN` |

**Response** `200` — `DevicePushTokenResponse` (`id`).

### `DELETE .../device-push-tokens`

**Request body** (`DevicePushTokenDeleteRequest`)

| Field | Type | Validation |
|-------|------|------------|
| `token` | string | Required, 10–4096 chars |

**Response** `204`.

---

## Problem Details codes

Stable `type` URIs use the form `urn:mowercare:problem:<CODE>`.

| HTTP | `code` | Typical situation |
|------|--------|-------------------|
| 401 | `BOOTSTRAP_UNAUTHORIZED` | Bootstrap token missing/wrong |
| 409 | `BOOTSTRAP_ALREADY_COMPLETED` | Bootstrap when orgs already exist |
| 400 | `VALIDATION_ERROR` | Bean validation, bad JSON, invalid query/patch |
| 401 | `AUTH_INVALID_CREDENTIALS` | Wrong email/password |
| 401 | `AUTH_REFRESH_INVALID` | Invalid/expired refresh |
| 401 | `AUTH_INVALID_TOKEN` | Bad Bearer JWT |
| 401 | `AUTH_REQUIRED` | Missing Bearer on protected route |
| 403 | `TENANT_ACCESS_DENIED` | Path org ≠ JWT org |
| 403 | `FORBIDDEN_ROLE` | Role not allowed for operation |
| 409 | `USER_EMAIL_CONFLICT` | Duplicate email in org |
| 400 | `INVITE_TOKEN_INVALID` | Invite token invalid/expired |
| 404 | `NOT_FOUND` | Resource not in tenant |
| 409 | `LAST_ADMIN_REMOVAL` | Cannot demote last admin |
| 409 | `LAST_ADMIN_DEACTIVATION` | Cannot deactivate last admin |
| 403 | `USER_DEACTIVATED` | User deactivated |
| 403 | `ACCOUNT_DEACTIVATED` | Account not active |
| 409 | `INVALID_STATUS_TRANSITION` | Illegal issue status change |
| 409 | `ISSUE_CLOSED` | Mutation blocked on closed issue |

Some infrastructure paths may return **500** with a generic Problem Details shape for unexpected constraint violations (`DATA_CONSTRAINT_VIOLATION`).

For route-level Admin vs Technician rules, see [rbac-matrix.md](rbac-matrix.md).

---

## Related documents

- [authentication.md](authentication.md) — token lifecycle and security filters  
- [database-schema.md](database-schema.md) — persistence model  
- [architecture.md](architecture.md) — layers and boundaries  
