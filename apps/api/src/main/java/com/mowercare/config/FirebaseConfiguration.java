package com.mowercare.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "mowercare.firebase", name = "enabled", havingValue = "true")
	public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
		String path = properties.credentialsPath();
		if (path == null || path.isBlank()) {
			throw new IllegalStateException("mowercare.firebase.enabled=true but credentials path is empty");
		}
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}
		try (FileInputStream in = new FileInputStream(path)) {
			GoogleCredentials credentials = GoogleCredentials.fromStream(in);
			FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
			return FirebaseApp.initializeApp(options);
		}
	}
}
