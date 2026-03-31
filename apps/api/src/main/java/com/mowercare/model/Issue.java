package com.mowercare.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(nullable = false, length = 500)
	@Setter
	private String title;

	@Column(columnDefinition = "text")
	@Setter
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	@Setter
	private IssueStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	@Setter
	private IssuePriority priority;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_user_id")
	@Setter
	private User assignee;

	@Column(name = "customer_label", length = 500)
	@Setter
	private String customerLabel;

	@Column(name = "site_label", length = 500)
	@Setter
	private String siteLabel;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Issue(
			Organization organization,
			String title,
			String description,
			IssueStatus status,
			IssuePriority priority,
			User assignee,
			String customerLabel,
			String siteLabel) {
		this.organization = organization;
		this.title = title;
		this.description = description;
		this.status = status;
		this.priority = priority;
		this.assignee = assignee;
		this.customerLabel = customerLabel;
		this.siteLabel = siteLabel;
	}

	public UUID getOrganizationId() {
		return organization.getId();
	}
}
