# mowercare

Service management for robotic lawn mower installations: issue tracking, notifications, and operational workflows.

## Monorepo layout

| Path | Description |
|------|-------------|
| `apps/api` | Spring Boot 4.x API (Maven, Java 25, PostgreSQL, Liquibase) |
| `apps/mobile` | Expo (React Native) client — Router, Paper, TanStack Query |
| `_bmad-output/` | Planning and implementation artifacts (BMad) |

## Access model (FR27)

The product is **employee-only** in v1: **no** public self-registration and **no** end-customer app access. The first organization is created via **bootstrap** (below); additional users are **invited or created by an Org Admin**. See [`docs/employee-only-access.md`](docs/employee-only-access.md).

## Prerequisites

- **JDK 25** (for the API)
- **Maven 3.9+** on your `PATH` (this repo does **not** ship the Maven Wrapper)
- **Node.js** (LTS recommended) and **npm**
- **Docker** running locally when you run **`mvn verify`** or **`mvn test`** in `apps/api` — tests use **Testcontainers** to start PostgreSQL (same requirement as CI, which runs Docker-backed jobs)
- **PostgreSQL** reachable from your machine when running the API locally (configure JDBC URL and credentials; see `.env.example`)

## Run the API

From the repository root:

```bash
cd apps/api
mvn spring-boot:run
```

The API expects a PostgreSQL database. **Spring Boot** reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the **process environment** only — it does **not** automatically load a project-root `.env` file. Export the variables in your shell, configure them in your IDE run configuration, use a tool such as **direnv**, or add a community `.env` loader if you want file-based loading. Variable names are listed in `.env.example`.

Schema changes are applied by **Liquibase** on startup. In `application.yaml`, Hibernate is set to `spring.jpa.hibernate.ddl-auto: none` so the database schema is **not** owned by Hibernate DDL.

### First organization and admin (bootstrap)

On an **empty** database (no organizations), set **`MOWERCARE_BOOTSTRAP_TOKEN`** in the environment (see `.env.example`), then call:

```http
POST /api/v1/bootstrap/organization
X-Bootstrap-Token: <same value as MOWERCARE_BOOTSTRAP_TOKEN>
Content-Type: application/json

{"organizationName":"Your org","adminEmail":"admin@example.com","adminPassword":"min 8 chars"}
```

A second bootstrap returns **409** with Problem Details (`BOOTSTRAP_ALREADY_COMPLETED`). Wrong or missing token → **401** (`BOOTSTRAP_UNAUTHORIZED`). Do not commit real tokens or passwords to git.

## Run the mobile app

```bash
cd apps/mobile
npx expo start
```

Use the Expo CLI prompts to open iOS simulator, Android emulator, or web. The app uses **Expo Router**, **React Native Paper**, and **TanStack Query** (see `apps/mobile/package.json` / `app.config.ts` for the Expo SDK version).

## Testing

**Overview (commands, CI vs local, prerequisites, coverage policy):** [`docs/testing.md`](docs/testing.md)

| Doc | Scope |
|-----|--------|
| [`docs/testing-backend.md`](docs/testing-backend.md) | API: `mvn verify`, Testcontainers, integration tests |
| [`docs/testing-mobile.md`](docs/testing-mobile.md) | Mobile: Jest, lint, typecheck |
| [`docs/testing-e2e.md`](docs/testing-e2e.md) | Maestro UI flows (local / optional CI), seed script |

## CI

Pull requests run [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- **API — verify:** JDK 25, install Maven 3.9.9, then `mvn -B verify` in `apps/api` (unit + integration with Testcontainers PostgreSQL).
- **Mobile:** `npm ci`, `npm run lint`, `npm run typecheck`, `npm test -- --ci` with an **informational** Jest coverage summary, then `npm run check:contrast`.

Maestro E2E is **not** part of the default PR workflow; see [`docs/testing-e2e.md`](docs/testing-e2e.md) for why and for local runs.
