package com.mowercare.exception;

/** PATCH body is empty or has no updatable content. */
public class EmptyIssuePatchException extends RuntimeException {

	public EmptyIssuePatchException() {
		super("Request must include at least one field to update");
	}
}
