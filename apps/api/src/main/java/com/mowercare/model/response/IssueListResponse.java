package com.mowercare.model.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated issue list (max 200 items per request; sorted by updatedAt desc, id desc)")
public record IssueListResponse(
		@Schema(description = "Issue rows") List<IssueListItemResponse> items) {}
