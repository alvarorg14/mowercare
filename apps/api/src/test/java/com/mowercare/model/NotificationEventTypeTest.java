package com.mowercare.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class NotificationEventTypeTest {

	@Test
	void fromIssueChangeType_mapsMvpTypes() {
		assertThat(NotificationEventType.fromIssueChangeType(IssueChangeType.CREATED))
				.contains(NotificationEventType.ISSUE_CREATED);
		assertThat(NotificationEventType.fromIssueChangeType(IssueChangeType.ASSIGNEE_CHANGED))
				.contains(NotificationEventType.ISSUE_ASSIGNED);
		assertThat(NotificationEventType.fromIssueChangeType(IssueChangeType.STATUS_CHANGED))
				.contains(NotificationEventType.ISSUE_STATUS_CHANGED);
	}

	@Test
	void fromIssueChangeType_emptyForNonMvpHistoryTypes() {
		assertThat(NotificationEventType.fromIssueChangeType(IssueChangeType.TITLE_CHANGED)).isEqualTo(Optional.empty());
		assertThat(NotificationEventType.fromIssueChangeType(IssueChangeType.PRIORITY_CHANGED)).isEqualTo(Optional.empty());
	}

	@Test
	void taxonomyValues_matchArchitectureDotSeparatedStrings() {
		assertThat(NotificationEventType.ISSUE_CREATED.taxonomyValue()).isEqualTo("issue.created");
		assertThat(NotificationEventType.ISSUE_ASSIGNED.taxonomyValue()).isEqualTo("issue.assigned");
		assertThat(NotificationEventType.ISSUE_STATUS_CHANGED.taxonomyValue()).isEqualTo("issue.status_changed");
	}

	@Test
	void fromTaxonomyValue_roundTripsPersistedStrings() {
		assertThat(NotificationEventType.fromTaxonomyValue("issue.created")).contains(NotificationEventType.ISSUE_CREATED);
		assertThat(NotificationEventType.fromTaxonomyValue("issue.assigned")).contains(NotificationEventType.ISSUE_ASSIGNED);
		assertThat(NotificationEventType.fromTaxonomyValue("issue.status_changed"))
				.contains(NotificationEventType.ISSUE_STATUS_CHANGED);
		assertThat(NotificationEventType.fromTaxonomyValue("unknown")).isEmpty();
	}
}
