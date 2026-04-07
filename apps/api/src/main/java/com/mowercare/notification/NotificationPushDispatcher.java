package com.mowercare.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mowercare.notification.DevicePushToken;
import com.mowercare.notification.NotificationEvent;
import com.mowercare.notification.NotificationRecipient;
import com.mowercare.notification.DevicePushTokenRepository;

/**
 * Best-effort push after in-app notification rows are created. Never throws to callers.
 */
@Service
public class NotificationPushDispatcher {

	private static final Logger log = LoggerFactory.getLogger(NotificationPushDispatcher.class);

	private final DevicePushTokenRepository devicePushTokenRepository;
	private final PushNotificationSender pushNotificationSender;

	public NotificationPushDispatcher(
			DevicePushTokenRepository devicePushTokenRepository, PushNotificationSender pushNotificationSender) {
		this.devicePushTokenRepository = devicePushTokenRepository;
		this.pushNotificationSender = pushNotificationSender;
	}

	public void dispatchForNewRecipient(NotificationRecipient recipient, NotificationEvent event) {
		try {
			UUID orgId = recipient.getOrganization().getId();
			UUID userId = recipient.getRecipient().getId();
			UUID issueId = event.getIssue().getId();

			Map<String, String> data = new HashMap<>();
			data.put("organizationId", orgId.toString());
			data.put("issueId", issueId.toString());
			data.put("notificationEventId", event.getId().toString());
			data.put("recipientId", recipient.getId().toString());

			Map<String, String> correlation =
					Map.of(
							"recipientId", recipient.getId().toString(),
							"notificationEventId", event.getId().toString());

			List<DevicePushToken> tokens = devicePushTokenRepository.findByOrganization_IdAndUser_Id(orgId, userId);
			for (DevicePushToken t : tokens) {
				try {
					pushNotificationSender.sendData(t, data, correlation);
				} catch (RuntimeException ex) {
					log.warn(
							"Unexpected runtime error sending FCM message (recipientId={} notificationEventId={})",
							correlation.get("recipientId"),
							correlation.get("notificationEventId"),
							ex);
				}
			}
		} catch (RuntimeException ex) {
			log.warn("Push dispatch failed (in-app rows already committed)", ex);
		}
	}
}
