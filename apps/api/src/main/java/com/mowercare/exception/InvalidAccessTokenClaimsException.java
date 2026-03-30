package com.mowercare.exception;

/**
 * Raised when an authenticated JWT is missing required claims or contains values that cannot be parsed for API use
 * (e.g. {@code organizationId} or {@code sub} not a UUID string).
 */
public class InvalidAccessTokenClaimsException extends RuntimeException {}
