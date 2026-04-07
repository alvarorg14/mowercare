package com.mowercare.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mowercare.model.AccountStatus;
import com.mowercare.model.Issue;
import com.mowercare.model.NotificationEvent;
import com.mowercare.model.NotificationRecipient;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.repository.NotificationRecipientRepository;
import com.mowercare.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientFanoutServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationRecipientRepository notificationRecipientRepository;

	@Mock
	private NotificationPushDispatcher notificationPushDispatcher;

	@InjectMocks
	private NotificationRecipientFanoutService fanoutService;

	private final UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private final UUID actorId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private final UUID adminId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@Test
	@DisplayName("given unknown event type when record then no recipients")
	void givenUnknownEventType_whenRecordRecipientsFor_thenNoRecipients() {
		NotificationEvent event = Mockito.mock(NotificationEvent.class);
		when(event.getEventType()).thenReturn("unknown.type");
		fanoutService.recordRecipientsFor(event);
		verify(notificationRecipientRepository, never()).save(any());
		verify(notificationPushDispatcher, never()).dispatchForNewRecipient(any(), any());
	}

	@Test
	@DisplayName("given issue created when record then saves recipient and dispatches")
	void givenIssueCreated_whenRecordRecipientsFor_thenSavesRecipientAndDispatches() {
		Organization organization = Mockito.mock(Organization.class);
		when(organization.getId()).thenReturn(orgId);
		User actor = Mockito.mock(User.class);
		when(actor.getId()).thenReturn(actorId);
		Issue issue = Mockito.mock(Issue.class);
		when(issue.getAssignee()).thenReturn(null);
		NotificationEvent event = Mockito.mock(NotificationEvent.class);
		when(event.getEventType()).thenReturn("issue.created");
		when(event.getOrganization()).thenReturn(organization);
		when(event.getActor()).thenReturn(actor);
		when(event.getIssue()).thenReturn(issue);

		User admin = Mockito.mock(User.class);
		when(admin.getId()).thenReturn(adminId);
		when(userRepository.findByOrganization_IdAndRoleAndAccountStatus(orgId, UserRole.ADMIN, AccountStatus.ACTIVE))
				.thenReturn(List.of(admin));

		NotificationRecipient saved = Mockito.mock(NotificationRecipient.class);
		when(notificationRecipientRepository.save(any(NotificationRecipient.class))).thenReturn(saved);

		fanoutService.recordRecipientsFor(event);

		verify(notificationRecipientRepository).save(any(NotificationRecipient.class));
		verify(notificationPushDispatcher).dispatchForNewRecipient(saved, event);
	}
}
