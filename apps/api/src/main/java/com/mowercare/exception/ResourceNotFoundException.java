package com.mowercare.exception;

/**
 * Requested resource does not exist or is not visible in this context (RFC 7807 404).
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
