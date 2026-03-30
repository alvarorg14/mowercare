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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_users_organization_id_email",
				columnNames = { "organization_id", "email" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false, length = 32)
	private AccountStatus accountStatus;

	@Column(name = "invite_token_hash", length = 64)
	private String inviteTokenHash;

	@Column(name = "invite_expires_at")
	private Instant inviteExpiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public User(Organization organization, String email, String passwordHash, UserRole role) {
		this(organization, email, passwordHash, role, AccountStatus.ACTIVE, null, null);
	}

	public User(
			Organization organization,
			String email,
			String passwordHash,
			UserRole role,
			AccountStatus accountStatus,
			String inviteTokenHash,
			Instant inviteExpiresAt) {
		this.organization = organization;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.accountStatus = accountStatus;
		this.inviteTokenHash = inviteTokenHash;
		this.inviteExpiresAt = inviteExpiresAt;
	}

	/**
	 * Completes invite acceptance: sets password and clears invite fields.
	 */
	public void activateFromInvite(String newPasswordHash) {
		if (accountStatus != AccountStatus.PENDING_INVITE) {
			throw new IllegalStateException("Account is not pending invite");
		}
		this.passwordHash = newPasswordHash;
		this.accountStatus = AccountStatus.ACTIVE;
		this.inviteTokenHash = null;
		this.inviteExpiresAt = null;
	}

	/**
	 * Stable tenant key for authorization (avoids relying on callers to navigate the association).
	 */
	public UUID getOrganizationId() {
		return organization.getId();
	}
}
