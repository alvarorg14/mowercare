package com.mowercare.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class OpaqueTokenServiceTest {

	private final OpaqueTokenService service = new OpaqueTokenService();

	@RepeatedTest(value = 3, name = "{displayName} — repetition {currentRepetition}/{totalRepetitions}")
	@DisplayName("given fresh service when generate raw token then url safe and unique")
	void givenFreshService_whenGenerateRawToken_thenUrlSafeAndUnique() {
		String a = service.generateRawToken();
		String b = service.generateRawToken();
		assertThat(a).isNotEqualTo(b);
		assertThat(a).doesNotContain("+", "/");
	}

	@Test
	@DisplayName("given same raw token when hash then deterministic hex")
	void givenSameRawToken_whenHash_thenDeterministicHex() {
		String raw = "test-raw-token-value";
		assertThat(service.hashRawToken(raw)).isEqualTo(service.hashRawToken(raw)).hasSize(64);
	}
}
