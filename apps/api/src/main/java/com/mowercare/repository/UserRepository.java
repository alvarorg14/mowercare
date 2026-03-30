package com.mowercare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mowercare.model.User;
import com.mowercare.model.UserRole;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByOrganization_IdAndEmail(UUID organizationId, String email);

	List<User> findByOrganization_IdOrderByEmailAsc(UUID organizationId);

	Optional<User> findByOrganization_IdAndId(UUID organizationId, UUID userId);

	Optional<User> findByInviteTokenHash(String inviteTokenHash);

	long countByOrganization_IdAndRole(UUID organizationId, UserRole role);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM User u WHERE u.organization.id = :orgId AND u.role = :role")
	List<User> lockByOrganizationIdAndRole(@Param("orgId") UUID orgId, @Param("role") UserRole role);
}
