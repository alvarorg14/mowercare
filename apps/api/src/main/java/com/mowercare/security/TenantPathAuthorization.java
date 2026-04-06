package com.mowercare.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.mowercare.exception.InvalidAccessTokenClaimsException;
import com.mowercare.exception.TenantAccessDeniedException;

/**
 * Validates that the {@code organizationId} path segment matches the JWT {@code organizationId} claim.
 *
 * <p>Shared by organization-scoped controllers (e.g. tenant probe, org profile).
 */
public final class TenantPathAuthorization {

	private TenantPathAuthorization() {}

	public static void requireJwtOrganizationMatchesPath(UUID organizationIdFromPath, Jwt jwt) {
		if (jwt == null) {
			throw new InvalidAccessTokenClaimsException();
		}
		UUID jwtOrg = requireUuidClaim(jwt, "organizationId");
		if (!organizationIdFromPath.equals(jwtOrg)) {
			throw new TenantAccessDeniedException();
		}
	}

	/** Resolves {@code sub} as a UUID or throws {@link InvalidAccessTokenClaimsException} (401). */
	public static UUID requireSubjectAsUuid(Jwt jwt) {
		if (jwt == null) {
			throw new InvalidAccessTokenClaimsException();
		}
		return parseRequiredUuid(jwt.getSubject());
	}

	private static UUID requireUuidClaim(Jwt jwt, String claimName) {
		return parseRequiredUuid(jwt.getClaimAsString(claimName));
	}

	private static UUID parseRequiredUuid(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new InvalidAccessTokenClaimsException();
		}
		try {
			return UUID.fromString(raw.trim());
		} catch (IllegalArgumentException ex) {
			throw new InvalidAccessTokenClaimsException();
		}
	}
}
