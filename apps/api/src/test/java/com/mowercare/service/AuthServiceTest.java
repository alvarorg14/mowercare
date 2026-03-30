package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mowercare.config.JwtProperties;
import com.mowercare.exception.InvalidCredentialsException;
import com.mowercare.exception.InvalidRefreshTokenException;
import com.mowercare.model.AccountStatus;
import com.mowercare.model.RefreshToken;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.model.response.TokenResponse;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private OpaqueTokenService opaqueTokenService;

	@Mock
	private User user;

	private JwtProperties jwtProperties;

	private AuthService authService;

	private final UUID orgId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		jwtProperties = new JwtProperties(
				"https://test.local",
				"test-jwt-secret-must-be-at-least-32-bytes-long!",
				java.time.Duration.ofMinutes(15),
				java.time.Duration.ofDays(7));
		Clock fixed = Clock.fixed(Instant.parse("2026-03-30T12:00:00Z"), ZoneOffset.UTC);
		authService = new AuthService(
				userRepository,
				refreshTokenRepository,
				passwordEncoder,
				jwtService,
				opaqueTokenService,
				jwtProperties,
				fixed);
		when(user.getId()).thenReturn(userId);
		when(user.getOrganizationId()).thenReturn(orgId);
		when(user.getRole()).thenReturn(UserRole.ADMIN);
		when(user.getPasswordHash()).thenReturn("hash");
		when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
	}

	@Test
	@DisplayName("given valid user when login then returns tokens and saves refresh row")
	void givenValidUser_whenLogin_thenReturnsTokens() {
		when(userRepository.findByOrganization_IdAndEmail(orgId, "admin@test.local")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret12345", "hash")).thenReturn(true);
		when(jwtService.issueAccessToken(userId, orgId, UserRole.ADMIN)).thenReturn("access-jwt");
		when(opaqueTokenService.generateRawToken()).thenReturn("raw-refresh");
		when(opaqueTokenService.hashRawToken("raw-refresh")).thenReturn("hash-refresh");
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

		TokenResponse response = authService.login(orgId, "  Admin@Test.LOCAL  ", "secret12345");

		assertThat(response.accessToken()).isEqualTo("access-jwt");
		assertThat(response.refreshToken()).isEqualTo("raw-refresh");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(900L);
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("given unknown user when login then throws InvalidCredentialsException")
	void givenUnknownUser_whenLogin_thenThrows() {
		when(userRepository.findByOrganization_IdAndEmail(orgId, "a@test.local")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(orgId, "a@test.local", "x"))
				.isInstanceOf(InvalidCredentialsException.class);
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	@DisplayName("given pending invite when login then throws InvalidCredentialsException")
	void givenPendingInvite_whenLogin_thenThrows() {
		when(userRepository.findByOrganization_IdAndEmail(orgId, "admin@test.local")).thenReturn(Optional.of(user));
		when(user.getAccountStatus()).thenReturn(AccountStatus.PENDING_INVITE);

		assertThatThrownBy(() -> authService.login(orgId, "admin@test.local", "secret12345"))
				.isInstanceOf(InvalidCredentialsException.class);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	@DisplayName("given deactivated account when login then throws InvalidCredentialsException")
	void givenDeactivated_whenLogin_thenThrows() {
		when(userRepository.findByOrganization_IdAndEmail(orgId, "admin@test.local")).thenReturn(Optional.of(user));
		when(user.getAccountStatus()).thenReturn(AccountStatus.DEACTIVATED);

		assertThatThrownBy(() -> authService.login(orgId, "admin@test.local", "secret12345"))
				.isInstanceOf(InvalidCredentialsException.class);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	@DisplayName("given wrong password when login then throws InvalidCredentialsException")
	void givenWrongPassword_whenLogin_thenThrows() {
		when(userRepository.findByOrganization_IdAndEmail(orgId, "admin@test.local")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(orgId, "admin@test.local", "bad"))
				.isInstanceOf(InvalidCredentialsException.class);
		verify(refreshTokenRepository, never()).save(any());
	}

	@Test
	@DisplayName("given valid refresh row when refresh then revokes old and issues new pair")
	void givenValidRefresh_whenRefresh_thenRotates() {
		RefreshToken row = new RefreshToken(UUID.randomUUID(), user, "stored-hash", Instant.parse("2026-04-30T12:00:00Z"));
		when(opaqueTokenService.hashRawToken("incoming")).thenReturn("stored-hash");
		when(refreshTokenRepository.findByTokenHash("stored-hash")).thenReturn(Optional.of(row));
		when(jwtService.issueAccessToken(userId, orgId, UserRole.ADMIN)).thenReturn("new-access");
		when(opaqueTokenService.generateRawToken()).thenReturn("new-raw");
		when(opaqueTokenService.hashRawToken("new-raw")).thenReturn("new-hash");
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

		TokenResponse response = authService.refresh("incoming");

		assertThat(response.accessToken()).isEqualTo("new-access");
		assertThat(response.refreshToken()).isEqualTo("new-raw");
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository, Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).hasSize(2);
		assertThat(row.getRevokedAt()).isNotNull();
	}

	@Test
	@DisplayName("given refresh row for deactivated user when refresh then throws InvalidRefreshTokenException")
	void givenDeactivatedUser_whenRefresh_thenThrows() {
		RefreshToken row = new RefreshToken(UUID.randomUUID(), user, "stored-hash", Instant.parse("2026-04-30T12:00:00Z"));
		when(opaqueTokenService.hashRawToken("incoming")).thenReturn("stored-hash");
		when(refreshTokenRepository.findByTokenHash("stored-hash")).thenReturn(Optional.of(row));
		when(user.getAccountStatus()).thenReturn(AccountStatus.DEACTIVATED);

		assertThatThrownBy(() -> authService.refresh("incoming")).isInstanceOf(InvalidRefreshTokenException.class);
		verify(refreshTokenRepository, never()).save(any());
	}
}
