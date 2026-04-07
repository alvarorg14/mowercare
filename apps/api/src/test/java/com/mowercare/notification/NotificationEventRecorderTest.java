package com.mowercare.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mowercare.issue.Issue;
import com.mowercare.issue.IssueChangeEvent;
import com.mowercare.issue.IssueChangeType;
import com.mowercare.notification.NotificationEvent;
import com.mowercare.organization.Organization;
import com.mowercare.user.User;
import com.mowercare.notification.NotificationEventRepository;

@ExtendWith(MockitoExtension.class)
class NotificationEventRecorderTest {

	@Mock
	private NotificationEventRepository notificationEventRepository;

	@Mock
	private NotificationRecipientFanoutService notificationRecipientFanoutService;

	@InjectMocks
	private NotificationEventRecorder recorder;

	@Test
	@DisplayName("given change type not mapped when record then no save")
	void givenChangeTypeNotMapped_whenRecordIfMvp_thenNoSave() {
		IssueChangeEvent ice = Mockito.mock(IssueChangeEvent.class);
		when(ice.getChangeType()).thenReturn(IssueChangeType.TITLE_CHANGED);
		recorder.recordIfMvp(ice);
		verify(notificationEventRepository, never()).save(any());
		verify(notificationRecipientFanoutService, never()).recordRecipientsFor(any());
	}

	@Test
	@DisplayName("given created change when record then saves event and fans out")
	void givenCreatedChange_whenRecordIfMvp_thenSavesEventAndFansOut() {
		Organization organization = Mockito.mock(Organization.class);
		Issue issue = Mockito.mock(Issue.class);
		User actor = Mockito.mock(User.class);
		IssueChangeEvent ice = Mockito.mock(IssueChangeEvent.class);
		when(ice.getChangeType()).thenReturn(IssueChangeType.CREATED);
		when(ice.getIssue()).thenReturn(issue);
		when(ice.getOrganization()).thenReturn(organization);
		when(ice.getActor()).thenReturn(actor);
		when(ice.getOccurredAt()).thenReturn(Instant.parse("2026-04-07T12:00:00Z"));

		NotificationEvent saved = Mockito.mock(NotificationEvent.class);
		when(notificationEventRepository.save(any(NotificationEvent.class))).thenReturn(saved);

		recorder.recordIfMvp(ice);

		verify(notificationEventRepository).save(any(NotificationEvent.class));
		verify(notificationRecipientFanoutService).recordRecipientsFor(saved);
	}
}
