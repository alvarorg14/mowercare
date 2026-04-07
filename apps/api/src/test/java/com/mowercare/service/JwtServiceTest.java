package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.mowercare.config.JwtProperties;
import com.mowercare.model.UserRole;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

	@Mock
	private JwtEncoder jwtEncoder;

	private JwtService jwtService;

	private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private final UUID orgId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@BeforeEach
	void setUp() {
		JwtProperties props =
				new JwtProperties(
						"https://issuer.test",
						"test-jwt-secret-must-be-at-least-32-bytes-long!",
						Duration.ofMinutes(15),
						Duration.ofDays(7));
		Clock clock = Clock.fixed(Instant.parse("2026-04-07T12:00:00Z"), ZoneOffset.UTC);
		jwtService = new JwtService(jwtEncoder, props, clock);
	}

	@Test
	@DisplayName("given encoder returns jwt when issue access token then returns token value string")
	void givenEncoderReturnsJwt_whenIssueAccessToken_thenReturnsTokenValueString() {
		Jwt encoded =
				Jwt.withTokenValue("header.payload.sig")
						.header("alg", "HS256")
						.subject(userId.toString())
						.issuedAt(Instant.parse("2026-04-07T12:00:00Z"))
						.build();
		when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encoded);
		String out = jwtService.issueAccessToken(userId, orgId, UserRole.ADMIN);
		assertThat(out).isEqualTo("header.payload.sig");
	}

	@Test
	@DisplayName("given issue access token when encode then claims include subject org and role")
	void givenIssueAccessToken_whenEncode_thenClaimsIncludeSubjectOrgAndRole() {
		Jwt encoded =
				Jwt.withTokenValue("x")
						.header("alg", "HS256")
						.subject(userId.toString())
						.issuedAt(Instant.parse("2026-04-07T12:00:00Z"))
						.build();
		ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
		when(jwtEncoder.encode(captor.capture())).thenReturn(encoded);
		jwtService.issueAccessToken(userId, orgId, UserRole.TECHNICIAN);
		var claims = captor.getValue().getClaims();
		assertThat(claims.getSubject()).isEqualTo(userId.toString());
		assertThat(claims.getClaimAsString("organizationId")).isEqualTo(orgId.toString());
		assertThat(claims.getClaimAsString("role")).isEqualTo("TECHNICIAN");
		assertThat(claims.getIssuer().toString()).isEqualTo("https://issuer.test");
	}
}
