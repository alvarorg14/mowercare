package com.mowercare.organization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.config.BootstrapProperties;
import com.mowercare.organization.BootstrapAlreadyCompletedException;
import com.mowercare.organization.InvalidBootstrapTokenException;
import com.mowercare.organization.Organization;
import com.mowercare.user.User;
import com.mowercare.user.UserRole;
import com.mowercare.organization.response.BootstrapOrganizationResponse;
import com.mowercare.organization.OrganizationRepository;
import com.mowercare.user.UserRepository;

import jakarta.persistence.EntityManager;

@Service
public class BootstrapService {

	/** Advisory lock key — single well-known id for bootstrap serialization (see Story 1.3). */
	private static final long BOOTSTRAP_ADVISORY_LOCK_KEY = 583_921_004L;

	private final BootstrapProperties bootstrapProperties;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EntityManager entityManager;

	public BootstrapService(
			BootstrapProperties bootstrapProperties,
			OrganizationRepository organizationRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			EntityManager entityManager) {
		this.bootstrapProperties = bootstrapProperties;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.entityManager = entityManager;
	}

	@Transactional(rollbackFor = Exception.class)
	public BootstrapOrganizationResponse bootstrapOrganization(
			String bootstrapToken,
			String organizationName,
			String adminEmail,
			String adminPassword) {
		validateToken(bootstrapToken);
		String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
		entityManager
				.createNativeQuery("SELECT pg_advisory_xact_lock(:k)")
				.setParameter("k", BOOTSTRAP_ADVISORY_LOCK_KEY)
				.getSingleResult();
		if (organizationRepository.count() > 0) {
			throw new BootstrapAlreadyCompletedException();
		}
		Organization org = new Organization(organizationName.trim());
		organizationRepository.save(org);
		String hash = passwordEncoder.encode(adminPassword);
		User user = new User(org, normalizedEmail, hash, UserRole.ADMIN);
		userRepository.save(user);
		return new BootstrapOrganizationResponse(org.getId(), user.getId());
	}

	private void validateToken(String headerToken) {
		String configured = bootstrapProperties.token();
		if (configured == null || configured.isBlank()) {
			throw new InvalidBootstrapTokenException();
		}
		if (headerToken == null || headerToken.isBlank()) {
			throw new InvalidBootstrapTokenException();
		}
		if (!MessageDigest.isEqual(
				configured.getBytes(StandardCharsets.UTF_8),
				headerToken.getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidBootstrapTokenException();
		}
	}
}
