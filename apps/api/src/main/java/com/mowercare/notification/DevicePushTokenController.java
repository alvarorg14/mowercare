package com.mowercare.notification;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.notification.request.DevicePushTokenDeleteRequest;
import com.mowercare.notification.request.DevicePushTokenPutRequest;
import com.mowercare.notification.response.DevicePushTokenResponse;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;
import com.mowercare.notification.DevicePushTokenService;

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
		name = "Notifications",
		description = "Organization-scoped notifications: in-app feed and device push token registration")
@SecurityRequirement(name = "bearer-jwt")
public class DevicePushTokenController {

	private final DevicePushTokenService devicePushTokenService;

	public DevicePushTokenController(DevicePushTokenService devicePushTokenService) {
		this.devicePushTokenService = devicePushTokenService;
	}

	@PutMapping("/{organizationId}/device-push-tokens")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "Register or refresh a device push token",
			description =
					"Stores the native FCM/APNs token for the JWT subject in this organization. "
							+ "Unique key (organization, user, token): re-submitting the same token updates platform and timestamp. "
							+ "**Contract:** returns **200 OK** with `DevicePushTokenResponse` (idempotent PUT upsert); "
							+ "this is the chosen pattern versus 201/204 alternatives. "
							+ "Admin and Technician — see docs/rbac-matrix.md.")
	@ApiResponse(
			responseCode = "200",
			description = "Token registered",
			content = @Content(schema = @Schema(implementation = DevicePushTokenResponse.class)))
	@ApiResponse(responseCode = "400", description = "Validation failed (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public DevicePushTokenResponse put(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody DevicePushTokenPutRequest body) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireEmployee(jwt);
		UUID userId = TenantPathAuthorization.requireSubjectAsUuid(jwt);
		return devicePushTokenService.register(organizationId, userId, body);
	}

	@DeleteMapping("/{organizationId}/device-push-tokens")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Revoke a device push token",
			description =
					"Deletes the stored token for the JWT subject so push is no longer sent to that device. "
							+ "Idempotent when the token is unknown.")
	@ApiResponse(responseCode = "204", description = "Revoked or already absent")
	@ApiResponse(responseCode = "400", description = "Validation failed (RFC 7807)")
	@ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token (RFC 7807)")
	@ApiResponse(responseCode = "403", description = "JWT organization does not match path (RFC 7807)")
	public void delete(
			@PathVariable UUID organizationId,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody DevicePushTokenDeleteRequest body) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireEmployee(jwt);
		UUID userId = TenantPathAuthorization.requireSubjectAsUuid(jwt);
		devicePushTokenService.revoke(organizationId, userId, body.token());
	}
}
