package com.mowercare.issue;

/**
 * Sort field for {@code GET .../issues}. Query uses camelCase: {@code updatedAt}, {@code createdAt},
 * {@code priority}.
 */
public enum IssueListSortField {
	UPDATED_AT("updatedAt"),
	CREATED_AT("createdAt"),
	PRIORITY("priority");

	private final String queryValue;

	IssueListSortField(String queryValue) {
		this.queryValue = queryValue;
	}

	public String queryValue() {
		return queryValue;
	}
}
