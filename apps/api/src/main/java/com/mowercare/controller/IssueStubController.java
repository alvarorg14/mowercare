package com.mowercare.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.model.response.IssueListStubResponse;
import com.mowercare.model.response.IssueStubCreatedResponse;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(
		name = "Issues (stub)",
		description = "RBAC demonstration stubs — see docs/rbac-matrix.md; Epic 3 replaces with real issues")
@SecurityRequirement(name = "bearer-jwt")
public class IssueStubController {

	private static final UUID STUB_ISSUE_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000001");

	@GetMapping("/{organizationId}/issues")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "List issues (stub)",
			description = "Stub empty list. Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "200", description = "Stub list")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public IssueListStubResponse listIssues(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		return new IssueListStubResponse(List.of());
	}

	@PostMapping("/{organizationId}/issues")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Create issue (stub)",
			description = "Stub create. Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "201", description = "Stub created")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public IssueStubCreatedResponse createIssue(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@RequestBody(required = false) Map<String, Object> ignored) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		return new IssueStubCreatedResponse(STUB_ISSUE_ID, "stub");
	}

	@PostMapping("/{organizationId}/issues/_admin/reassign")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Admin-only reassign stub",
			description = "Stub admin-only action — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "204", description = "Success (no body)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(
			responseCode = "403",
			description = "JWT organization does not match path, or caller is not Admin (RFC 7807)")
	public void adminReassignStub(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
	}
}
