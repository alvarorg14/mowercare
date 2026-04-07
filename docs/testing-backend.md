# Backend testing (`apps/api`)

Commands:

- Unit + integration: `cd apps/api && ./mvnw -B verify` (or `mvn -B verify` if Maven is on `PATH`).
- CI: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs `mvn -B verify` on Ubuntu with Docker (Testcontainers PostgreSQL).

**Prerequisites:** JDK **25**, **Docker** for integration tests (PostgreSQL 16 via Testcontainers).

## BDD naming (Story 5.1 AC6)

- New `@Test` methods use **`given` / `when` / `then`** segments in the method name and an **`@DisplayName`** with a readable Given–When–Then sentence.
- `@ParameterizedTest` uses a `name = "..."` template when present.

## Integration tests satisfying Story 5.1 AC2 (representative flows)

| Requirement | Covered by (existing `*IT`) |
|-------------|------------------------------|
| Cross-tenant denial | [`TenantScopeIT`](../apps/api/src/test/java/com/mowercare/organization/TenantScopeIT.java) |
| Auth on protected routes | [`AuthIT`](../apps/api/src/test/java/com/mowercare/auth/AuthIT.java), [`RbacEnforcementIT`](../apps/api/src/test/java/com/mowercare/user/RbacEnforcementIT.java) |
| Issues HTTP path + OpenAPI alignment | [`IssueCreateIT`](../apps/api/src/test/java/com/mowercare/issue/IssueCreateIT.java), [`IssueListIT`](../apps/api/src/test/java/com/mowercare/issue/IssueListIT.java), [`IssuePatchIT`](../apps/api/src/test/java/com/mowercare/issue/IssuePatchIT.java), [`IssueDetailIT`](../apps/api/src/test/java/com/mowercare/issue/IssueDetailIT.java) |
| Notifications HTTP path | [`NotificationInboxIT`](../apps/api/src/test/java/com/mowercare/notification/NotificationInboxIT.java) |

Push delivery does not call real FCM in tests: [`NoOpPushNotificationSenderConfig`](../apps/api/src/test/java/com/mowercare/testsupport/NoOpPushNotificationSenderConfig.java).

## No-logic types (no dedicated unit test)

These types are **data-only**, **Spring Data interfaces**, **simple exception constructors**, **wiring-only config**, or **marker contracts**. They are listed here per Story 5.1 AC1 (single inventory).

| Category | Types |
|----------|--------|
| Application entry | `ApiApplication` |
| Spring Data repositories | `DevicePushTokenRepository`, `IssueChangeEventRepository`, `IssueRepository`, `NotificationEventRepository`, `NotificationRecipientRepository`, `OrganizationRepository`, `RefreshTokenRepository`, `UserRepository` |
| DTOs / request / response records | Per-domain `*.request` / `*.response` packages (e.g. `issue.request`, `auth.response`; records with validation only where applicable — validation is exercised via controller tests) |
| JPA entities | `DevicePushToken`, `Issue`, `IssueChangeEvent`, `NotificationEvent`, `NotificationRecipient`, `Organization`, `RefreshToken`, `User` |
| Simple enums (no custom behavior tests required here) | `AccountStatus`, `DevicePushPlatform`, `IssueChangeType`, `IssueListScope`, `IssuePriority`, `IssueStatus`, `UserRole` |
| Exception types (constructors / getters only) | `BootstrapAlreadyCompletedException`, `EmptyIssuePatchException`, `ForbiddenRoleException`, `InvalidAccessTokenClaimsException`, `InvalidBootstrapTokenException`, `InvalidCredentialsException`, `InvalidIssueListQueryException`, `InvalidIssuePatchException`, `InvalidRefreshTokenException`, `InvalidScopeException`, `InvalidStatusTransitionException`, `InviteTokenInvalidException`, `IssueClosedException`, `LastAdminDeactivationException`, `LastAdminRemovalException`, `ResourceNotFoundException`, `TenantAccessDeniedException`, `UserDeactivatedManagementException`, `UserEmailConflictException` |
| Interface | `PushNotificationSender` |
| Wiring-oriented config (no branch logic to unit-test in isolation) | `FirebaseConfiguration`, `FirebaseProperties`, `JacksonObjectMapperConfig`, `JwtConfig`, `JwtProperties`, `BootstrapProperties`, `InviteProperties`, `OpenApiConfig`, `SecurityConfig` |
| `FcmPushNotificationSender` | Real FCM I/O; behavior covered with **no-op** `PushNotificationSender` in ITs |

**Note:** Logic-heavy types receive dedicated `*Test` / `@WebMvcTest` / `*IT` as listed in the test source tree; this table only records **exempt** categories.
