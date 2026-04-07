package com.mowercare.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

/**
 * Opaque refresh tokens: cryptographically random value sent to the client once; only SHA-256 hex is stored.
 */
@Service
public class OpaqueTokenService {

	private static final int RAW_BYTES = 32;
	private final SecureRandom random = new SecureRandom();

	public String generateRawToken() {
		byte[] raw = new byte[RAW_BYTES];
		random.nextBytes(raw);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
	}

	public String hashRawToken(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
