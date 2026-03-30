package com.mowercare.exception;

/**
 * Raised when the authenticated user's organization (from JWT) does not match the organization in the request path.
 */
public class TenantAccessDeniedException extends RuntimeException {}
