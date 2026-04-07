package com.mowercare.issue;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mowercare.organization.Organization;
import com.mowercare.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issue_change_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueChangeEvent {

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

	@Enumerated(EnumType.STRING)
	@Column(name = "change_type", nullable = false, length = 64)
	private IssueChangeType changeType;

	@Column(name = "old_value", columnDefinition = "text")
	private String oldValue;

	@Column(name = "new_value", columnDefinition = "text")
	private String newValue;

	public IssueChangeEvent(
			Issue issue,
			Organization organization,
			User actor,
			Instant occurredAt,
			IssueChangeType changeType,
			String oldValue,
			String newValue) {
		this.issue = issue;
		this.organization = organization;
		this.actor = actor;
		this.occurredAt = occurredAt;
		this.changeType = changeType;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
}
