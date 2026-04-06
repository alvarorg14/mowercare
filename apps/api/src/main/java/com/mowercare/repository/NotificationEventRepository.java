package com.mowercare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.NotificationEvent;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

	List<NotificationEvent> findByIssue_IdOrderByOccurredAtAsc(UUID issueId);
}
