package com.mowercare.notification;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mowercare.notification.DevicePushToken;
import com.mowercare.notification.DevicePushTokenRepository;

import jakarta.annotation.PostConstruct;

@Component
public class FcmPushNotificationSender implements PushNotificationSender {

	private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationSender.class);

	private final ObjectProvider<FirebaseApp> firebaseApp;
	private final DevicePushTokenRepository devicePushTokenRepository;

	public FcmPushNotificationSender(
			ObjectProvider<FirebaseApp> firebaseApp, DevicePushTokenRepository devicePushTokenRepository) {
		this.firebaseApp = firebaseApp;
		this.devicePushTokenRepository = devicePushTokenRepository;
	}

	@PostConstruct
	void logConfiguration() {
		if (firebaseApp.getIfAvailable() == null) {
			log.info(
					"FCM push disabled: Firebase not configured (set mowercare.firebase.enabled=true and credentials path)");
		}
	}

	@Override
	public void sendData(DevicePushToken token, Map<String, String> data, Map<String, String> correlation) {
		FirebaseApp app = firebaseApp.getIfAvailable();
		if (app == null) {
			return;
		}
		Message.Builder builder = Message.builder().setToken(token.getToken());
		for (Map.Entry<String, String> e : data.entrySet()) {
			builder.putData(e.getKey(), e.getValue());
		}
		Message message = builder.build();
		try {
			FirebaseMessaging.getInstance(app).send(message);
		} catch (FirebaseMessagingException ex) {
			log.warn(
					"FCM send failed: {} (recipientId={} notificationEventId={})",
					ex.getMessagingErrorCode() != null ? ex.getMessagingErrorCode().name() : ex.getMessage(),
					correlation.getOrDefault("recipientId", ""),
					correlation.getOrDefault("notificationEventId", ""));
			if (shouldDropToken(ex)) {
				devicePushTokenRepository.deleteById(token.getId());
			}
		}
	}

	/**
	 * Drop persisted row when FCM indicates the token is gone or unknown for this project (NFR-R2).
	 * {@link MessagingErrorCode#UNREGISTERED} plus {@link ErrorCode#NOT_FOUND} on the parent exception.
	 */
	private static boolean shouldDropToken(FirebaseMessagingException ex) {
		if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
			return true;
		}
		return ex.getErrorCode() == ErrorCode.NOT_FOUND;
	}
}
