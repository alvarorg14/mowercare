package com.mowercare.common.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Interprets {@link DataIntegrityViolationException} causes without relying solely on free-text messages.
 */
public final class DataIntegrityViolations {

	private static final String UQ_USERS_ORG_EMAIL = "uq_users_organization_id_email";

	private DataIntegrityViolations() {}

	/**
	 * True when the violation is the unique (organization_id, email) pair on {@code users}.
	 */
	public static boolean isDuplicateOrgEmail(DataIntegrityViolationException ex) {
		Throwable cause = ex.getMostSpecificCause();
		if (cause instanceof ConstraintViolationException hce) {
			String name = hce.getConstraintName();
			if (name != null && UQ_USERS_ORG_EMAIL.equalsIgnoreCase(name)) {
				return true;
			}
		}
		String msg = cause != null ? String.valueOf(cause.getMessage()) : "";
		return msg.contains(UQ_USERS_ORG_EMAIL);
	}
}
