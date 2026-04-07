package com.mowercare.notification;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mowercare.issue.Issue;
import com.mowercare.issue.IssueChangeEvent;
import com.mowercare.organization.Organization;
import com.mowercare.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id", nullable = false)
	private Issue issue;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id", nullable = false)
	private User actor;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_issue_change_event_id")
	private IssueChangeEvent sourceIssueChangeEvent;

	public NotificationEvent(
			Issue issue,
			Organization organization,
			User actor,
			Instant occurredAt,
			NotificationEventType notificationType,
			IssueChangeEvent sourceIssueChangeEvent) {
		this.issue = issue;
		this.organization = organization;
		this.actor = actor;
		this.occurredAt = occurredAt;
		this.eventType = notificationType.taxonomyValue();
		this.sourceIssueChangeEvent = sourceIssueChangeEvent;
	}
}
