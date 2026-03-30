package com.mowercare.common;

import java.util.Locale;

/**
 * Shared email normalization for login and provisioning so addresses cannot drift between flows.
 */
public final class EmailNormalization {

	private EmailNormalization() {}

	public static String normalize(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
