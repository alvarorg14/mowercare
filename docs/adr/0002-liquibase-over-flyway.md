# ADR 0002: Liquibase for database migrations (vs Flyway)

## Status

Accepted

## Context

The API uses **PostgreSQL** as the system of record. Schema changes must be **repeatable**, **reviewable**, and **independent** of Hibernate auto-DDL. Both **Liquibase** and **Flyway** are common choices.

## Decision

Use **Liquibase** with YAML/SQL changelogs under `apps/api/src/main/resources/db/changelog/`. Hibernate **`spring.jpa.hibernate.ddl-auto`** is set to **`none`**.

## Consequences

**Positive**

- Single source of truth for schema in version control.
- Consistent upgrades across dev/staging/prod.
- Aligns with product preference recorded in planning artifacts.

**Negative / trade-offs**

- Team must learn Liquibase changelog conventions.
- Flyway expertise in the org does not transfer directly — different DSL and layout.

## Related

- [../database-schema.md](../database-schema.md)  
