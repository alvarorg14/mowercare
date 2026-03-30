package com.mowercare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByOrganization_IdAndEmail(UUID organizationId, String email);

	List<User> findByOrganization_IdOrderByEmailAsc(UUID organizationId);

	Optional<User> findByOrganization_IdAndId(UUID organizationId, UUID userId);

	Optional<User> findByInviteTokenHash(String inviteTokenHash);
}
