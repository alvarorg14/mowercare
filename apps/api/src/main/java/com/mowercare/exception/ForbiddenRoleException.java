package com.mowercare.exception;

import com.mowercare.model.UserRole;

/**
 * Raised when the JWT {@code role} claim does not allow the requested operation.
 */
public class ForbiddenRoleException extends RuntimeException {

	private final UserRole requiredRole;

	public ForbiddenRoleException(UserRole requiredRole) {
		super("Required role: " + requiredRole.name());
		this.requiredRole = requiredRole;
	}

	public UserRole getRequiredRole() {
		return requiredRole;
	}
}
