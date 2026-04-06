package com.mowercare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional Firebase / FCM credentials. When {@code enabled} is false or path is empty, push sends are no-op.
 *
 * @param enabled When true, {@link com.google.firebase.FirebaseApp} is initialized from {@code credentialsPath}
 * @param credentialsPath Filesystem path to the service account JSON (often same as {@code GOOGLE_APPLICATION_CREDENTIALS})
 */
@ConfigurationProperties(prefix = "mowercare.firebase")
public record FirebaseProperties(boolean enabled, String credentialsPath) {}
