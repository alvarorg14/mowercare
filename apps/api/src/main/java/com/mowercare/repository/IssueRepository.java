package com.mowercare.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.Issue;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

	Optional<Issue> findByIdAndOrganization_Id(UUID id, UUID organizationId);
}
