package com.mowercare.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.model.IssueChangeEvent;
import com.mowercare.model.NotificationEvent;
import com.mowercare.model.NotificationEventType;
import com.mowercare.repository.NotificationEventRepository;

/**
 * Persists MVP notification domain rows when {@link com.mowercare.model.IssueChangeType} maps to the taxonomy.
 */
@Service
public class NotificationEventRecorder {

	private final NotificationEventRepository notificationEventRepository;
	private final NotificationRecipientFanoutService notificationRecipientFanoutService;

	public NotificationEventRecorder(
			NotificationEventRepository notificationEventRepository,
			NotificationRecipientFanoutService notificationRecipientFanoutService) {
		this.notificationEventRepository = notificationEventRepository;
		this.notificationRecipientFanoutService = notificationRecipientFanoutService;
	}

	@Transactional
	public void recordIfMvp(IssueChangeEvent persistedChangeEvent) {
		NotificationEventType.fromIssueChangeType(persistedChangeEvent.getChangeType())
				.ifPresent(
						type -> {
							NotificationEvent saved =
									notificationEventRepository.save(
											new NotificationEvent(
													persistedChangeEvent.getIssue(),
													persistedChangeEvent.getOrganization(),
													persistedChangeEvent.getActor(),
													persistedChangeEvent.getOccurredAt(),
													type,
													persistedChangeEvent));
							notificationRecipientFanoutService.recordRecipientsFor(saved);
						});
	}
}
