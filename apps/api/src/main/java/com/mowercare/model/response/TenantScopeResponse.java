package com.mowercare.model.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Echo of tenant context from the access token for the matched organization path.")
public record TenantScopeResponse(
		@Schema(description = "Organization id from JWT; must match the path organization id.") UUID organizationId,
		@Schema(description = "User id from JWT subject.") UUID userId,
		@Schema(description = "Role claim from the access token.") String role) {}
