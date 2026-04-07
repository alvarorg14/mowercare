package com.mowercare.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailNormalizationTest {

	@Test
	@DisplayName("given null email when normalize then empty string")
	void givenNullEmail_whenNormalize_thenEmptyString() {
		assertThat(EmailNormalization.normalize(null)).isEmpty();
	}

	@Test
	@DisplayName("given spaced upper case email when normalize then trimmed lower case")
	void givenSpacedUpperCaseEmail_whenNormalize_thenTrimmedLowerCase() {
		assertThat(EmailNormalization.normalize("  Admin@ACME.TEST \t")).isEqualTo("admin@acme.test");
	}

	@Test
	@DisplayName("given empty string when normalize then empty string")
	void givenEmptyString_whenNormalize_thenEmptyString() {
		assertThat(EmailNormalization.normalize("")).isEmpty();
	}

	@Test
	@DisplayName("given only whitespace when normalize then empty string")
	void givenOnlyWhitespace_whenNormalize_thenEmptyString() {
		assertThat(EmailNormalization.normalize("   \t\n")).isEmpty();
	}
}
