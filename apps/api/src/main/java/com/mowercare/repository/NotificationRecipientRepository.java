package com.mowercare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.NotificationRecipient;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {

	List<NotificationRecipient> findByNotificationEvent_Id(UUID notificationEventId);
}
