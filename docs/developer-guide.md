# Developer guide

How to run, test, and extend MowerCare locally. For CI vs local testing commands, see [testing.md](testing.md).

---

## Prerequisites

| Tool | Notes |
|------|--------|
| **JDK 25** | Matches `apps/api/pom.xml` and CI |
| **Maven 3.9+** | On `PATH`; repo does not ship `mvnw` |
| **Node.js 20** + **npm** | Matches [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) |
| **Docker** | Required for `mvn verify` in `apps/api` (Testcontainers) |
| **PostgreSQL** | For running the API against a real DB (or use Docker Compose in `apps/api`) |
| **Expo CLI** | Via `npx expo` (no global install required) |

Optional: **Maestro** for E2E ([testing-e2e.md](testing-e2e.md)).

---

## Clone and first-time setup

```bash
git clone https://github.com/alvarorg14/mowercare.git
cd mowercare
```

**API:** install nothing at repo root — Maven resolves Java dependencies.

**Mobile:**

```bash
cd apps/mobile
npm ci
```

---

## Environment variables

Copy names from [`.env.example`](../.env.example) into your shell or tool. Spring Boot **does not** auto-load a root `.env` file.

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | For local API | JDBC URL (default in `application.yaml`: `jdbc:postgresql://localhost:5432/mowercare`) |
| `SPRING_DATASOURCE_USERNAME` | For local API | DB user |
| `SPRING_DATASOURCE_PASSWORD` | For local API | DB password |
| `MOWERCARE_BOOTSTRAP_TOKEN` | First org only | Header token for `POST /api/v1/bootstrap/organization` |
| `MOWERCARE_JWT_SECRET` | Production | Min 32 UTF-8 bytes; HS256 signing |
| `MOWERCARE_JWT_ISSUER` | Optional | JWT issuer claim |
| `MOWERCARE_JWT_ACCESS_TTL` | Optional | Default `PT15M` |
| `MOWERCARE_JWT_REFRESH_TTL` | Optional | Default `P7D` |
| `EXPO_PUBLIC_API_BASE_URL` | Mobile | Public API origin (e.g. `http://192.168.x.x:8080` on device) |
| `MOWERCARE_FIREBASE_ENABLED` | Optional | `true` to send FCM pushes |
| `GOOGLE_APPLICATION_CREDENTIALS` | If FCM | Path to Firebase Admin JSON |

E2E / Maestro names are documented in `.env.example` comments and [testing-e2e.md](testing-e2e.md).

---

## Run the API locally

1. Start PostgreSQL (example using repo Compose):

   ```bash
   cd apps/api
   docker compose up -d
   ```

2. Export `SPRING_DATASOURCE_*` and JWT/bootstrap variables as needed.

3. Run:

   ```bash
   cd apps/api
   mvn spring-boot:run
   ```

Liquibase applies migrations on startup. Then bootstrap the first org (see [README.md](../README.md#first-organization-and-admin-bootstrap)).

---

## Run the mobile app

```bash
cd apps/mobile
npx expo start
```

- **Simulator/emulator:** use Expo prompts.
- **Physical device:** set `EXPO_PUBLIC_API_BASE_URL` to your machine’s LAN IP and port **8080** (or your reverse proxy). `localhost` on the device points to the device itself, not your computer.

---

## Running tests

| Area | Command |
|------|---------|
| API (unit + integration) | `cd apps/api && mvn -B verify` |
| Mobile lint | `cd apps/mobile && npm run lint` |
| Mobile typecheck | `cd apps/mobile && npm run typecheck` |
| Mobile unit tests | `cd apps/mobile && npm test` |
| Issue status contrast | `cd apps/mobile && npm run check:contrast` |

---

## IDE tips

- **IntelliJ IDEA:** import `apps/api` as Maven; enable **Lombok** annotation processing; JDK 25 project SDK.
- **VS Code / Cursor:** open `apps/mobile`; use ESLint and TypeScript workspace version; Expo extension optional.

---

## Debugging

- **API:** run `mvn spring-boot:run` with JVM debug flags, or use IDE “Debug” on `ApiApplication`. Breakpoints in services/controllers/integration tests.
- **Mobile:** React Native **LogBox**, Expo dev menu, `console.log`; use Flipper/React Native debugger if configured.

---

## Common tasks

### Add a REST endpoint

1. Add or extend a **controller** under `com.mowercare.<domain>`.
2. Implement **service** + **repository**; enforce **tenant** + **role** (see [authentication.md](authentication.md)).
3. Return DTOs, not entities; use **Problem Details** for errors.
4. Add **Liquibase** changeset if schema changes — never rely on Hibernate DDL in prod.
5. Add **tests** in `src/test/java` (unit and/or `@SpringBootTest` with Testcontainers pattern used in repo).

### Add a mobile screen

1. Add route under `apps/mobile/app/` (Expo Router file naming).
2. Wrap data with **TanStack Query** hooks calling `lib/*-api.ts`.
3. Use **Paper** components and `lib/theme.ts` for colors.

### Add a DB migration

1. New YAML under `apps/api/src/main/resources/db/changelog/changes/` with next id.
2. Include it from `db.changelog-master.yaml`.
3. Map columns in JPA entities and DTOs.

### Add a notification event type

1. Extend **notification taxonomy** in API code (`NotificationEventType` / recording path).
2. Ensure fan-out rules in notification services match product intent.
3. Update mobile display if new `eventType` strings appear in inbox.

---

## Troubleshooting

| Symptom | Things to check |
|---------|------------------|
| API won’t start | Postgres up? JDBC URL/user/password? Port 5432 free? |
| `mvn verify` fails | Docker running? Testcontainers needs Docker. |
| 401 on mobile | API URL correct? Token expired? Refresh failing? |
| Bootstrap 409 | Organizations already exist — DB not empty |
| Push not received | `MOWERCARE_FIREBASE_ENABLED`, credentials, device token registered |

---

## Related documents

- [deployment.md](deployment.md) — production-oriented setup  
- [architecture.md](architecture.md) — layers and boundaries  
- [api-reference.md](api-reference.md) — HTTP contract  
