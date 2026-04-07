# Deployment guide

MowerCare is designed as a **stateless Spring Boot API** plus a **PostgreSQL** database and an **Expo** mobile client. This repo does **not** include production Terraform/Helm; use this checklist with your cloud of choice.

---

## Deployment overview

| Component | Typical shape |
|-----------|----------------|
| **API** | JVM process or container running the Spring Boot fat JAR (`mvn package`), behind TLS termination |
| **Database** | Managed PostgreSQL (16 compatible), backups and HA per provider |
| **Mobile** | **EAS Build** (or equivalent) for iOS/Android; distribute via TestFlight / Play internal track before GA |
| **Push** | Optional **Firebase** project; API uses Firebase Admin SDK when enabled |

---

## Environment configuration

Set the same variables as local development, but use a **secret manager** in production:

- **Database:** `SPRING_DATASOURCE_*`
- **JWT:** `MOWERCARE_JWT_SECRET` (strong, rotated), `MOWERCARE_JWT_ISSUER`, TTLs
- **Bootstrap:** `MOWERCARE_BOOTSTRAP_TOKEN` — protect fiercely; disable or rotate after first org creation
- **Firebase:** `MOWERCARE_FIREBASE_ENABLED`, `GOOGLE_APPLICATION_CREDENTIALS` or workload identity

Never commit secrets. Align `MOWERCARE_JWT_ISSUER` with your public API URL if you validate issuer strictly.

---

## Docker

- **Local Postgres:** `apps/api/docker-compose.yml` runs `postgres:16-alpine` for development.
- **API container:** not shipped in-repo; build a JAR with `mvn -B package` and use a minimal **JRE 25** base image, non-root user, and health checks pointing at your load balancer.

---

## Database

- **Migrations:** Liquibase runs on API startup; ensure **one** API instance owns migrations during deploy, or run migrations as a separate job before rolling out new code.
- **Backups:** managed DB automated backups + restore drills.
- **Connections:** size pool for expected load; monitor connections and slow queries.

---

## API deployment checklist

- [ ] JDK **25** runtime (or container with same)
- [ ] **TLS** at load balancer or ingress
- [ ] Strong **`MOWERCARE_JWT_SECRET`** (≥ 32 bytes) stored in secrets manager
- [ ] **CORS** / allowed origins if you add a web client later
- [ ] **Health** — expose Spring Boot Actuator health if you add the dependency (not mandatory in this repo’s default POM; add when needed)
- [ ] **Logs** — JSON structured logs in production; no tokens or passwords in logs
- [ ] **Rate limiting** — consider API gateway limits for `/api/v1/auth/*` when exposed publicly

---

## Mobile deployment

- Configure **`EXPO_PUBLIC_API_BASE_URL`** (or `extra.apiBaseUrl` in `app.config.ts`) to your **production API** HTTPS origin.
- Use **EAS** profiles for dev/staging/prod; do not ship dev API URLs in store builds.
- iOS/Android push: configure Firebase and store policies per store guidelines.

---

## Firebase (optional FCM)

1. Create a Firebase project; download service account JSON for the server.
2. Set `MOWERCARE_FIREBASE_ENABLED=true` and credentials path (or use cloud secret mounting).
3. Ensure mobile registers device tokens via **`PUT /api/v1/organizations/{id}/device-push-tokens`**.

When disabled, in-app notifications still work via REST; push sends are skipped.

---

## Monitoring and observability

- Start with **structured logs** and HTTP access logs at the edge.
- Add **Actuator** (`/actuator/health`, `/actuator/readiness`) when you need orchestrator probes.
- **APM/tracing** (OpenTelemetry, etc.) can be added later — see architecture planning for deferred observability.

---

## Security hardening

- Rotate JWT secret with a planned cutover (short access TTL reduces blast radius).
- **TLS** everywhere; HSTS at edge.
- Review **RBAC** when adding endpoints ([rbac-matrix.md](rbac-matrix.md)).
- **Dependency updates:** Maven `versions:display-dependency-updates`, npm audit in `apps/mobile`.

---

## Related documents

- [developer-guide.md](developer-guide.md) — local run and env vars  
- [authentication.md](authentication.md) — tokens and bootstrap  
- [architecture.md](architecture.md) — components  
