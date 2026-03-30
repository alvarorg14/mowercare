package com.mowercare.model.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stub issue list — Epic 3 replaces with real issues")
public record IssueListStubResponse(
		@Schema(description = "Issue rows (empty in stub)") List<Object> items) {}
