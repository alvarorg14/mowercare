package com.mowercare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mowercare.exception.ForbiddenRoleException;
import com.mowercare.exception.InvalidAccessTokenClaimsException;
import com.mowercare.model.UserRole;

class RoleAuthorizationTest {

	private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000099");

	@Test
	@DisplayName("given blank role claim when requireAdmin then InvalidAccessTokenClaimsException")
	void givenBlankRole_whenRequireAdmin_thenInvalidClaims() {
		Jwt jwt = jwt("");

		assertThatThrownBy(() -> RoleAuthorization.requireAdmin(jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given TECHNICIAN when requireAdmin then ForbiddenRoleException")
	void givenTechnician_whenRequireAdmin_thenForbidden() {
		Jwt jwt = jwt(UserRole.TECHNICIAN.name());

		assertThatThrownBy(() -> RoleAuthorization.requireAdmin(jwt))
				.isInstanceOf(ForbiddenRoleException.class)
				.extracting(ex -> ((ForbiddenRoleException) ex).getRequiredRole())
				.isEqualTo(UserRole.ADMIN);
	}

	@Test
	@DisplayName("given ADMIN when requireAdmin then no exception")
	void givenAdmin_whenRequireAdmin_thenOk() {
		Jwt jwt = jwt(UserRole.ADMIN.name());

		RoleAuthorization.requireAdmin(jwt);

		assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
	}

	@Test
	@DisplayName("given unknown role string when requireAdmin then ForbiddenRoleException")
	void givenUnknownRole_whenRequireAdmin_thenForbidden() {
		Jwt jwt = jwt("DISPATCHER");

		assertThatThrownBy(() -> RoleAuthorization.requireAdmin(jwt))
				.isInstanceOf(ForbiddenRoleException.class);
	}

	@Test
	@DisplayName("given ADMIN when requireEmployee then no exception")
	void givenAdmin_whenRequireEmployee_thenOk() {
		RoleAuthorization.requireEmployee(jwt(UserRole.ADMIN.name()));
	}

	@Test
	@DisplayName("given TECHNICIAN when requireEmployee then no exception")
	void givenTechnician_whenRequireEmployee_thenOk() {
		RoleAuthorization.requireEmployee(jwt(UserRole.TECHNICIAN.name()));
	}

	@Test
	@DisplayName("given DISPATCHER when requireEmployee then ForbiddenRoleException with employee detail")
	void givenDispatcher_whenRequireEmployee_thenForbidden() {
		Jwt jwt = jwt("DISPATCHER");

		assertThatThrownBy(() -> RoleAuthorization.requireEmployee(jwt))
				.isInstanceOf(ForbiddenRoleException.class)
				.extracting(ex -> ((ForbiddenRoleException) ex).getDetailForProblem())
				.isEqualTo("This action requires the ADMIN or TECHNICIAN role.");
	}

	private static Jwt jwt(String role) {
		Instant now = Instant.now();
		var builder = Jwt.withTokenValue("unit-test-token")
				.subject(USER.toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("organizationId", ORG.toString())
				.header("alg", "none");
		if (role != null) {
			builder.claim("role", role);
		}
		return builder.build();
	}
}
