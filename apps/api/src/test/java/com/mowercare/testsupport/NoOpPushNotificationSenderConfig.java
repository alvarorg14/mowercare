package com.mowercare.testsupport;

import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.mowercare.notification.DevicePushToken;
import com.mowercare.notification.PushNotificationSender;

@TestConfiguration
public class NoOpPushNotificationSenderConfig {

	@Bean
	@Primary
	PushNotificationSender noopPushNotificationSender() {
		return new PushNotificationSender() {
			@Override
			public void sendData(
					DevicePushToken token, Map<String, String> data, Map<String, String> correlation) {
				// ITs must not call Google FCM
			}
		};
	}
}
