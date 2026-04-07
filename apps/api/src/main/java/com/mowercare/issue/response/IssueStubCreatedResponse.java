package com.mowercare.issue.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stub issue create response — Epic 3 replaces with real issues")
public record IssueStubCreatedResponse(
		@Schema(description = "Placeholder id") UUID id,
		@Schema(description = "Stub marker") String status) {}
