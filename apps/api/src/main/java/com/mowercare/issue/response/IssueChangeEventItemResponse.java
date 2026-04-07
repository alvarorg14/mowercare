package com.mowercare.issue.response;

import java.time.Instant;
import java.util.UUID;

import com.mowercare.issue.IssueChangeType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One append-only issue change event (material field change or creation)")
public record IssueChangeEventItemResponse(
		@Schema(description = "Event id") UUID id,
		@Schema(description = "When the change occurred (UTC)") Instant occurredAt,
		@Schema(description = "Change discriminator") IssueChangeType changeType,
		@Schema(description = "User who performed the change") UUID actorUserId,
		@Schema(description = "Actor display label (email in MVP)") String actorLabel,
		@Schema(description = "Previous value as stored", nullable = true) String oldValue,
		@Schema(description = "New value as stored", nullable = true) String newValue,
		@Schema(
				description =
						"Resolved assignee label for old assignee when changeType is ASSIGNEE_CHANGED; null otherwise",
				nullable = true)
		String oldAssigneeLabel,
		@Schema(
				description =
						"Resolved assignee label for new assignee when changeType is ASSIGNEE_CHANGED; null otherwise",
				nullable = true)
		String newAssigneeLabel) {}
