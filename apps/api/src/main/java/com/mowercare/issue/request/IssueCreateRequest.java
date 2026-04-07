package com.mowercare.issue.request;

import java.util.UUID;

import com.mowercare.issue.IssuePriority;
import com.mowercare.issue.IssueStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create issue (MVP fields)")
public record IssueCreateRequest(
		@NotBlank @Size(max = 500) @Schema(description = "Issue title", example = "Blade replacement") String title,
		@Size(max = 20000) @Schema(description = "Long description", nullable = true) String description,
		@NotNull @Schema(description = "Initial status", example = "OPEN") IssueStatus status,
		@NotNull @Schema(description = "Priority", example = "MEDIUM") IssuePriority priority,
		@Schema(description = "Assignee user id in this organization; omit for unassigned", nullable = true) UUID assigneeUserId,
		@Size(max = 500) @Schema(description = "Customer label (FR12)", nullable = true) String customerLabel,
		@Size(max = 500) @Schema(description = "Site label (FR12)", nullable = true) String siteLabel) {}
