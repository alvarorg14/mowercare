package com.mowercare.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.mowercare.organization.Organization;
import com.mowercare.user.User;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "notification_recipients",
		uniqueConstraints =
				@UniqueConstraint(
						name = "uq_notification_recipients_event_user",
						columnNames = { "notification_event_id", "recipient_user_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRecipient {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "notification_event_id", nullable = false)
	private NotificationEvent notificationEvent;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_user_id", nullable = false)
	private User recipient;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/** When non-null, the recipient has seen this notification in-app (Story 4.3). */
	@Column(name = "read_at")
	@Setter
	private Instant readAt;

	public NotificationRecipient(
			Organization organization, NotificationEvent notificationEvent, User recipient) {
		this.organization = organization;
		this.notificationEvent = notificationEvent;
		this.recipient = recipient;
	}

	public boolean isRead() {
		return readAt != null;
	}

	/**
	 * Marks as read if not already (idempotent).
	 */
	public void markRead(Instant at) {
		if (this.readAt == null) {
			this.readAt = at;
		}
	}
}
