package com.mowercare.exception;

import com.mowercare.model.UserRole;

/**
 * Raised when the JWT {@code role} claim does not allow the requested operation.
 */
public class ForbiddenRoleException extends RuntimeException {

	private final UserRole requiredRole;
	/** When non-null, use as Problem Details {@code detail} instead of the single-role template. */
	private final String detailOverride;

	public ForbiddenRoleException(UserRole requiredRole) {
		this(requiredRole, null);
	}

	/**
	 * @param detailOverride full Problem Details detail line (e.g. when multiple roles are allowed)
	 */
	public ForbiddenRoleException(UserRole requiredRole, String detailOverride) {
		super(detailOverride != null ? detailOverride : "Required role: " + requiredRole.name());
		this.requiredRole = requiredRole;
		this.detailOverride = detailOverride;
	}

	public UserRole getRequiredRole() {
		return requiredRole;
	}

	/** Text for HTTP Problem Details {@code detail} (RFC 7807). */
	public String getDetailForProblem() {
		if (detailOverride != null) {
			return detailOverride;
		}
		return "This action requires the " + requiredRole.name() + " role.";
	}
}
