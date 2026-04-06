package com.mowercare.model;

import java.util.Optional;

/**
 * MVP notification taxonomy: dot-separated domain event strings (architecture). Only a subset of
 * {@link IssueChangeType} values produce notification rows in v1 — see {@link #fromIssueChangeType(IssueChangeType)}.
 */
public enum NotificationEventType {
	/** New issue persisted. */
	ISSUE_CREATED("issue.created"),
	/** Assignee changed (including clear). */
	ISSUE_ASSIGNED("issue.assigned"),
	/** Status transition (resolved, closed, etc.). */
	ISSUE_STATUS_CHANGED("issue.status_changed");

	private final String taxonomyValue;

	NotificationEventType(String taxonomyValue) {
		this.taxonomyValue = taxonomyValue;
	}

	public String taxonomyValue() {
		return taxonomyValue;
	}

	/**
	 * Maps history discriminator to MVP notification type; empty when the change should not emit a notification row.
	 */
	public static Optional<NotificationEventType> fromIssueChangeType(IssueChangeType changeType) {
		return switch (changeType) {
			case CREATED -> Optional.of(ISSUE_CREATED);
			case ASSIGNEE_CHANGED -> Optional.of(ISSUE_ASSIGNED);
			case STATUS_CHANGED -> Optional.of(ISSUE_STATUS_CHANGED);
			default -> Optional.empty();
		};
	}

	/** Resolves persisted {@code notification_events.event_type} back to this enum. */
	public static Optional<NotificationEventType> fromTaxonomyValue(String taxonomyValue) {
		if (taxonomyValue == null || taxonomyValue.isBlank()) {
			return Optional.empty();
		}
		for (NotificationEventType t : values()) {
			if (t.taxonomyValue().equals(taxonomyValue)) {
				return Optional.of(t);
			}
		}
		return Optional.empty();
	}
}
