package com.mowercare.model.response;

import java.time.Instant;
import java.util.UUID;

import com.mowercare.model.IssuePriority;
import com.mowercare.model.IssueStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue summary row for list views")
public record IssueListItemResponse(
		@Schema(description = "Issue id") UUID id,
		@Schema(description = "Title") String title,
		@Schema(description = "Status") IssueStatus status,
		@Schema(description = "Priority") IssuePriority priority,
		@Schema(description = "Customer label", nullable = true) String customerLabel,
		@Schema(description = "Site label", nullable = true) String siteLabel,
		@Schema(description = "Assignee user id", nullable = true) UUID assigneeUserId,
		@Schema(description = "Assignee display label (email in MVP)", nullable = true) String assigneeLabel,
		@Schema(description = "Created at (UTC)") Instant createdAt,
		@Schema(description = "Updated at (UTC)") Instant updatedAt) {}
