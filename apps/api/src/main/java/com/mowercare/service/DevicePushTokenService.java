package com.mowercare.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.model.DevicePushToken;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.request.DevicePushTokenPutRequest;
import com.mowercare.model.response.DevicePushTokenResponse;
import com.mowercare.repository.DevicePushTokenRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;

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
