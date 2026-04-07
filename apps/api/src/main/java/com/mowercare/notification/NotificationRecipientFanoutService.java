package com.mowercare.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.user.AccountStatus;
import com.mowercare.notification.NotificationEvent;
import com.mowercare.notification.NotificationEventType;
import com.mowercare.notification.NotificationRecipient;
import com.mowercare.user.User;
import com.mowercare.user.UserRole;
import com.mowercare.notification.NotificationRecipientRepository;
import com.mowercare.user.UserRepository;

@Service
public class NotificationRecipientFanoutService {

	private final UserRepository userRepository;
	private final NotificationRecipientRepository notificationRecipientRepository;
	private final NotificationPushDispatcher notificationPushDispatcher;

	public NotificationRecipientFanoutService(
			UserRepository userRepository,
			NotificationRecipientRepository notificationRecipientRepository,
			NotificationPushDispatcher notificationPushDispatcher) {
		this.userRepository = userRepository;
		this.notificationRecipientRepository = notificationRecipientRepository;
		this.notificationPushDispatcher = notificationPushDispatcher;
	}

	@Transactional
	public void recordRecipientsFor(NotificationEvent event) {
		Optional<NotificationEventType> typeOpt = NotificationEventType.fromTaxonomyValue(event.getEventType());
		if (typeOpt.isEmpty()) {
			return;
		}
		UUID orgId = event.getOrganization().getId();
		List<UUID> activeAdminIds =
				userRepository
						.findByOrganization_IdAndRoleAndAccountStatus(orgId, UserRole.ADMIN, AccountStatus.ACTIVE)
						.stream()
						.map(User::getId)
						.toList();

		UUID actorId = event.getActor().getId();
		User assignee = event.getIssue().getAssignee();
		UUID assigneeCandidate =
				assignee != null && assignee.getAccountStatus() == AccountStatus.ACTIVE
						? assignee.getId()
						: null;

		LinkedHashSet<UUID> recipientIds =
				switch (typeOpt.get()) {
					case ISSUE_CREATED, ISSUE_ASSIGNED, ISSUE_STATUS_CHANGED ->
							NotificationRecipientRules.resolve(actorId, assigneeCandidate, activeAdminIds);
				};

		for (UUID recipientId : recipientIds) {
			NotificationRecipient saved =
					notificationRecipientRepository.save(
							new NotificationRecipient(
									event.getOrganization(), event, userRepository.getReferenceById(recipientId)));
			notificationPushDispatcher.dispatchForNewRecipient(saved, event);
		}
	}
}
