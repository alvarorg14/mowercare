package com.mowercare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.IssueChangeEvent;

public interface IssueChangeEventRepository extends JpaRepository<IssueChangeEvent, UUID> {

	List<IssueChangeEvent> findByIssue_IdOrderByOccurredAtAsc(UUID issueId);

	@EntityGraph(attributePaths = "actor")
	Page<IssueChangeEvent> findByIssue_IdAndOrganization_Id(UUID issueId, UUID organizationId, Pageable pageable);
}
