package com.mowercare.notification;

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

import com.mowercare.notification.DevicePushToken;
import com.mowercare.issue.Issue;
import com.mowercare.notification.NotificationEvent;
import com.mowercare.notification.NotificationRecipient;
import com.mowercare.organization.Organization;
import com.mowercare.user.User;
import com.mowercare.notification.DevicePushTokenRepository;

@ExtendWith(MockitoExtension.class)
class NotificationPushDispatcherTest {

	@Mock
	private DevicePushTokenRepository devicePushTokenRepository;

	@Mock
	private PushNotificationSender pushNotificationSender;

	@InjectMocks
	private NotificationPushDispatcher dispatcher;

	@Test
	@DisplayName("given tokens when dispatch then sends data for each token")
	void givenTokens_whenDispatchForNewRecipient_thenSendsDataForEachToken() {
		UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
		UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		UUID issueId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
		UUID eventId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
		UUID recipientRowId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

		Organization organization = Mockito.mock(Organization.class);
		when(organization.getId()).thenReturn(orgId);
		User recipientUser = Mockito.mock(User.class);
		when(recipientUser.getId()).thenReturn(userId);
		Issue issue = Mockito.mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		NotificationEvent event = Mockito.mock(NotificationEvent.class);
		when(event.getId()).thenReturn(eventId);
		when(event.getIssue()).thenReturn(issue);

		NotificationRecipient nr = Mockito.mock(NotificationRecipient.class);
		when(nr.getId()).thenReturn(recipientRowId);
		when(nr.getOrganization()).thenReturn(organization);
		when(nr.getRecipient()).thenReturn(recipientUser);

		DevicePushToken token = Mockito.mock(DevicePushToken.class);
		when(devicePushTokenRepository.findByOrganization_IdAndUser_Id(orgId, userId)).thenReturn(List.of(token));

		dispatcher.dispatchForNewRecipient(nr, event);

		verify(pushNotificationSender).sendData(Mockito.eq(token), any(), any());
	}

	@Test
	@DisplayName("given no device tokens when dispatch then does not send")
	void givenNoDeviceTokens_whenDispatchForNewRecipient_thenDoesNotSend() {
		UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
		UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		UUID issueId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
		UUID eventId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
		UUID recipientRowId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

		Organization organization = Mockito.mock(Organization.class);
		when(organization.getId()).thenReturn(orgId);
		User recipientUser = Mockito.mock(User.class);
		when(recipientUser.getId()).thenReturn(userId);
		Issue issue = Mockito.mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		NotificationEvent event = Mockito.mock(NotificationEvent.class);
		when(event.getId()).thenReturn(eventId);
		when(event.getIssue()).thenReturn(issue);

		NotificationRecipient nr = Mockito.mock(NotificationRecipient.class);
		when(nr.getId()).thenReturn(recipientRowId);
		when(nr.getOrganization()).thenReturn(organization);
		when(nr.getRecipient()).thenReturn(recipientUser);

		when(devicePushTokenRepository.findByOrganization_IdAndUser_Id(orgId, userId)).thenReturn(List.of());

		dispatcher.dispatchForNewRecipient(nr, event);

		verify(pushNotificationSender, never()).sendData(any(), any(), any());
	}
}
