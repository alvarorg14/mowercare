# Deferred work (from code reviews)

## Deferred from: code review of 1-1-initialize-monorepo-from-expo-and-spring-boot-starters.md (2026-03-27)

- **Initializr placeholder metadata in `pom.xml`:** Empty `license`, `developers`, and `scm` blocks are common Initializr noise; fill in when publishing or open-sourcing the API module.
- **Static Testcontainers container in tests:** A single static `PostgreSQLContainer` is fine for one test class; if the suite grows and JUnit parallelizes multiple classes using containers, revisit lifecycle (per-class containers, singleton reuse pattern, or dedicated base test).
