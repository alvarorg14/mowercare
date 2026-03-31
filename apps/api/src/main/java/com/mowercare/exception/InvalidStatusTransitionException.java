package com.mowercare.exception;

import com.mowercare.model.IssueStatus;

/** Thrown when a status change violates the MVP transition rules (e.g. reopening from CLOSED). */
public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException(IssueStatus from, IssueStatus to) {
		super("Invalid status transition from " + from + " to " + to);
	}
}
