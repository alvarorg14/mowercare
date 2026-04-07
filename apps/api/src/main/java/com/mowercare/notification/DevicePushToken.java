package com.mowercare.notification;

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

import com.mowercare.organization.Organization;
import com.mowercare.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "device_push_tokens",
		uniqueConstraints =
				@UniqueConstraint(
						name = "uq_device_push_tokens_org_user_token",
						columnNames = { "organization_id", "user_id", "token" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevicePushToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, columnDefinition = "text")
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	@Setter
	private DevicePushPlatform platform;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public DevicePushToken(Organization organization, User user, String token, DevicePushPlatform platform) {
		this.organization = organization;
		this.user = user;
		this.token = token;
		this.platform = platform;
	}
}
