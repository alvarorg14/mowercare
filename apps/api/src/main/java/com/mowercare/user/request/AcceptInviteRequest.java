package com.mowercare.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Complete employee invite acceptance (sets password). Public endpoint; token is single-use.
 */
public record AcceptInviteRequest(
		@NotBlank String token,
		@NotBlank @Size(min = 8, max = 255) String password) {}
