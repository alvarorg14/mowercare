package com.mowercare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.IssueChangeEvent;

public interface IssueChangeEventRepository extends JpaRepository<IssueChangeEvent, UUID> {

	List<IssueChangeEvent> findByIssue_IdOrderByOccurredAtAsc(UUID issueId);
}
