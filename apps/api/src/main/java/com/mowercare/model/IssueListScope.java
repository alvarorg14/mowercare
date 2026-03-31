package com.mowercare.model;

import com.mowercare.exception.InvalidScopeException;

/**
 * Query scope for {@code GET .../issues}. API uses lowercase query values: {@code open}, {@code all}, {@code mine}.
 */
public enum IssueListScope {
	OPEN,
	ALL,
	MINE;

	/**
	 * @param raw query parameter; {@code null} or blank defaults to {@link #OPEN}
	 */
	public static IssueListScope parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return OPEN;
		}
		return switch (raw.toLowerCase()) {
			case "open" -> OPEN;
			case "all" -> ALL;
			case "mine" -> MINE;
			default -> throw new InvalidScopeException(raw);
		};
	}
}
