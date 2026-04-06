# Test Automation Summary

Generated: 2026-04-03 (QA generate E2E / automation workflow)

## Generated tests

### API tests

- [x] Existing suite under `apps/api/src/test/java` — issue flows covered by `IssueCreateIT`, `IssueListIT`, `IssueDetailIT`, `IssuePatchIT`, `IssueChangeEventsIT`, plus auth/RBAC ITs. No new Java files were added in this pass.

### UI / mobile tests

- [x] `apps/mobile/__tests__/IssueActivityTimeline.test.ts` — `summarizeChangeEvent` behavior (issue activity copy: status, assignee, unknown types).

### Tooling added

- [x] `apps/mobile/jest.config.js` — `jest-expo` preset.
- [x] `apps/mobile/package.json` — `test` / `test:watch` scripts; devDependencies: `jest`, `jest-expo`, `@types/jest`, `@testing-library/react-native`.

## Coverage (high level)

| Area | Notes |
|------|--------|
| API endpoints | Issue CRUD/list/history already exercised by integration tests when Docker is available. |
| Mobile issue UX | Automated coverage starts with pure helpers (`summarizeChangeEvent`). Screens and navigation are candidates for RTL or Maestro/Detox next. |

## Test runs

| Command | Result in this environment |
|---------|------------------------------|
| `cd apps/mobile && npm test` | Pass |
| `cd apps/api && mvn test` | Failed here: Testcontainers could not reach Docker (`Could not find a valid Docker environment`). **Run with Docker running** on your machine or CI. |

## Next steps

- Run `mvn test` in CI with Docker (or a Testcontainers-supported remote environment).
- Extend mobile tests with React Native Testing Library for interactive components (e.g. `AssigneePicker`) using mocked API/query clients.
- Optionally add Maestro or Detox for full end-to-end flows on simulator/device.
