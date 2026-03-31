package com.mowercare.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.model.Issue;
import com.mowercare.model.IssueListScope;
import com.mowercare.model.request.IssueCreateRequest;
import com.mowercare.model.response.IssueCreatedResponse;
import com.mowercare.model.response.IssueListResponse;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;
import com.mowercare.service.IssueService;

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
		name = "Issues",
		description =
				"Organization-scoped issues. GET list supports `scope=open|all|mine` (default `open`); POST create is live; `_admin/reassign` remains stub until later stories — see docs/rbac-matrix.md.")
@SecurityRequirement(name = "bearer-jwt")
public class IssueStubController {

	private final IssueService issueService;

	public IssueStubController(IssueService issueService) {
		this.issueService = issueService;
	}

	@GetMapping("/{organizationId}/issues")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "List issues",
			description =
					"Returns up to 200 issues for the organization, sorted by `updatedAt` desc then `id` desc. "
							+ "Query `scope`: `open` (default, non-terminal statuses), `all`, or `mine` (assignee = caller). "
							+ "Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "200", description = "Issue list", content = @Content(schema = @Schema(implementation = IssueListResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid `scope` query value (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public IssueListResponse listIssues(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Schema(
					description = "Queue scope: `open` (default), `all`, or `mine`",
					allowableValues = {"open", "all", "mine"})
					@RequestParam(required = false)
					String scope) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		IssueListScope scopeEnum = IssueListScope.parse(scope);
		UUID actorUserId = UUID.fromString(jwt.getSubject());
		return issueService.listIssues(organizationId, actorUserId, scopeEnum);
	}

	@PostMapping(value = "/{organizationId}/issues", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Create issue",
			description = "Creates an issue in this organization. Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(
			responseCode = "201",
			description = "Created",
			content = @Content(schema = @Schema(implementation = IssueCreatedResponse.class)))
	@ApiResponse(responseCode = "400", description = "Validation or JSON parse error (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	@ApiResponse(responseCode = "404", description = "Organization, actor, or assignee not found (RFC 7807)")
	public IssueCreatedResponse createIssue(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody IssueCreateRequest body) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		UUID actorUserId = UUID.fromString(jwt.getSubject());
		Issue issue = issueService.createIssue(
				organizationId,
				actorUserId,
				body.title().trim(),
				nullIfBlank(body.description()),
				body.status(),
				body.priority(),
				body.assigneeUserId(),
				nullIfBlank(body.customerLabel()),
				nullIfBlank(body.siteLabel()));
		return toCreatedResponse(issue, body.assigneeUserId());
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

	/**
	 * Use {@code assigneeUserId} from the request for the response to avoid lazy-loading {@code Issue#getAssignee()}
	 * after the service transaction completes.
	 */
	private static IssueCreatedResponse toCreatedResponse(Issue issue, UUID assigneeUserId) {
		return new IssueCreatedResponse(
				issue.getId(),
				issue.getTitle(),
				issue.getStatus(),
				issue.getPriority(),
				issue.getDescription(),
				issue.getCustomerLabel(),
				issue.getSiteLabel(),
				assigneeUserId,
				issue.getCreatedAt());
	}

	private static String nullIfBlank(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		return s.trim();
	}
}
