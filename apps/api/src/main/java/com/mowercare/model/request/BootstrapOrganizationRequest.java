package com.mowercare.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapOrganizationRequest(
		@NotBlank @Size(max = 255) String organizationName,
		@NotBlank @Email String adminEmail,
		@NotBlank @Size(min = 8, max = 128) String adminPassword) {
}
