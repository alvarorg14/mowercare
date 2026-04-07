package com.mowercare.user.request;

import com.mowercare.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin provisioning of an employee. Omit {@code initialPassword} to create a pending invite (token returned once).
 */
public record CreateEmployeeUserRequest(
		@NotBlank @Email String email,
		@NotNull UserRole role,
		@Size(min = 8, max = 255) String initialPassword) {}
