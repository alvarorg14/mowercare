package com.mowercare.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Remove a device token so push is no longer sent to it (sign-out or disable push)")
public record DevicePushTokenDeleteRequest(
		@NotBlank @Size(min = 10, max = 4096) @Schema(description = "Same token string previously registered") String token) {}
