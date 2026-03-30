package com.mowercare.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mowercare.exception.InvalidAccessTokenClaimsException;

class TenantScopeControllerTest {

	private static final UUID PATH_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID JWT_ORG = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private TenantScopeController controller;

	@BeforeEach
	void setUp() {
		controller = new TenantScopeController();
	}

	@Test
	@DisplayName("given null Jwt principal when tenantScope then InvalidAccessTokenClaimsException")
	void givenNullJwt_whenTenantScope_thenInvalidClaims() {
		assertThatThrownBy(() -> controller.tenantScope(PATH_ORG, null))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given JWT with non-UUID organizationId claim when tenantScope then InvalidAccessTokenClaimsException")
	void givenMalformedJwtOrgClaim_whenTenantScope_thenInvalidClaims() {
		Jwt jwt = jwtWithOrgAndSubject("not-a-uuid", UUID.randomUUID().toString());

		assertThatThrownBy(() -> controller.tenantScope(PATH_ORG, jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given JWT with null subject when tenantScope then InvalidAccessTokenClaimsException")
	void givenNullSubject_whenTenantScope_thenInvalidClaims() {
		Jwt jwt = jwtWithOrgAndSubject(JWT_ORG.toString(), null);

		assertThatThrownBy(() -> controller.tenantScope(JWT_ORG, jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given JWT with non-UUID subject when tenantScope then InvalidAccessTokenClaimsException")
	void givenMalformedSubject_whenTenantScope_thenInvalidClaims() {
		Jwt jwt = jwtWithOrgAndSubject(JWT_ORG.toString(), "not-uuid");

		assertThatThrownBy(() -> controller.tenantScope(JWT_ORG, jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given matching org and valid claims when tenantScope then returns response")
	void givenValidClaims_whenTenantScope_thenOk() {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000099");
		Jwt jwt = jwt(JWT_ORG.toString(), userId.toString(), "ADMIN");

		var response = controller.tenantScope(JWT_ORG, jwt);

		assertThat(response.organizationId()).isEqualTo(JWT_ORG);
		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.role()).isEqualTo("ADMIN");
	}

	private static Jwt jwtWithOrgAndSubject(String organizationId, String subject) {
		return jwt(organizationId, subject, null);
	}

	private static Jwt jwt(String organizationId, String subject, String role) {
		Instant now = Instant.now();
		var builder = Jwt.withTokenValue("unit-test-token")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("organizationId", organizationId)
				.header("alg", "none");
		if (subject != null) {
			builder.subject(subject);
		}
		if (role != null) {
			builder.claim("role", role);
		}
		return builder.build();
	}
}
