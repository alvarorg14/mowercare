# mowercare

Service management for robotic lawn mower installations: issue tracking, notifications, and operational workflows.

## Monorepo layout

| Path | Description |
|------|-------------|
| `apps/api` | Spring Boot 4.x API (Maven, Java 25, PostgreSQL, Liquibase) |
| `apps/mobile` | Expo (React Native) client — **blank TypeScript** template |
| `_bmad-output/` | Planning and implementation artifacts (BMad) |

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

## Run the mobile app

```bash
cd apps/mobile
npx expo start
```

Use the Expo CLI prompts to open iOS simulator, Android emulator, or web. The template used is **blank TypeScript** (see `apps/mobile/package.json` / `app.json` for the Expo SDK version).

## CI

Pull requests run:

- **API:** `mvn -B verify` under JDK 25 (includes tests using Testcontainers and PostgreSQL).
- **Mobile:** `npm ci`, `npm run lint`, and `npm run typecheck`.
