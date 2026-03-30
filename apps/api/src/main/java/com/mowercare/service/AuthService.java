package com.mowercare.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.config.JwtProperties;
import com.mowercare.exception.InvalidCredentialsException;
import com.mowercare.exception.InvalidRefreshTokenException;
import com.mowercare.model.RefreshToken;
import com.mowercare.model.User;
import com.mowercare.model.response.TokenResponse;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;

/**
 * Employee authentication: login, refresh with rotation, logout by revoking refresh row.
 *
 * <p><strong>Refresh rotation:</strong> Each successful {@code /auth/refresh} marks the presented refresh
 * token row as revoked and issues a new opaque refresh token + new access JWT. The old refresh value cannot be reused.
 *
 * <p><strong>Logout:</strong> Revokes the presented refresh token row only (MVP — no token families).
 */
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final OpaqueTokenService opaqueTokenService;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			OpaqueTokenService opaqueTokenService,
			JwtProperties jwtProperties,
			Clock clock) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.opaqueTokenService = opaqueTokenService;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	@Transactional
	public TokenResponse login(UUID organizationId, String email, String password) {
		String normalizedEmail = normalizeEmail(email);
		User user = userRepository
				.findByOrganization_IdAndEmail(organizationId, normalizedEmail)
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		return issueTokensForUser(user);
	}

	/**
	 * Validates refresh token hash, rotates refresh (revokes old row, new opaque token), returns new pair.
	 */
	@Transactional
	public TokenResponse refresh(String rawRefreshToken) {
		String hash = opaqueTokenService.hashRawToken(rawRefreshToken);
		RefreshToken row =
				refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
		Instant now = clock.instant();
		if (!row.isActive(now)) {
			throw new InvalidRefreshTokenException();
		}
		User user = row.getUser();
		row.revoke(now);
		refreshTokenRepository.save(row);
		return issueTokensForUser(user);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		String hash = opaqueTokenService.hashRawToken(rawRefreshToken);
		RefreshToken row =
				refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
		Instant now = clock.instant();
		if (!row.isActive(now)) {
			throw new InvalidRefreshTokenException();
		}
		row.revoke(now);
		refreshTokenRepository.save(row);
	}

	private TokenResponse issueTokensForUser(User user) {
		String accessToken = jwtService.issueAccessToken(user.getId(), user.getOrganizationId(), user.getRole());
		String rawRefresh = opaqueTokenService.generateRawToken();
		String hash = opaqueTokenService.hashRawToken(rawRefresh);
		Instant now = clock.instant();
		Instant expires = now.plus(jwtProperties.refreshTokenTtl());
		RefreshToken entity = new RefreshToken(UUID.randomUUID(), user, hash, expires);
		refreshTokenRepository.save(entity);
		long expiresInSeconds = jwtProperties.accessTokenTtl().toSeconds();
		return new TokenResponse(accessToken, rawRefresh, "Bearer", expiresInSeconds);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}
}
