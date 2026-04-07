package com.mowercare.notification.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered device push token row id (stable across re-PUT of same token)")
public record DevicePushTokenResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {}
