package com.mowercare.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Organization profile fields that may be updated (MVP: name).")
public record OrganizationProfilePatchRequest(
		@NotBlank @Size(max = 255) @Schema(description = "Organization display name") String name) {}
