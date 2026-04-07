package com.mowercare.notification;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.notification.DevicePushToken;
import com.mowercare.organization.Organization;
import com.mowercare.user.User;
import com.mowercare.notification.request.DevicePushTokenPutRequest;
import com.mowercare.notification.response.DevicePushTokenResponse;
import com.mowercare.notification.DevicePushTokenRepository;
import com.mowercare.organization.OrganizationRepository;
import com.mowercare.user.UserRepository;

@Service
public class DevicePushTokenService {

	private final DevicePushTokenRepository devicePushTokenRepository;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;

	public DevicePushTokenService(
			DevicePushTokenRepository devicePushTokenRepository,
			OrganizationRepository organizationRepository,
			UserRepository userRepository) {
		this.devicePushTokenRepository = devicePushTokenRepository;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Upsert by unique (organization, user, token): insert or update platform and bump {@code updated_at}.
	 */
	@Transactional
	public DevicePushTokenResponse register(UUID organizationId, UUID userId, DevicePushTokenPutRequest request) {
		Organization org = organizationRepository.getReferenceById(organizationId);
		User user = userRepository.getReferenceById(userId);
		return devicePushTokenRepository
				.findByOrganization_IdAndUser_IdAndToken(organizationId, userId, request.token())
				.map(
						existing -> {
							existing.setPlatform(request.platform());
							return new DevicePushTokenResponse(devicePushTokenRepository.save(existing).getId());
						})
				.orElseGet(
						() -> {
							DevicePushToken created = new DevicePushToken(org, user, request.token(), request.platform());
							return new DevicePushTokenResponse(devicePushTokenRepository.save(created).getId());
						});
	}

	@Transactional
	public void revoke(UUID organizationId, UUID userId, String token) {
		devicePushTokenRepository.deleteByOrganization_IdAndUser_IdAndToken(organizationId, userId, token);
	}
}
