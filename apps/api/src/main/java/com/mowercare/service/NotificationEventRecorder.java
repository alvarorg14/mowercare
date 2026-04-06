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

	public NotificationEventRecorder(NotificationEventRepository notificationEventRepository) {
		this.notificationEventRepository = notificationEventRepository;
	}

	@Transactional
	public void recordIfMvp(IssueChangeEvent persistedChangeEvent) {
		NotificationEventType.fromIssueChangeType(persistedChangeEvent.getChangeType())
				.ifPresent(
						type -> notificationEventRepository.save(
								new NotificationEvent(
										persistedChangeEvent.getIssue(),
										persistedChangeEvent.getOrganization(),
										persistedChangeEvent.getActor(),
										persistedChangeEvent.getOccurredAt(),
										type,
										persistedChangeEvent)));
	}
}
