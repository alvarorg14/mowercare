package com.mowercare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByOrganization_IdAndEmail(UUID organizationId, String email);
}
