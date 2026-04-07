package com.mowercare.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mowercare.issue.InvalidIssueListQueryException;
import com.mowercare.issue.IssueListQueryParser.IssueListFilters;
import com.mowercare.issue.IssuePriority;
import com.mowercare.issue.IssueStatus;

class IssueListQueryParserTest {

	@Test
	void parse_skipsBlanks() {
		IssueListFilters f = IssueListQueryParser.parse(List.of("OPEN", " ", ""), List.of("LOW"), null, null);
		assertThat(f.statuses()).containsExactly(IssueStatus.OPEN);
		assertThat(f.priorities()).containsExactly(IssuePriority.LOW);
		assertThat(f.sortField()).isEqualTo(IssueListSortField.UPDATED_AT);
		assertThat(f.sortDesc()).isTrue();
	}

	@Test
	void parse_invalidStatus() {
		assertThatThrownBy(() -> IssueListQueryParser.parse(List.of("NOT_A_STATUS"), null, null, null))
				.isInstanceOf(InvalidIssueListQueryException.class)
				.hasMessageContaining("Invalid status");
	}

	@Test
	void parse_invalidSort() {
		assertThatThrownBy(() -> IssueListQueryParser.parse(null, null, "bogus", null))
				.isInstanceOf(InvalidIssueListQueryException.class)
				.hasMessageContaining("Invalid sort");
	}

	@Test
	void parse_directionAsc() {
		IssueListFilters f = IssueListQueryParser.parse(null, null, "createdAt", "asc");
		assertThat(f.sortField()).isEqualTo(IssueListSortField.CREATED_AT);
		assertThat(f.sortDesc()).isFalse();
	}
}
