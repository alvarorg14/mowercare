package com.mowercare.issue.response;

import java.time.Instant;
import java.util.UUID;

import com.mowercare.issue.IssuePriority;
import com.mowercare.issue.IssueStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Created issue")
public record IssueCreatedResponse(
		@Schema(description = "Issue id") UUID id,
		@Schema(description = "Title") String title,
		@Schema(description = "Status") IssueStatus status,
		@Schema(description = "Priority") IssuePriority priority,
		@Schema(description = "Description", nullable = true) String description,
		@Schema(description = "Customer label", nullable = true) String customerLabel,
		@Schema(description = "Site label", nullable = true) String siteLabel,
		@Schema(description = "Assignee user id", nullable = true) UUID assigneeUserId,
		@Schema(description = "Created at (UTC)") Instant createdAt) {}
