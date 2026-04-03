package com.mowercare.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import com.mowercare.model.Issue;
import com.mowercare.model.IssueStatus;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

	@EntityGraph(attributePaths = {"assignee"})
	Optional<Issue> findByIdAndOrganization_Id(UUID id, UUID organizationId);

	@EntityGraph(attributePaths = {"assignee"})
	Page<Issue> findByOrganization_IdAndStatusIn(
			UUID organizationId, Collection<IssueStatus> statuses, Pageable pageable);

	@EntityGraph(attributePaths = {"assignee"})
	Page<Issue> findByOrganization_Id(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"assignee"})
	Page<Issue> findByOrganization_IdAndAssignee_Id(UUID organizationId, UUID assigneeId, Pageable pageable);

	@EntityGraph(attributePaths = {"assignee"})
	@Override
	Page<Issue> findAll(@Nullable Specification<Issue> spec, Pageable pageable);
}
