# mowercare automated tests

This folder holds repo-level test documentation. Executables live next to each app.

## API (Spring Boot)

- **Location:** `apps/api/src/test/java`
- **Style:** JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL) for integration tests (`*IT.java`).
- **Run:** `cd apps/api && mvn test`
- **Requirement:** Docker must be running so Testcontainers can start PostgreSQL.

## Mobile (Expo / React Native)

- **Location:** `apps/mobile/__tests__/` and `*.test.ts(x)` next to sources.
- **Style:** Jest with `jest-expo` preset; React Native Testing Library available for component tests.
- **Run:** `cd apps/mobile && npm test`

## True device E2E

The mobile app does not yet include Detox or Maestro flows. Add one of those (or Playwright against `expo web`) when you need full navigation E2E outside Jest.
