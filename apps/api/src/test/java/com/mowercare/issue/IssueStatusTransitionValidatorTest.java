package com.mowercare.issue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.mowercare.exception.InvalidStatusTransitionException;
import com.mowercare.model.IssueStatus;

class IssueStatusTransitionValidatorTest {

	@Test
	void closedToOpen_throws() {
		assertThatThrownBy(() -> IssueStatusTransitionValidator.validate(IssueStatus.CLOSED, IssueStatus.OPEN))
				.isInstanceOf(InvalidStatusTransitionException.class);
	}

	@Test
	void closedToClosed_ok() {
		IssueStatusTransitionValidator.validate(IssueStatus.CLOSED, IssueStatus.CLOSED);
	}

	@Test
	void openToClosed_ok() {
		IssueStatusTransitionValidator.validate(IssueStatus.OPEN, IssueStatus.CLOSED);
	}
}
