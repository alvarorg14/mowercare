package com.mowercare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mowercare.auth.InvalidAccessTokenClaimsException;
import com.mowercare.common.exception.TenantAccessDeniedException;

class TenantPathAuthorizationTest {

	private final UUID pathOrg = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID jwtOrg = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	@DisplayName("given jwt org matches path when require then no exception")
	void givenJwtOrgMatchesPath_whenRequireJwtOrganizationMatchesPath_thenNoException() {
		Jwt jwt = jwtWithOrg(jwtOrg);
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(jwtOrg, jwt);
	}

	@Test
	@DisplayName("given jwt org differs from path when require then tenant denied")
	void givenJwtOrgDiffersFromPath_whenRequireJwtOrganizationMatchesPath_thenTenantDenied() {
		Jwt jwt = jwtWithOrg(jwtOrg);
		assertThatThrownBy(() -> TenantPathAuthorization.requireJwtOrganizationMatchesPath(pathOrg, jwt))
				.isInstanceOf(TenantAccessDeniedException.class);
	}

	@Test
	@DisplayName("given null jwt when require org match then invalid claims")
	void givenNullJwt_whenRequireJwtOrganizationMatchesPath_thenInvalidClaims() {
		assertThatThrownBy(() -> TenantPathAuthorization.requireJwtOrganizationMatchesPath(pathOrg, null))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given valid subject when require subject as uuid then returns uuid")
	void givenValidSubject_whenRequireSubjectAsUuid_thenReturnsUuid() {
		UUID sub = UUID.fromString("33333333-3333-3333-3333-333333333333");
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "none")
				.subject(sub.toString())
				.build();
		assertThat(TenantPathAuthorization.requireSubjectAsUuid(jwt)).isEqualTo(sub);
	}

	@Test
	@DisplayName("given blank subject when require subject as uuid then invalid claims")
	void givenBlankSubject_whenRequireSubjectAsUuid_thenInvalidClaims() {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("  ").build();
		assertThatThrownBy(() -> TenantPathAuthorization.requireSubjectAsUuid(jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given null jwt when require subject as uuid then invalid claims")
	void givenNullJwt_whenRequireSubjectAsUuid_thenInvalidClaims() {
		assertThatThrownBy(() -> TenantPathAuthorization.requireSubjectAsUuid(null))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	@Test
	@DisplayName("given organization id claim not a uuid when require path match then invalid claims")
	void givenOrganizationIdClaimNotUuid_whenRequireJwtOrganizationMatchesPath_thenInvalidClaims() {
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "none")
				.claim("organizationId", "not-a-uuid")
				.subject(UUID.randomUUID().toString())
				.build();
		assertThatThrownBy(() -> TenantPathAuthorization.requireJwtOrganizationMatchesPath(pathOrg, jwt))
				.isInstanceOf(InvalidAccessTokenClaimsException.class);
	}

	private static Jwt jwtWithOrg(UUID orgId) {
		return Jwt.withTokenValue("t")
				.header("alg", "none")
				.claim("organizationId", orgId.toString())
				.subject(UUID.randomUUID().toString())
				.build();
	}
}
