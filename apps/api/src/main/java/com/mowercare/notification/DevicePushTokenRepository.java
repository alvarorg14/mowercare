package com.mowercare.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.notification.DevicePushToken;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {

	List<DevicePushToken> findByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);

	Optional<DevicePushToken> findByOrganization_IdAndUser_IdAndToken(
			UUID organizationId, UUID userId, String token);

	void deleteByOrganization_IdAndUser_IdAndToken(UUID organizationId, UUID userId, String token);
}
