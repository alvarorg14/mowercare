package com.mowercare.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.model.response.NotificationListResponse;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;
import com.mowercare.service.NotificationInboxService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Notifications", description = "Organization-scoped in-app notification feed for the current user")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationInboxController {

	private final NotificationInboxService notificationInboxService;

	public NotificationInboxController(NotificationInboxService notificationInboxService) {
		this.notificationInboxService = notificationInboxService;
	}

	@GetMapping("/{organizationId}/notifications")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "List notifications for the current user",
			description =
					"Returns notification_recipients rows for the JWT subject in this organization, newest first "
							+ "(recipient createdAt desc). Query `page` and `size` (default size 50, max 100). "
							+ "Admin and Technician — see docs/rbac-matrix.md.")
	@ApiResponse(
			responseCode = "200",
			description = "Page of notifications",
			content = @Content(schema = @Schema(implementation = NotificationListResponse.class)))
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public NotificationListResponse list(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@PageableDefault(size = 50) Pageable pageable) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireEmployee(jwt);
		UUID userId = UUID.fromString(jwt.getSubject());
		return notificationInboxService.listForUser(organizationId, userId, pageable);
	}

	@PatchMapping("/{organizationId}/notifications/{recipientId}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Mark one notification as read",
			description =
					"Sets read_at on the recipient row if it belongs to the JWT subject in this organization. Idempotent. "
							+ "Admin and Technician — see docs/rbac-matrix.md.")
	@ApiResponse(responseCode = "204", description = "Marked read (or already read)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	@ApiResponse(responseCode = "404", description = "Recipient row not found for this user (RFC 7807)")
	public void markRead(
			@PathVariable UUID organizationId,
			@PathVariable UUID recipientId,
			@AuthenticationPrincipal Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireEmployee(jwt);
		UUID userId = UUID.fromString(jwt.getSubject());
		notificationInboxService.markRead(organizationId, userId, recipientId);
	}
}
