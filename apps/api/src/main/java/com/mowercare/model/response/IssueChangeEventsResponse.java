package com.mowercare.model.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated issue change history (default sort occurredAt ascending)")
public record IssueChangeEventsResponse(
		@Schema(description = "Change events for this page") List<IssueChangeEventItemResponse> items,
		@Schema(description = "Total elements across all pages") long totalElements,
		@Schema(description = "Total pages") int totalPages,
		@Schema(description = "Current page index (0-based)") int number,
		@Schema(description = "Page size") int size) {}
