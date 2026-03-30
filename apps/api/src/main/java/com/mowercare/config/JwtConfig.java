package com.mowercare.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		byte[] secret = requireSecret(properties.secret());
		SecretKey secretKey = new SecretKeySpec(secret, "HmacSHA256");
		return NimbusJwtEncoder.withSecretKey(secretKey).build();
	}

	private static byte[] requireSecret(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("mowercare.jwt.secret (MOWERCARE_JWT_SECRET) must be set");
		}
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("mowercare.jwt.secret must be at least 32 bytes (UTF-8)");
		}
		return bytes;
	}
}
