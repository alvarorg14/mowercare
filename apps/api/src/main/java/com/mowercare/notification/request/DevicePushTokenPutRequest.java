package com.mowercare.notification.request;

import com.mowercare.notification.DevicePushPlatform;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Register or replace a native FCM/APNs device token for the current user in this organization")
public record DevicePushTokenPutRequest(
		@NotBlank
				@Size(min = 10, max = 4096)
				@Schema(description = "FCM registration token from the mobile client (expo-notifications getDevicePushTokenAsync)")
				String token,
		@NotNull @Schema(description = "Client platform") DevicePushPlatform platform) {}
