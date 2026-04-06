package com.mowercare.service;

import java.util.Map;

import com.mowercare.model.DevicePushToken;

/**
 * Sends FCM data messages for a stored device token. Implementations must not throw for transport failures;
 * invalid tokens may be removed from persistence by the implementation.
 */
public interface PushNotificationSender {

	default void sendData(DevicePushToken token, Map<String, String> data) {
		sendData(token, data, Map.of());
	}

	/**
	 * @param data FCM data payload (string map)
	 * @param correlation safe keys for logs only (e.g. recipientId) — never token strings
	 */
	void sendData(DevicePushToken token, Map<String, String> data, Map<String, String> correlation);
}
