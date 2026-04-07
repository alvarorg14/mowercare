package com.mowercare.organization;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.organization.Organization;
import com.mowercare.organization.request.OrganizationProfilePatchRequest;
import com.mowercare.organization.response.OrganizationProfileResponse;
import com.mowercare.organization.OrganizationRepository;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization-scoped API (tenant boundary)")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationProfileController {

	private final OrganizationRepository organizationRepository;

	public OrganizationProfileController(OrganizationRepository organizationRepository) {
		this.organizationRepository = organizationRepository;
	}

	@GetMapping("/{organizationId}/profile")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get organization profile")
	@ApiResponse(
			responseCode = "200",
			description = "Organization profile",
			content = @Content(schema = @Schema(implementation = OrganizationProfileResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public OrganizationProfileResponse getProfile(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		Organization org = organizationRepository
				.findById(organizationId)
				.orElseThrow(() -> new IllegalStateException("Organization not found for tenant"));
		return toResponse(org);
	}

	@PatchMapping("/{organizationId}/profile")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Update organization profile (Admin only)")
	@ApiResponse(
			responseCode = "200",
			description = "Updated organization profile",
			content = @Content(schema = @Schema(implementation = OrganizationProfileResponse.class)))
	@ApiResponse(responseCode = "400", description = "Validation failed (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(
			responseCode = "403",
			description = "JWT organization does not match path, or caller is not Admin (RFC 7807)")
	public OrganizationProfileResponse patchProfile(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody OrganizationProfilePatchRequest body) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		Organization org = organizationRepository
				.findById(organizationId)
				.orElseThrow(() -> new IllegalStateException("Organization not found for tenant"));
		org.setName(body.name().trim());
		organizationRepository.save(org);
		return toResponse(org);
	}

	private static OrganizationProfileResponse toResponse(Organization org) {
		return new OrganizationProfileResponse(org.getId(), org.getName(), org.getCreatedAt(), org.getUpdatedAt());
	}
}
