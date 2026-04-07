package com.mowercare.issue;

import com.mowercare.issue.InvalidStatusTransitionException;
import com.mowercare.issue.IssueStatus;

/**
 * MVP rules: CLOSED is terminal — no transition to another status. All other transitions are
 * allowed between non-terminal states and RESOLVED.
 */
public final class IssueStatusTransitionValidator {

	private IssueStatusTransitionValidator() {}

	public static void validate(IssueStatus from, IssueStatus to) {
		if (from == IssueStatus.CLOSED && to != IssueStatus.CLOSED) {
			throw new InvalidStatusTransitionException(from, to);
		}
	}
}
