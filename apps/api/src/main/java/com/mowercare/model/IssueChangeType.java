package com.mowercare.model;

/**
 * Append-only history discriminator; aligns with material fields on {@link Issue}.
 */
public enum IssueChangeType {
	CREATED,
	STATUS_CHANGED,
	ASSIGNEE_CHANGED,
	PRIORITY_CHANGED,
	TITLE_CHANGED,
	DESCRIPTION_CHANGED,
	CUSTOMER_LABEL_CHANGED,
	SITE_LABEL_CHANGED
}
