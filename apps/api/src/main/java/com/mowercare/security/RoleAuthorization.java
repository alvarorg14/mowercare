package com.mowercare.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.mowercare.user.ForbiddenRoleException;
import com.mowercare.auth.InvalidAccessTokenClaimsException;
import com.mowercare.user.UserRole;

/**
 * Role checks for JWTs issued by this API ({@code role} claim matches {@link UserRole#name()}).
 *
 * <p><strong>Missing or blank {@code role}:</strong> {@link InvalidAccessTokenClaimsException} → {@code 401}
 * {@code AUTH_INVALID_TOKEN}.</p>
 *
 * <p><strong>Unknown {@code role} string:</strong> when verifying a specific {@link UserRole}, treated like failing
 * that check → {@link ForbiddenRoleException} with the {@code required} role from the caller (HTTP {@code 403}
 * {@code FORBIDDEN_ROLE}).</p>
 */
public final class RoleAuthorization {

	private static final String EMPLOYEE_ROLES_DETAIL = "This action requires the ADMIN or TECHNICIAN role.";

	private RoleAuthorization() {}

	/**
	 * Requires the caller's JWT {@code role} claim to equal {@code required}.
	 */
	public static void requireRole(Jwt jwt, UserRole required) {
		String raw = jwt.getClaimAsString("role");
		if (raw == null || raw.isBlank()) {
			throw new InvalidAccessTokenClaimsException();
		}
		UserRole actual;
		try {
			actual = UserRole.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			throw new ForbiddenRoleException(required);
		}
		if (actual != required) {
			throw new ForbiddenRoleException(required);
		}
	}

	public static void requireAdmin(Jwt jwt) {
		requireRole(jwt, UserRole.ADMIN);
	}

	/**
	 * Requires an organization employee: {@link UserRole#ADMIN} or {@link UserRole#TECHNICIAN} (MVP employee roles).
	 */
	public static void requireEmployee(Jwt jwt) {
		String raw = jwt.getClaimAsString("role");
		if (raw == null || raw.isBlank()) {
			throw new InvalidAccessTokenClaimsException();
		}
		UserRole actual;
		try {
			actual = UserRole.valueOf(raw.trim());
		} catch (IllegalArgumentException ignored) {
			throw new ForbiddenRoleException(UserRole.ADMIN, EMPLOYEE_ROLES_DETAIL);
		}
		if (actual != UserRole.ADMIN && actual != UserRole.TECHNICIAN) {
			throw new ForbiddenRoleException(UserRole.ADMIN, EMPLOYEE_ROLES_DETAIL);
		}
	}
}
