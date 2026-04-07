package com.mowercare.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.notification.NotificationRecipient;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

	List<NotificationRecipient> findByNotificationEvent_Id(UUID notificationEventId);

	@EntityGraph(attributePaths = {"notificationEvent", "notificationEvent.issue"})
	Page<NotificationRecipient> findByOrganization_IdAndRecipient_Id(
			UUID organizationId, UUID recipientId, Pageable pageable);

	Optional<NotificationRecipient> findByIdAndOrganization_IdAndRecipient_Id(
			UUID id, UUID organizationId, UUID recipientId);
}
