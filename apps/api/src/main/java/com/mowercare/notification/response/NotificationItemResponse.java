package com.mowercare.notification.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One in-app notification for the current user")
public record NotificationItemResponse(
		@Schema(description = "notification_recipients.id — use for mark-read") UUID id,
		@Schema(description = "Referenced issue") UUID issueId,
		@Schema(description = "Issue title for display") String issueTitle,
		@Schema(description = "Taxonomy string, e.g. issue.created") String eventType,
		@Schema(description = "When the domain event occurred (UTC)") Instant occurredAt,
		@Schema(description = "Whether the user has marked this row read") boolean read) {}
