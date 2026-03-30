package com.mowercare.model.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Organization profile for API clients (camelCase JSON).")
public record OrganizationProfileResponse(
		@Schema(description = "Organization id") UUID id,
		@Schema(description = "Display name") String name,
		@Schema(description = "Created at (ISO-8601 UTC)") Instant createdAt,
		@Schema(description = "Updated at (ISO-8601 UTC)") Instant updatedAt) {}
