# Story 1.5: Global API security and tenant context on protected routes

Status: review

<!-- Ultimate context engine analysis completed — comprehensive developer guide created. -->

## Story

As the **system**,
I want **protected `/api/v1/**` routes to require a valid JWT and validated org membership (tenant context)**,
so that **unauthenticated access is denied (FR6) and cross-tenant path tricks fail (FR1, NFR-S3)**.

**Implements:** FR6, FR1; NFR-S3.

**Epic:** Epic 1 — Bootstrap, tenancy & authentication — follows **1.4** (JWT issuance, claims `sub`, `organizationId`, `role`); **precedes** **1.6** (mobile shell; client will send Bearer tokens on protected calls).

## Acceptance Criteria

1. **Given** a request **without** a valid `Authorization: Bearer <access_token>` header (missing, malformed, or not Bearer)  
   **When** a **protected** `/api/v1/**` endpoint is called (not on the public allowlist below)  
   **Then** the response is **401 Unauthorized** with **`Content-Type: application/problem+json`**, stable machine **`code`**, and the same Problem Details field conventions as existing handlers (`type`, `title`, `status`, `detail`, `instance`).

2. **Given** a **valid** access JWT (issued by this API, not expired, signature valid) whose **`organizationId` claim** does **not** equal the **`organizationId` path variable** on a protected route that exposes org scope in the URL  
   **When** the request is processed  
   **Then** access is denied with **403 Forbidden** (preferred for this MVP — explicit “wrong tenant”) **or** **404 Not Found** — **pick one policy, document it in code (class-level Javadoc) and in this story’s Dev Notes**, and **integration tests must assert the chosen status and `code`**.

3. **Given** a valid access JWT whose **`organizationId` matches** the resource path’s org id  
   **When** that protected endpoint is called  
   **Then** the request succeeds (**2xx**) and proves tenant context is available to the controller layer (e.g. minimal JSON echo or fixed payload — implementation detail).

4. **Given** **invalid signature**, **wrong issuer**, **expired** access token, or **malformed** JWT  
   **When** a protected endpoint is called  
   **Then** the API responds with **401** and Problem Details (no stack traces, no token bodies in logs).

**Public allowlist (must remain callable without Bearer JWT):**

- `/api/v1/auth/**` — login, refresh, logout (Story 1.4).
- `/api/v1/bootstrap/**` — bootstrap uses **`X-Bootstrap-Token`**, not JWT; do **not** require Bearer here.
- **OpenAPI / docs** used by the project (e.g. **`/v3/api-docs/**`**, **`/swagger-ui/**`** if enabled) — **permitAll** so CI and devs can read the contract without a token (align with existing springdoc setup).

## Tasks / Subtasks

- [x] **Dependencies & JWT validation** (AC: 1, 4)
  - [x] Add **`spring-boot-starter-oauth2-resource-server`** (Boot **4.0.x** BOM-managed) so the filter chain can validate Bearer JWTs with a **`JwtDecoder`** bean.
  - [x] Expose a **`JwtDecoder`** using the **same symmetric key / algorithm (HS256)** as **`JwtConfig`**’s **`JwtEncoder`** (same `mowercare.jwt.secret` / `MOWERCARE_JWT_SECRET`) — avoid divergent key material between issue and validate.
  - [x] Build the decoder **programmatically** (e.g. **`NimbusJwtDecoder.withSecretKey`**) and attach validators so **`iss`** matches **`JwtProperties.issuer()`** (the same string **`JwtService`** uses for access tokens). **Do not** rely on **`spring.security.oauth2.resourceserver.jwt.issuer-uri`** alone unless you actually expose OIDC discovery for that issuer; self-issued HS256 tokens typically use a **`JwtDecoder` `@Bean`** only.
  - [x] There is **no `aud` claim** on tokens from **`JwtService`** today — ensure the **`JwtDecoder`** configuration does **not** require an audience claim (default **`NimbusJwtDecoder.withSecretKey`** behavior is fine; avoid extra validators that assume OAuth2 client `aud`).
- [x] **Security filter chain** (AC: 1, 4)
  - [x] Replace global **`permitAll()`** on `anyRequest()` in **`SecurityConfig`** with **`authenticated()`** for protected API routes; keep explicit **`permitAll`** for **`/api/v1/auth/**`**, **`/api/v1/bootstrap/**`**, and springdoc paths (see AC).
  - [x] Use **`oauth2ResourceServer().jwt()`** (or equivalent supported DSL for Boot 4) wired to your **`JwtDecoder`**.
  - [x] Customize **`AuthenticationEntryPoint`** (and as needed **`BearerTokenResolver`**) so **401** responses to API clients use **`application/problem+json`** and **`code`** consistent with **`ApiExceptionHandler`** (e.g. new type URI + code such as **`AUTH_REQUIRED`** / **`AUTH_INVALID_TOKEN`** — **document new `ProblemDetail` types** next to existing urns).
  - [x] Ensure **CSRF** remains disabled for stateless JWT APIs (existing pattern).
- [x] **Tenant-scoped protected endpoint** (AC: 2, 3)
  - [x] Introduce **one** minimal protected controller route that includes **`{organizationId}`** in the path (e.g. `GET /api/v1/organizations/{organizationId}/tenant-scope` — name is flexible) returning a small JSON body on success.
  - [x] Implement **defense in depth**: after JWT authentication, compare **`UUID` from path** to **`organizationId` claim** (both as UUIDs). Mismatch → **403** (or **404** per your documented policy). Match → **200**.
  - [x] For **403** (or **404**, if chosen), return **`application/problem+json`** with stable **`code`** and **`type`** URI under **`urn:mowercare:problem:`** — same shape as **`ApiExceptionHandler`** (not an empty body or plain text). Prefer a dedicated exception + **`@ExceptionHandler`** or shared **`ProblemDetail`** factory for consistency.
  - [x] Optionally register a small **principal / context type** (e.g. method argument **`@AuthenticationPrincipal Jwt jwt`** or a custom **`Authentication`** wrapper) so later stories can reuse **user id** and **org id** without re-parsing JWT in every controller — keep it minimal for 1.5.
- [x] **OpenAPI** (AC: 1–3)
  - [x] Register a **global** **`Bearer` JWT** security scheme in springdoc (or equivalent) so generated docs show protected routes require authentication.
  - [x] Document the new protected endpoint and **401/403** Problem Details where appropriate.
- [x] **Tests** (AC: 1–4)
  - [x] **Integration tests** (Testcontainers + **`AbstractPostgresIntegrationTest`** / `MockMvc`):  
    - Protected route **without** `Authorization` → **401** + Problem Details + `code`.  
    - Protected route with **valid** Bearer from **login** (reuse patterns from **`AuthIT`**) and **matching** `organizationId` → **2xx**.  
    - Same token, **non-matching** `organizationId` in path → **403** or **404** per documented policy.  
    - Optional: **expired** token case if practical (short TTL in test profile or clock override).
  - [x] **Do not** log access tokens in test output or assertions.

### Review / risk notes (for implementer)

- **Bootstrap vs JWT:** Do not conflate **`X-Bootstrap-Token`** with Bearer JWT; bootstrap stays on allowlist.
- **403 vs 404:** For internal B2B, **403 + stable `code`** is often clearer; **404** can reduce enumeration — **choose one** and test it.
- **Future RBAC:** Story **1.5** is **authentication + tenant boundary**; **role enforcement** beyond carrying **`role`** in JWT is **Epic 2** — do not block 1.5 on full `@PreAuthorize` matrix.

## Dev Notes

### Scope boundaries

- **Implemented tenant policy:** **403 Forbidden** with **`TENANT_ACCESS_DENIED`** when JWT `organizationId` ≠ path `organizationId` (see **`TenantScopeController`** Javadoc).
- **In scope:** Bearer JWT validation on protected routes; Problem Details for **401** (unauthenticated / bad token); **tenant mismatch** on a path that includes **`organizationId`**; OpenAPI security scheme; integration tests proving **NFR-S3** cross-tenant denial.
- **Out of scope:** Mobile client wiring (1.6–1.7); **RBAC** matrix (Epic 2); **deactivated user** denial at authentication time (FR26 — Epic 2); full **org profile** read/write (1.8) — this story may add a **minimal** org-scoped endpoint only to prove enforcement.

### Architecture compliance

- **Auth boundary:** JWT validation **before** controllers; only **auth** (and bootstrap/docs) unauthenticated. [Source: `architecture.md` — API boundaries / auth boundary]
- **Tenant boundary:** Never trust **path** `organizationId` without matching **JWT `organizationId` claim**. [Source: `architecture.md` — Critical conflict points]
- **Spring Security + explicit checks:** Align with **`@PreAuthorize` + explicit `organizationId` checks** direction; for 1.5, explicit check on the sample route is sufficient. [Source: `architecture.md` — Authorization row]
- **Problem Details:** RFC 7807, **`application/problem+json`**, stable **`code`**, **no secrets/tokens in logs**. [Source: `architecture.md` — API & errors; Story 1.4]
- **REST:** `/api/v1/...`, camelCase JSON. [Source: `architecture.md`]

### Technical requirements

| Area | Requirement |
|------|-------------|
| Runtime | Spring Boot **4.0.x**, **Java 25**, Maven — same as **1.1–1.4** |
| JWT | **HS256** access tokens from **`JwtService`**; validate with **`JwtDecoder`** using **same secret** as **`JwtEncoder`** |
| Security | **`spring-boot-starter-oauth2-resource-server`** for OAuth2 Resource Server JWT filter chain |
| Errors | **401/403** must use Problem Details shape consistent with **`ApiExceptionHandler`** |

### Library / framework requirements

- **`spring-boot-starter-oauth2-resource-server`** — Bearer JWT validation, **`JwtDecoder`** support.
- **`spring-security-oauth2-jose`** — already used for **`JwtEncoder`**; **`NimbusJwtDecoder`** for symmetric validation pairs naturally with **`NimbusJwtEncoder`**.

### File structure requirements (illustrative)

```
apps/api/src/main/java/com/mowercare/
  config/
    SecurityConfig.java          # oauth2ResourceServer, permit rules, entry point
    JwtConfig.java               # extend or add @Bean JwtDecoder (or separate config class)
  controller/
    ... new tenant-scope or organization stub controller
  exception/
    ApiExceptionHandler.java     # extend if shared handlers for security exceptions preferred
apps/api/src/test/java/com/mowercare/
  controller/
    ... new *IT.java for tenant + auth enforcement
```

Keep **package-by-type** consistency with **1.2–1.4** unless doing an intentional migration.

### Testing requirements

- **`mvn verify`** green.
- Reuse **`AbstractPostgresIntegrationTest`**; seed org + user + login via **`AuthIT`** patterns to obtain **access token**.
- Assertion style: **`given…_when…_then…`** + **`@DisplayName`** where consistent with **1.3–1.4**.
- JSONPath: align with **`AuthIT`** for Problem Details (`$.code` vs `$.properties.code` — follow **working pattern from 1.4**).

### Previous story intelligence

- **1.4** left **`SecurityConfig`** with **`anyRequest().permitAll()`** — **this story removes that** for protected APIs; **`/api/v1/auth/**`** must stay **permitAll**.
- **1.4** **`JwtService.issueAccessToken`** sets claims **`sub`** (user id), **`organizationId`**, **`role`** — tenant checks must parse **`organizationId`** as **UUID** string.
- **1.4** review: **`NimbusJwtEncoder.withSecretKey`** fixed HS256 signing — mirror with **`NimbusJwtDecoder.withSecretKey`** (or equivalent) for validation.
- **1.4** **`ApiExceptionHandler`** patterns for **`code`** / `type` URIs — extend consistently for auth/authorization failures.

### Git intelligence

- Recent commits: **1.4** JWT auth stack; **1.3** bootstrap; **1.2** schema — enforcement layer is the **next** increment on the same API module.

### Latest tech notes (verify at implementation time)

- **Spring Boot 4** + **Spring Security 7**: use the **supported** OAuth2 Resource Server DSL; verify **`HttpSecurity`** lambda API against current docs if something moved.
- **JwtDecoder** bean name / **`@Primary`** if multiple JWT-related beans appear.

### Project context

- **`docs/project-context.md`** — not present; optional later (**Generate Project Context** skill).

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 1.5 (acceptance criteria)
- `_bmad-output/planning-artifacts/architecture.md` — Tenant isolation, JWT claims, auth boundary
- `_bmad-output/implementation-artifacts/1-4-authentication-api-login-refresh-logout.md` — JWT claims, SecurityConfig allowlist, test patterns

## Dev Agent Record

### Agent Model Used

Cursor (GPT-5.2) — bmad-dev-story workflow

### Debug Log References

- Spring Boot 4.0.5: **`ApiAuthenticationEntryPoint`** uses a static **`com.fasterxml.jackson.databind.ObjectMapper`** (Jackson 2 on classpath) because no **`ObjectMapper`** bean is registered by default for this stack; aligns with **`ApiExceptionHandler`** JSON shape for **`$.code`** in tests.
- **`JwtDecoder`**: **`NimbusJwtDecoder.withSecretKey`** + **`JwtValidators.createDefaultWithIssuer`** matching **`JwtService`** issuer.

### Completion Notes List

- **`spring-boot-starter-oauth2-resource-server`** added; **`JwtDecoder`** bean in **`JwtConfig`** (HS256 + issuer validation).
- **`SecurityConfig`**: stateless session; **`permitAll`** for auth, bootstrap, **`/v3/api-docs/**`**, **`/swagger-ui/**`**; **`authenticated`** elsewhere; **`oauth2ResourceServer().jwt()`** + **`ApiAuthenticationEntryPoint`** (**`AUTH_REQUIRED`** / **`AUTH_INVALID_TOKEN`**).
- **`TenantScopeController`**: **`GET /api/v1/organizations/{organizationId}/tenant-scope`**; **`TenantAccessDeniedException`** → **`ApiExceptionHandler`** (**403** **`TENANT_ACCESS_DENIED`**).
- **`OpenApiConfig`**: **`bearer-jwt`** security scheme for springdoc.
- **IT:** **`TenantScopeIT`**; **`ApiExceptionHandlerTest`** extended for tenant handler.
- Expired-access-token case left optional (not added as separate IT).

### File List

- `apps/api/pom.xml`
- `apps/api/src/main/java/com/mowercare/config/ApiAuthenticationEntryPoint.java`
- `apps/api/src/main/java/com/mowercare/config/JwtConfig.java`
- `apps/api/src/main/java/com/mowercare/config/OpenApiConfig.java`
- `apps/api/src/main/java/com/mowercare/config/SecurityConfig.java`
- `apps/api/src/main/java/com/mowercare/controller/TenantScopeController.java`
- `apps/api/src/main/java/com/mowercare/exception/ApiExceptionHandler.java`
- `apps/api/src/main/java/com/mowercare/exception/TenantAccessDeniedException.java`
- `apps/api/src/main/java/com/mowercare/model/response/TenantScopeResponse.java`
- `apps/api/src/test/java/com/mowercare/controller/TenantScopeIT.java`
- `apps/api/src/test/java/com/mowercare/exception/ApiExceptionHandlerTest.java`

### Change Log

- 2026-03-30 — Story 1.5: OAuth2 resource server JWT validation, tenant-scope endpoint, Problem Details for 401/403, OpenAPI bearer scheme, **`TenantScopeIT`**.

### Review Findings

- [x] [Review][Patch] Harden JWT claim parsing in `TenantScopeController` — **Fixed:** `InvalidAccessTokenClaimsException` + `ApiExceptionHandler` (**401** **`AUTH_INVALID_TOKEN`**); null `Jwt` guard; `TenantScopeControllerTest` coverage. **`TenantScopeIT`** uses **`springSecurity()`** on MockMvc so the filter chain runs; **`ApiAuthenticationEntryPoint`** writes Problem JSON as a **Map** so **`$.code`** matches **`ApiExceptionHandler`** (plain **`ObjectMapper`** on **`ProblemDetail`** nested `code` under `properties`).
- [x] [Review][Defer] Duplicate `requestInstance(HttpServletRequest)` for Problem `instance` — Same logic exists in `ApiAuthenticationEntryPoint` and `ApiExceptionHandler`; consider a small shared helper when touching error handling next. [`ApiAuthenticationEntryPoint.java` ~56–62, `ApiExceptionHandler.java` ~114+] — deferred, pre-existing pattern extended

## Story completion status

- **Status:** review  
- **Note:** Implementation complete; **`mvn verify`** green in `apps/api`.

## Story validation report

**Validated:** 2026-03-30 — checklist: `.cursor/skills/bmad-create-story/checklist.md`  
**Story:** `1-5-global-api-security-and-tenant-context-on-protected-routes`  
**Epic source:** `_bmad-output/planning-artifacts/epics.md` (Story 1.5)  
**Cross-check:** `JwtService` / `JwtProperties` in repo (access tokens: **`iss`**, **`sub`**, **`organizationId`**, **`role`**; **no `aud`**)

**Summary:** 2 critical gaps were **fixed in-place** in Tasks (see below). 3 enhancements recommended (optional). 1 optimization noted.

### Critical issues (addressed in this file)

1. **`issuer-uri` vs self-issued JWT** — Risk that a dev wires **`spring.security.oauth2.resourceserver.jwt.issuer-uri`** and expects metadata discovery; **mowercare** issues HS256 tokens locally with **`JwtEncoder`**, not an IdP. **Fix:** Added explicit Tasks bullets: programmatic **`JwtDecoder`**, **`iss`** alignment with **`JwtProperties`**, and **no spurious `aud`** requirement.

2. **403 body shape** — Epic requires **403 or 404** with tests; story implied Problem Details for **401** but not explicitly for **tenant mismatch**. **Fix:** Added Task to return **Problem Details** for tenant denial matching **`ApiExceptionHandler`**.

### Enhancement opportunities (optional)

1. **CORS** — Not required for 1.5 API-only work; add when mobile (1.6–1.7) calls a browser or Expo web — track outside this story.

2. **`AccessDeniedHandler`** — Only needed if you use **`@PreAuthorize`** or authorization manager that throws **`AccessDeniedException`** in the filter chain; tenant mismatch in **controller** can stay application-level **403 ProblemDetail** per Tasks.

3. **Actuator** — No **`spring-boot-starter-actuator`** in **`apps/api/pom.xml`** today; if added later, **`/actuator/health`** is often **`permitAll`** — document when introduced.

### Optimizations

1. **JSONPath in tests** — Story already points to **`$.code`**; **`AuthIT`** uses **`jsonPath("$.code")`** for Problem Details — keep new ITs consistent (no `$.properties.code`).

### Verdict

**Ready for `bmad-dev-story`** after the in-file Task updates above. Re-run this validate pass after implementation if scope changes.
