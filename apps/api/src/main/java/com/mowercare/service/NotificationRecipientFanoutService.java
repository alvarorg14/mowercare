package com.mowercare.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.model.AccountStatus;
import com.mowercare.model.NotificationEvent;
import com.mowercare.model.NotificationEventType;
import com.mowercare.model.NotificationRecipient;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.repository.NotificationRecipientRepository;
import com.mowercare.repository.UserRepository;

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
