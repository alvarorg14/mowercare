package com.mowercare.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

import jakarta.persistence.EntityManager;

@Transactional
class OrganizationMembershipPersistenceTest extends AbstractPostgresIntegrationTest {

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("Given a persisted organization and user, when the session is cleared and the user is reloaded, then organization id matches")
	void givenOrganizationAndUser_whenPersistedAndReloadedAfterClear_thenOrganizationIdMatches() {
		Organization org = organizationRepository.save(new Organization("Acme Mowers"));
		User user = userRepository.save(new User(org));

		userRepository.flush();
		entityManager.clear();

		User loaded = userRepository.findById(user.getId()).orElseThrow();
		assertThat(loaded.getOrganizationId()).isEqualTo(org.getId());
		assertThat(loaded.getOrganizationId()).isNotNull();
	}

	@Test
	@DisplayName("Given a reference to a missing organization, when a user is persisted, then flush fails with a foreign key violation")
	void givenMissingOrganizationReference_whenUserPersisted_thenFlushFailsWithForeignKeyViolation() {
		UUID missingOrgId = UUID.randomUUID();
		Organization ref = organizationRepository.getReferenceById(missingOrgId);

		assertThatThrownBy(() -> {
			userRepository.save(new User(ref));
			userRepository.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Given a shared organization, when two users are persisted, then user count is two")
	void givenSharedOrganization_whenTwoUsersPersisted_thenUserCountIsTwo() {
		Organization org = organizationRepository.save(new Organization("Shared Org"));
		userRepository.save(new User(org));
		userRepository.save(new User(org));
		userRepository.flush();
		assertThat(userRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("Given an organization, when it is saved and reloaded after clear, then name and timestamps are set")
	void givenOrganization_whenSavedAndReloadedAfterClear_thenNameAndTimestampsAreSet() {
		Organization saved = organizationRepository.save(new Organization("North Branch"));
		organizationRepository.flush();
		entityManager.clear();

		Organization loaded = organizationRepository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getName()).isEqualTo("North Branch");
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();
		assertThat(loaded.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
	}

	@Test
	@DisplayName("Given an organization, when its name is updated and reloaded, then the new name is stored")
	void givenOrganization_whenNameUpdatedAndReloaded_thenNewNameIsStored() {
		Organization org = organizationRepository.save(new Organization("Old Name"));
		org.setName("New Name");
		organizationRepository.save(org);
		organizationRepository.flush();
		entityManager.clear();

		Organization loaded = organizationRepository.findById(org.getId()).orElseThrow();
		assertThat(loaded.getName()).isEqualTo("New Name");
	}

	@Test
	@DisplayName("Given two organizations, when each has one user, then reloaded users reference the correct organization")
	void givenTwoOrganizations_whenOneUserEach_thenReloadedUsersReferenceCorrectOrg() {
		Organization orgA = organizationRepository.save(new Organization("Tenant A"));
		Organization orgB = organizationRepository.save(new Organization("Tenant B"));
		User userA = userRepository.save(new User(orgA));
		User userB = userRepository.save(new User(orgB));
		userRepository.flush();
		entityManager.clear();

		User loadedA = userRepository.findById(userA.getId()).orElseThrow();
		User loadedB = userRepository.findById(userB.getId()).orElseThrow();
		assertThat(loadedA.getOrganizationId()).isEqualTo(orgA.getId());
		assertThat(loadedB.getOrganizationId()).isEqualTo(orgB.getId());
		assertThat(organizationRepository.count()).isEqualTo(2);
		assertThat(userRepository.count()).isEqualTo(2);
	}
}
