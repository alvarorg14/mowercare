package com.mowercare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.model.request.CreateEmployeeUserRequest;
import com.mowercare.model.response.CreateEmployeeUserResponse;
import com.mowercare.model.response.EmployeeUserResponse;
import com.mowercare.service.OrganizationUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(
		name = "Organization users",
		description = "Employee user provisioning for an organization (Admin only; employee accounts, not customers)")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationUsersController {

	private final OrganizationUserService organizationUserService;

	public OrganizationUsersController(OrganizationUserService organizationUserService) {
		this.organizationUserService = organizationUserService;
	}

	@GetMapping("/{organizationId}/users")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "List employee users in this organization")
	@ApiResponse(
			responseCode = "200",
			description = "Users (email, role, account status — no secrets)",
			content = @Content(schema = @Schema(implementation = EmployeeUserResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "Tenant mismatch or not Admin (RFC 7807)")
	public List<EmployeeUserResponse> listUsers(
			@PathVariable UUID organizationId, @AuthenticationPrincipal Jwt jwt) {
		return organizationUserService.listUsers(organizationId, jwt);
	}

	@GetMapping("/{organizationId}/users/{userId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get one employee user by id")
	@ApiResponse(responseCode = "200", description = "User")
	@ApiResponse(responseCode = "404", description = "User not found in this organization")
	@ApiResponse(responseCode = "403", description = "Tenant mismatch or not Admin (RFC 7807)")
	public EmployeeUserResponse getUser(
			@PathVariable UUID organizationId,
			@PathVariable UUID userId,
			@AuthenticationPrincipal Jwt jwt) {
		return organizationUserService
				.getUser(organizationId, userId, jwt)
				.orElseThrow(() -> new ResourceNotFoundException("User not found in this organization"));
	}

	@PostMapping(value = "/{organizationId}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Create or invite an employee user",
			description =
					"Provide initialPassword to create an active account immediately; omit it to invite (pending) — invite token is returned once in the response.")
	@ApiResponse(
			responseCode = "201",
			description = "User created",
			content = @Content(schema = @Schema(implementation = CreateEmployeeUserResponse.class)))
	@ApiResponse(responseCode = "409", description = "Email already used in this organization (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "Tenant mismatch or not Admin (RFC 7807)")
	public CreateEmployeeUserResponse createUser(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateEmployeeUserRequest body) {
		return organizationUserService.createUser(organizationId, jwt, body);
	}
}
