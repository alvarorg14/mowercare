package com.mowercare.issue;

/** Thrown when issue list query parameters (status, priority, sort, direction) are invalid. */
public class InvalidIssueListQueryException extends RuntimeException {

	public InvalidIssueListQueryException(String message) {
		super(message);
	}
}
