package com.mowercare.issue.request;

import java.util.UUID;

import com.mowercare.issue.IssuePriority;
import com.mowercare.issue.IssueStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/** OpenAPI schema for PATCH body (controller parses JSON to {@link com.fasterxml.jackson.databind.JsonNode} then {@link IssuePatch#from}). */
@Schema(
		description =
				"Partial issue update: JSON object with at least one known field (camelCase). Omit fields you do not change.")
public record IssueUpdateRequest(
		@Schema(nullable = true, description = "Issue title", example = "Blade replacement") String title,
		@Schema(nullable = true, description = "Description; null clears when permitted") String description,
		@Schema(nullable = true, description = "Status") IssueStatus status,
		@Schema(nullable = true, description = "Priority") IssuePriority priority,
		@Schema(nullable = true, description = "Assignee user id in this organization; null unassigns") UUID assigneeUserId,
		@Schema(nullable = true, description = "Customer label") String customerLabel,
		@Schema(nullable = true, description = "Site label") String siteLabel) {}
