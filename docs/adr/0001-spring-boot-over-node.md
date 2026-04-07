# ADR 0001: Spring Boot for the HTTP API (vs Node/NestJS)

## Status

Accepted

## Context

MowerCare needs a multi-tenant REST API with relational data, transactions, RBAC, and versioned schema migrations. Alternative stacks included **NestJS/Node** and **Spring Boot/JVM**.

## Decision

Use **Spring Boot 4** on **Java 25** with **Spring Web MVC**, **Spring Data JPA**, **Spring Security**, and **PostgreSQL**.

## Consequences

**Positive**

- Strong fit for **layered** domain code (controllers → services → repositories) and **JPA** for relational modeling.
- Mature ecosystem for **security**, **validation**, and **integration testing** (including Testcontainers).
- Explicit control over **tenant** and **RBAC** rules in plain Java.

**Negative / trade-offs**

- Heavier runtime than a small Node service; team needs JVM fluency.
- Mobile TypeScript types are **not** shared with the server — contract alignment is manual or via OpenAPI/codegen later.

## Related

- [0002-liquibase-over-flyway.md](0002-liquibase-over-flyway.md)  
- [../architecture.md](../architecture.md)  
