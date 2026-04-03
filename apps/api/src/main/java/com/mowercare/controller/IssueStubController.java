package com.mowercare.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.issue.IssueListQueryParser;
import com.mowercare.model.Issue;
import com.mowercare.model.IssueListScope;
import com.mowercare.exception.InvalidIssuePatchException;
import com.mowercare.model.request.IssueCreateRequest;
import com.mowercare.model.request.IssuePatch;
import com.mowercare.model.request.IssueUpdateRequest;
import com.mowercare.model.response.IssueChangeEventsResponse;
import com.mowercare.model.response.IssueCreatedResponse;
import com.mowercare.model.response.IssueDetailResponse;
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
				"Organization-scoped issues. GET list supports `scope=open|all|mine` (default `open`), optional repeated `status`/`priority` filters, `sort`/`direction`; GET by id returns full detail; GET `.../issues/{issueId}/change-events` returns paginated history; POST create; PATCH partial update; `_admin/reassign` remains stub — see docs/rbac-matrix.md.")
@SecurityRequirement(name = "bearer-jwt")
public class IssueStubController {

	private final IssueService issueService;
	private final ObjectMapper objectMapper;

	public IssueStubController(IssueService issueService, ObjectMapper objectMapper) {
		this.issueService = issueService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/{organizationId}/issues")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "List issues",
			description =
					"Returns up to 200 issues for the organization. Default sort: `updatedAt` desc, `id` desc. "
							+ "Query `scope`: `open` (default, non-terminal statuses), `all`, or `mine` (assignee = caller). "
							+ "Optional repeated `status` and `priority` params (status: OPEN, IN_PROGRESS, …; priority: LOW, URGENT, …) intersect with `scope`. "
							+ "Optional `sort`: `updatedAt` (default), `createdAt`, or `priority` (severity order). Optional `direction`: `asc` or `desc` (default `desc`). "
							+ "Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "200", description = "Issue list", content = @Content(schema = @Schema(implementation = IssueListResponse.class)))
	@ApiResponse(
			responseCode = "400",
			description = "Invalid `scope`, `status`, `priority`, `sort`, or `direction` query value (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public IssueListResponse listIssues(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Schema(
					description = "Queue scope: `open` (default), `all`, or `mine`",
					allowableValues = {"open", "all", "mine"})
					@RequestParam(required = false)
					String scope,
			@Schema(description = "Repeat to filter by status (e.g. OPEN, IN_PROGRESS). Intersects with `scope`.")
					@RequestParam(required = false)
					List<String> status,
			@Schema(description = "Repeat to filter by priority (e.g. LOW, URGENT).")
					@RequestParam(required = false)
					List<String> priority,
			@Schema(
					description = "Sort field",
					allowableValues = {"updatedAt", "createdAt", "priority"})
					@RequestParam(required = false)
					String sort,
			@Schema(description = "Sort direction", allowableValues = {"asc", "desc"})
					@RequestParam(required = false)
					String direction) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		IssueListScope scopeEnum = IssueListScope.parse(scope);
		UUID actorUserId = UUID.fromString(jwt.getSubject());
		var filters = IssueListQueryParser.parse(status, priority, sort, direction);
		return issueService.listIssues(organizationId, actorUserId, scopeEnum, filters);
	}

	@PatchMapping(value = "/{organizationId}/issues/{issueId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "Patch issue",
			description =
					"Partial update (camelCase). Optional fields: `title`, `description`, `status`, `priority`, `assigneeUserId` (null to unassign), `customerLabel`, `siteLabel`. "
							+ "Admin and Technician allowed — see docs/rbac-matrix.md.",
			requestBody =
					@io.swagger.v3.oas.annotations.parameters.RequestBody(
							required = true,
							content = @Content(schema = @Schema(implementation = IssueUpdateRequest.class))))
	@ApiResponse(
			responseCode = "200",
			description = "Updated issue",
			content = @Content(schema = @Schema(implementation = IssueDetailResponse.class)))
	@ApiResponse(responseCode = "400", description = "Validation, closed issue, or invalid transition (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	@ApiResponse(responseCode = "404", description = "Issue or assignee not found (RFC 7807)")
	public IssueDetailResponse patchIssue(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@PathVariable @Schema(description = "Issue id") UUID issueId,
			@AuthenticationPrincipal Jwt jwt,
			@RequestBody String body) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		JsonNode json;
		try {
			json = objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new InvalidIssuePatchException("Invalid JSON");
		}
		IssuePatch patch = IssuePatch.from(json);
		UUID actorUserId = UUID.fromString(jwt.getSubject());
		return issueService.patchIssue(organizationId, issueId, actorUserId, patch);
	}

	@GetMapping("/{organizationId}/issues/{issueId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "Get issue by id",
			description =
					"Returns one issue in this organization. Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(
			responseCode = "200",
			description = "Issue detail",
			content = @Content(schema = @Schema(implementation = IssueDetailResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	@ApiResponse(responseCode = "404", description = "Issue not found in this organization (RFC 7807)")
	public IssueDetailResponse getIssue(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@PathVariable @Schema(description = "Issue id") UUID issueId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		return issueService.getIssue(organizationId, issueId);
	}

	@GetMapping("/{organizationId}/issues/{issueId}/change-events")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "List issue change events",
			description =
					"Returns paginated append-only history for the issue, sorted by `occurredAt` ascending. "
							+ "Query `page` and `size` (default size 50, max 100). "
							+ "Admin and Technician allowed — see docs/rbac-matrix.md.")
	@ApiResponse(
			responseCode = "200",
			description = "Change events page",
			content = @Content(schema = @Schema(implementation = IssueChangeEventsResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	@ApiResponse(responseCode = "404", description = "Issue not found in this organization (RFC 7807)")
	public IssueChangeEventsResponse listIssueChangeEvents(
			@PathVariable @Schema(description = "Organization id from URL; must match JWT") UUID organizationId,
			@PathVariable @Schema(description = "Issue id") UUID issueId,
			@AuthenticationPrincipal Jwt jwt,
			@PageableDefault(size = 50) Pageable pageable) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		return issueService.listChangeEvents(organizationId, issueId, pageable);
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
