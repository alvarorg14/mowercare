package com.mowercare.exception;

/** Thrown when an update is attempted on an issue in CLOSED status (immutable). */
public class IssueClosedException extends RuntimeException {

	public IssueClosedException() {
		super("Issue is closed and cannot be modified");
	}
}
