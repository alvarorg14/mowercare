package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mowercare.model.DevicePushPlatform;
import com.mowercare.model.DevicePushToken;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.request.DevicePushTokenPutRequest;
import com.mowercare.repository.DevicePushTokenRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DevicePushTokenServiceTest {

	@Mock
	private DevicePushTokenRepository devicePushTokenRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private DevicePushTokenService devicePushTokenService;

	private final UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private final UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Test
	@DisplayName("given no existing token when register then insert new")
	void givenNoExistingToken_whenRegister_thenInsertNew() {
		when(organizationRepository.getReferenceById(orgId)).thenReturn(Mockito.mock(Organization.class));
		when(userRepository.getReferenceById(userId)).thenReturn(Mockito.mock(User.class));
		when(devicePushTokenRepository.findByOrganization_IdAndUser_IdAndToken(orgId, userId, "tok-1"))
				.thenReturn(Optional.empty());
		DevicePushToken saved = Mockito.mock(DevicePushToken.class);
		when(saved.getId()).thenReturn(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
		when(devicePushTokenRepository.save(any(DevicePushToken.class))).thenReturn(saved);

		var req = new DevicePushTokenPutRequest("tok-1", DevicePushPlatform.IOS);
		var res = devicePushTokenService.register(orgId, userId, req);
		assertThat(res.id()).isEqualTo(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
	}

	@Test
	@DisplayName("given existing token when register then updates platform and returns same id")
	void givenExistingToken_whenRegister_thenUpdatesPlatformAndReturnsSameId() {
		when(organizationRepository.getReferenceById(orgId)).thenReturn(Mockito.mock(Organization.class));
		when(userRepository.getReferenceById(userId)).thenReturn(Mockito.mock(User.class));
		DevicePushToken existing = Mockito.mock(DevicePushToken.class);
		when(devicePushTokenRepository.findByOrganization_IdAndUser_IdAndToken(orgId, userId, "tok-1"))
				.thenReturn(Optional.of(existing));
		when(devicePushTokenRepository.save(existing)).thenReturn(existing);
		when(existing.getId()).thenReturn(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));

		var req = new DevicePushTokenPutRequest("tok-1", DevicePushPlatform.ANDROID);
		var res = devicePushTokenService.register(orgId, userId, req);

		verify(existing).setPlatform(DevicePushPlatform.ANDROID);
		assertThat(res.id()).isEqualTo(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
		verify(devicePushTokenRepository).save(existing);
	}

	@Test
	@DisplayName("given revoke when delete then delegates to repository")
	void givenRevoke_whenRevoke_thenDelegatesToRepository() {
		devicePushTokenService.revoke(orgId, userId, "tok-x");
		verify(devicePushTokenRepository).deleteByOrganization_IdAndUser_IdAndToken(orgId, userId, "tok-x");
	}
}
