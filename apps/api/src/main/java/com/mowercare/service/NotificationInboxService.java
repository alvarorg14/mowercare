package com.mowercare.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.model.NotificationRecipient;
import com.mowercare.repository.NotificationRecipientRepository;
import com.mowercare.model.response.NotificationItemResponse;
import com.mowercare.model.response.NotificationListResponse;

@Service
public class NotificationInboxService {

	private static final int NOTIFICATIONS_MAX_PAGE = 100;

	private final NotificationRecipientRepository notificationRecipientRepository;

	public NotificationInboxService(NotificationRecipientRepository notificationRecipientRepository) {
		this.notificationRecipientRepository = notificationRecipientRepository;
	}

	/**
	 * In-app feed: recipients for this user in this org, newest first ({@code createdAt} on recipient row).
	 */
	@Transactional(readOnly = true)
	public NotificationListResponse listForUser(UUID organizationId, UUID recipientUserId, Pageable pageable) {
		int size = Math.min(Math.max(1, pageable.getPageSize()), NOTIFICATIONS_MAX_PAGE);
		int page = Math.max(0, pageable.getPageNumber());
		Pageable sorted =
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<NotificationRecipient> result =
				notificationRecipientRepository.findByOrganization_IdAndRecipient_Id(
						organizationId, recipientUserId, sorted);
		return new NotificationListResponse(
				result.getContent().stream().map(this::toItem).toList(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.getNumber(),
				result.getSize());
	}

	/**
	 * Marks one recipient row read for the current user. Idempotent.
	 */
	@Transactional
	public void markRead(UUID organizationId, UUID recipientUserId, UUID notificationRecipientId) {
		NotificationRecipient row =
				notificationRecipientRepository
						.findByIdAndOrganization_IdAndRecipient_Id(
								notificationRecipientId, organizationId, recipientUserId)
						.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
		row.markRead(Instant.now());
	}

	private NotificationItemResponse toItem(NotificationRecipient nr) {
		var ev = nr.getNotificationEvent();
		var issue = ev.getIssue();
		return new NotificationItemResponse(
				nr.getId(),
				issue.getId(),
				issue.getTitle(),
				ev.getEventType(),
				ev.getOccurredAt(),
				nr.isRead());
	}
}
