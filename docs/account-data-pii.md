# Account and invite data (NFR-PR1)

Employee provisioning stores **minimal** personal data for MVP:

- **Email** — identifier and login; normalized (trim + lower-case) consistently for login and admin APIs.
- **Timestamps** — `created_at` / `updated_at` on user rows.
- **Invite tokens** — only a **SHA-256 hash** of the opaque token is stored; the raw token is returned **once** to the admin when creating a pending invite (no email delivery in MVP).

There is **no** public self-registration; org creation remains **bootstrap-only**. For the full **FR27** access model (employees vs customers), see [`employee-only-access.md`](employee-only-access.md). See [`rbac-matrix.md`](rbac-matrix.md) for who may call each route.
