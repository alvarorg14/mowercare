# ADR 0005: Shared-schema multi-tenancy with `organization_id`

## Status

Accepted

## Context

MowerCare is **multi-tenant**: many organizations share one application deployment. Isolation strategies include **database-per-tenant**, **schema-per-tenant**, and **shared schema** with a **tenant discriminator column** (here `organization_id`).

## Decision

Use a **single PostgreSQL database** and **shared tables** with **`organization_id`** on tenant-owned rows. Every query and mutation must scope by organization consistent with the authenticated user.

## Consequences

**Positive**

- Simpler operations: one migration path, one backup topology for early scale.
- Standard B2B SaaS pattern; fits Spring Data JPA repositories.

**Negative / trade-offs**

- **Risk of cross-tenant bugs** if a query omits `organization_id` — mitigated by reviews, helpers, and **tests** that assert tenant denial.
- No hard DB-level isolation between tenants; compliance-heavy customers may ask for stronger isolation later.

## Related

- [../database-schema.md](../database-schema.md)  
- [../authentication.md](../authentication.md)  
