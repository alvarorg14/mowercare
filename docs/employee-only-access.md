# Employee-only access (FR27)

MowerCare **v1** is built for **employees** of a service organization (field technicians and admins). There is:

- **No public self-registration** — end customers do not create accounts in the app or API.
- **No end-customer portal** — the mobile client is not a consumer-facing product in MVP.
- **Organization creation** — only via controlled **bootstrap** (`POST /api/v1/bootstrap/organization` with `X-Bootstrap-Token` on an empty database). See the root [`README.md`](../README.md#first-organization-and-admin-bootstrap).
- **Employee accounts** — created or invited by an **Org Admin** after bootstrap (`POST /api/v1/organizations/{organizationId}/users`, invite flow, `POST /api/v1/auth/accept-invite` for pending invites).

For API route permissions, see [`rbac-matrix.md`](rbac-matrix.md). For data minimization on accounts, see [`account-data-pii.md`](account-data-pii.md).
