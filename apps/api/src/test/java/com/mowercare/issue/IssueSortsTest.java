package com.mowercare.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

class IssueSortsTest {

	@Test
	@DisplayName("given updated at desc when build then updated at and id tie break desc")
	void givenUpdatedAtDesc_whenBuild_thenUpdatedAtAndIdTieBreakDesc() {
		Sort sort = IssueSorts.build(IssueListSortField.UPDATED_AT, true);
		assertThat(sort.stream().map(IssueSortsTest::propertyWithDirection))
				.containsExactly("updatedAt:DESC", "id:DESC");
	}

	@Test
	@DisplayName("given priority sort when build then case expression present in order")
	void givenPrioritySort_whenBuild_thenCaseExpressionPresentInOrder() {
		Sort sort = IssueSorts.build(IssueListSortField.PRIORITY, false);
		List<Order> orders = sort.stream().toList();
		assertThat(orders).hasSize(2);
		assertThat(orders.get(0).getProperty()).containsIgnoringCase("CASE").containsIgnoringCase("priority");
		assertThat(orders.get(1).getProperty()).isEqualTo("id");
		assertThat(orders.get(1).getDirection()).isEqualTo(Direction.DESC);
	}

	@Test
	@DisplayName("given created at ascending when build then primary sort is ascending")
	void givenCreatedAtAscending_whenBuild_thenPrimarySortIsAscending() {
		Sort sort = IssueSorts.build(IssueListSortField.CREATED_AT, false);
		assertThat(sort.stream().map(IssueSortsTest::propertyWithDirection))
				.containsExactly("createdAt:ASC", "id:DESC");
	}

	private static String propertyWithDirection(Order o) {
		return o.getProperty() + ":" + o.getDirection();
	}
}
