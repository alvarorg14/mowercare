# End-to-end UI tests (Maestro)

Black-box UI flows for **critical journeys** that unit tests do not cover. The primary tool is **[Maestro](https://maestro.mobile.dev/)** (declarative YAML; one primary approach per [Story 5.3](../_bmad-output/implementation-artifacts/5-3-end-to-end-tests-for-critical-flows.md)).

Related docs:

- Unit / component tests: [`testing-mobile.md`](testing-mobile.md)
- API integration tests (CI fallback): [`testing-backend.md`](testing-backend.md)

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **Maestro CLI** | Install: `curl -Ls "https://get.maestro.mobile.dev" \| bash` |
| **API** | Running Spring Boot app; PostgreSQL per [`.env.example`](../.env.example) |
| **Mobile** | iOS Simulator, Android emulator, or device with the app installed |
| **Node / JDK** | Same as repo ([`README.md`](../README.md)); API tests use JDK **25** |
| **Docker** | Required for `mvn verify` in `apps/api` (Testcontainers), not for Maestro alone |

## Toolchain

- **Flows:** [`.maestro/`](../.maestro/) — `issues_smoke.yaml`, `notifications_smoke.yaml`
- **Data seeding:** [`.maestro/seed.sh`](../.maestro/seed.sh) (runs [`.maestro/seed.py`](../.maestro/seed.py)) — bootstrap org (if DB is empty), login, create **E2E Smoke Issue** so the issues list is non-empty

Selectors use **accessibility labels** and visible text from the app (there are no `testID`s on these screens). Example: auth fields use `accessibilityLabel` **Organization ID**, **Email**, **Password** ([`app/(auth)/index.tsx`](../apps/mobile/app/(auth)/index.tsx)).

## Environment variables

**Never commit real passwords or tokens.** Export in your shell or use a local untracked file (see [`.gitignore`](../.gitignore) — `.env` is ignored).

| Variable | Required | Purpose |
|----------|----------|---------|
| `E2E_APP_ID` | Yes | Android **applicationId** / iOS **bundle id** of the app under test. **Expo Go (Android):** `host.exp.exponent`. For a dev client or release build, use the id from your build output / Xcode / Gradle. |
| `E2E_API_URL` | No | API origin for seed script (default `http://localhost:8080`) |
| `E2E_BOOTSTRAP_TOKEN` | For seed | Same value as **`MOWERCARE_BOOTSTRAP_TOKEN`** on the API |
| `E2E_ORG_ID` | Sometimes | If bootstrap already ran (**409**), set to your org UUID; seed skips bootstrap |
| `E2E_ORG_NAME` | No | Bootstrap org name (default `E2E Org`) |
| `E2E_ADMIN_EMAIL` | Yes | Login email (must match bootstrapped admin if org was just created) |
| `E2E_ADMIN_PASSWORD` | Yes | Login password (min 8 chars) |
| `E2E_ISSUE_TITLE` | No | Title of the seeded issue (default `E2E Smoke Issue`); flows tap this text on the list |

**Mobile app → API:** set **`EXPO_PUBLIC_API_BASE_URL`** to the same host the device/emulator can reach (e.g. `http://10.0.2.2:8080` for Android emulator → host loopback, or LAN IP for a physical device). See [`apps/mobile/lib/config.ts`](../apps/mobile/lib/config.ts).

## Seeding the API

1. Start PostgreSQL and the API (`cd apps/api && mvn spring-boot:run` with env from `.env.example`).
2. Export `E2E_BOOTSTRAP_TOKEN`, **`E2E_ADMIN_PASSWORD`** (required — no default in the script), and optional overrides for email and other `E2E_*` vars.
3. Run:

```bash
bash .maestro/seed.sh
```

4. Copy each printed `export ...` line from the script output into your shell (do not use `eval` on script output; if the script errors, fix the failure before exporting).

If the database was **already** bootstrapped, set **`E2E_ORG_ID`** and credentials for an existing user in that org, then run the seed script again (it will only login + create the issue).

## Running Maestro

1. Start Metro: `cd apps/mobile && npx expo start`.
2. Open the app in **Expo Go** or a **development build** on the simulator/emulator/device.
3. Export all **`E2E_*`** variables (including **`E2E_APP_ID`** and values from the seed step).
4. Run one flow:

```bash
maestro test .maestro/issues_smoke.yaml
# or
maestro test .maestro/notifications_smoke.yaml
```

**Expo Go vs dev client:** Maestro drives the **installed** app. Expo Go is the fastest way to iterate locally; if a flow fails because a native module is missing in Expo Go, use an EAS/dev build and set **`E2E_APP_ID`** accordingly.

### Push / deep links

Automating **FCM/APNs** or **tap-to-open from push** is **out of scope** for default CI (no Apple/Google signing secrets). Cover those in **manual / device-lab** runs or rely on unit tests such as [`apps/mobile/__tests__/push-navigation.test.ts`](../apps/mobile/__tests__/push-navigation.test.ts).

## CI (GitHub Actions)

### Default pull requests

PR CI **does not** run Maestro. It runs:

- **API:** `mvn -B verify` (Testcontainers PostgreSQL) — see [`testing-backend.md`](testing-backend.md)
- **Mobile:** `npm ci`, lint, typecheck, `npm test -- --ci` — see [`testing-mobile.md`](testing-mobile.md)

That gives **fast** feedback and **API contract / domain** regression without emulators.

### Trade-off (explicit)

Full **device UI E2E** needs an Android emulator or iOS simulator (or attached device), extra setup time, and is more **flaky** than unit/API tests. This repo therefore treats Maestro as **local** or **optional** automation (e.g. a future **`workflow_dispatch`**, **scheduled**, or **self-hosted** job). **API-level** confidence remains the integration suite above.

If you add a CI job later, consider caching the Android SDK, using **`workflow_dispatch`** first, and keeping Maestro off the critical path until stable.

### How E2E fits next to PR checks

| Layer | Where it runs | Role |
|-------|----------------|------|
| Maestro | Local / optional job | Smoke UI journeys |
| `mvn verify` | Every PR | Tenant, auth, issues, notifications HTTP paths |
| Jest | Every PR | Mobile units and mocks |

No silent gap: UI smoke is **documented here**; API regression is **always** on PR.
