package com.mowercare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.model.request.AcceptInviteRequest;
import com.mowercare.model.request.LoginRequest;
import com.mowercare.model.request.LogoutRequest;
import com.mowercare.model.request.RefreshRequest;
import com.mowercare.model.response.TokenResponse;
import com.mowercare.service.AuthService;
import com.mowercare.service.OrganizationUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, refresh, and logout (JWT access + opaque refresh tokens)")
public class AuthController {

	private final AuthService authService;
	private final OrganizationUserService organizationUserService;

	public AuthController(AuthService authService, OrganizationUserService organizationUserService) {
		this.authService = authService;
		this.organizationUserService = organizationUserService;
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Sign in with organization, email, and password")
	@ApiResponse(
			responseCode = "200",
			description = "Access and refresh tokens issued",
			content = @Content(schema = @Schema(implementation = TokenResponse.class)))
	@ApiResponse(responseCode = "401", description = "Invalid credentials (RFC 7807)")
	@ApiResponse(responseCode = "400", description = "Validation error (RFC 7807)")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request.organizationId(), request.email(), request.password());
	}

	@PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Exchange a valid refresh token for a new access + refresh pair (rotation)")
	@ApiResponse(responseCode = "200", description = "New tokens issued")
	@ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Revoke the presented refresh token")
	@ApiResponse(responseCode = "204", description = "Refresh token revoked")
	@ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
	public void logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
	}

	@PostMapping(value = "/accept-invite", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Accept employee invite and set password",
			description =
					"Unauthenticated. Pending accounts cannot sign in until this succeeds (see login). Token is single-use; not sent by email in MVP.")
	@ApiResponse(responseCode = "204", description = "Password set; account is active")
	@ApiResponse(responseCode = "400", description = "Invalid or expired invite token (RFC 7807)")
	public void acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
		organizationUserService.acceptInvite(request.token(), request.password());
	}
}
