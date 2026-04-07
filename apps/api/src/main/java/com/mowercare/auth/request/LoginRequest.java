package com.mowercare.auth.request;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotNull UUID organizationId,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 255) String password) {}
