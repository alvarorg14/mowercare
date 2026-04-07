package com.mowercare.issue;

/** Unknown field, invalid field value, or malformed patch body. */
public class InvalidIssuePatchException extends RuntimeException {

	public InvalidIssuePatchException(String message) {
		super(message);
	}
}
