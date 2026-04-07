package com.mowercare.organization;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.organization.response.TenantScopeResponse;
import com.mowercare.security.TenantPathAuthorization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Tenant boundary probe for {@code /api/v1/organizations/{organizationId}/...}.
 *
 * <p>If the path {@code organizationId} does not match the {@code organizationId} claim in the JWT, the API returns
 * <strong>403 Forbidden</strong> with Problem Details ({@code TENANT_ACCESS_DENIED}) — explicit wrong-tenant denial for B2B diagnostics.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization-scoped API (tenant boundary)")
@SecurityRequirement(name = "bearer-jwt")
public class TenantScopeController {

	@GetMapping("/{organizationId}/tenant-scope")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Verify JWT organization matches path organization")
	@ApiResponse(
			responseCode = "200",
			description = "Tenant scope matches",
			content = @Content(schema = @Schema(implementation = TenantScopeResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public TenantScopeResponse tenantScope(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		UUID userId = TenantPathAuthorization.requireSubjectAsUuid(jwt);
		String role = jwt.getClaimAsString("role");
		return new TenantScopeResponse(organizationId, userId, role);
	}
}
