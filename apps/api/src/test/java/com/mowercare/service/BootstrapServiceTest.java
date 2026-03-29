package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.mowercare.config.BootstrapProperties;
import com.mowercare.exception.BootstrapAlreadyCompletedException;
import com.mowercare.exception.InvalidBootstrapTokenException;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.model.response.BootstrapOrganizationResponse;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BootstrapServiceTest {

	private static final String TOKEN = "bootstrap-shared-secret";

	@Mock
	private BootstrapProperties bootstrapProperties;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EntityManager entityManager;

	@Mock
	private Query advisoryLockQuery;

	private BootstrapService bootstrapService;

	@BeforeEach
	void setUp() {
		when(bootstrapProperties.token()).thenReturn(TOKEN);
		when(entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:k)")).thenReturn(advisoryLockQuery);
		when(advisoryLockQuery.setParameter(eq("k"), eq(583_921_004L))).thenReturn(advisoryLockQuery);
		when(advisoryLockQuery.getSingleResult()).thenReturn(null);
		bootstrapService = new BootstrapService(
				bootstrapProperties,
				organizationRepository,
				userRepository,
				passwordEncoder,
				entityManager);
	}

	@Test
	@DisplayName("given configured token and valid header when bootstrap on empty database then creates admin user with normalized email and bcrypt hash")
	void givenValidTokenAndEmptyDb_whenBootstrap_thenCreatesOrgAndAdmin() {
		when(organizationRepository.count()).thenReturn(0L);
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> {
			Organization o = inv.getArgument(0);
			ReflectionTestUtils.setField(o, "id", orgId);
			return o;
		});
		when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			ReflectionTestUtils.setField(u, "id", userId);
			return u;
		});

		BootstrapOrganizationResponse response = bootstrapService.bootstrapOrganization(
				TOKEN,
				"  Acme  ",
				"  Admin@ACME.TEST  ",
				"password123");

		assertThat(response.organizationId()).isEqualTo(orgId);
		assertThat(response.userId()).isEqualTo(userId);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@acme.test");
		assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
	}

	@Test
	@DisplayName("given blank configured token when bootstrap then throws InvalidBootstrapTokenException")
	void givenBlankConfiguredToken_whenBootstrap_thenThrowsUnauthorized() {
		when(bootstrapProperties.token()).thenReturn(" ");

		assertThatThrownBy(() -> bootstrapService.bootstrapOrganization(TOKEN, "O", "a@b.co", "password123"))
				.isInstanceOf(InvalidBootstrapTokenException.class);
		verify(organizationRepository, never()).save(any());
	}

	@Test
	@DisplayName("given missing header token when bootstrap then throws InvalidBootstrapTokenException")
	void givenMissingHeaderToken_whenBootstrap_thenThrowsUnauthorized() {
		assertThatThrownBy(() -> bootstrapService.bootstrapOrganization(null, "O", "a@b.co", "password123"))
				.isInstanceOf(InvalidBootstrapTokenException.class);
		verify(organizationRepository, never()).save(any());
	}

	@Test
	@DisplayName("given wrong header token when bootstrap then throws InvalidBootstrapTokenException")
	void givenWrongHeaderToken_whenBootstrap_thenThrowsUnauthorized() {
		assertThatThrownBy(
				() -> bootstrapService.bootstrapOrganization("not-the-token", "O", "a@b.co", "password123"))
				.isInstanceOf(InvalidBootstrapTokenException.class);
		verify(organizationRepository, never()).save(any());
	}

	@Test
	@DisplayName("given organization already exists when bootstrap then throws BootstrapAlreadyCompletedException")
	void givenOrganizationExists_whenBootstrap_thenThrowsConflict() {
		when(organizationRepository.count()).thenReturn(1L);

		assertThatThrownBy(() -> bootstrapService.bootstrapOrganization(TOKEN, "O", "a@b.co", "password123"))
				.isInstanceOf(BootstrapAlreadyCompletedException.class);
		verify(organizationRepository, never()).save(any());
	}
}
