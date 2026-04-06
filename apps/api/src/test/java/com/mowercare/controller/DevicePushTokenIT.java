package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.repository.DevicePushTokenRepository;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.NotificationEventRepository;
import com.mowercare.repository.NotificationRecipientRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;
import com.mowercare.testsupport.NoOpPushNotificationSenderConfig;

@Import(NoOpPushNotificationSenderConfig.class)
class DevicePushTokenIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String BOOTSTRAP_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

	private static final UUID FOREIGN_ORG = UUID.fromString("00000000-0000-0000-0000-000000000042");

	private static final String FCM_TOKEN =
			"fake-fcm-token-123456789012345678901234567890123456789012345678901234567890";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private IssueChangeEventRepository issueChangeEventRepository;

	@Autowired
	private IssueRepository issueRepository;

	@Autowired
	private NotificationEventRepository notificationEventRepository;

	@Autowired
	private NotificationRecipientRepository notificationRecipientRepository;

	@Autowired
	private DevicePushTokenRepository devicePushTokenRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	void setUp() {
		notificationRecipientRepository.deleteAll();
		notificationEventRepository.deleteAll();
		issueChangeEventRepository.deleteAll();
		issueRepository.deleteAll();
		devicePushTokenRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
		organizationRepository.deleteAll();
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	@DynamicPropertySource
	static void bootstrapToken(DynamicPropertyRegistry registry) {
		registry.add("mowercare.bootstrap.token", () -> BOOTSTRAP_TOKEN);
	}

	@Test
	@DisplayName("given org when PUT device token then 200 and idempotent upsert")
	void givenOrg_whenPutDeviceToken_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		String body =
				"{\"token\":\"" + FCM_TOKEN + "\",\"platform\":\"ANDROID\"}";

		String id1 =
				mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
								.header("Authorization", "Bearer " + access)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.id").exists())
						.andReturn()
						.getResponse()
						.getContentAsString();
		JsonNode idJson = objectMapper.readTree(id1);
		String id = idJson.get("id").asText();

		mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));
	}

	@Test
	@DisplayName("given token when DELETE then revoked and PUT creates new id")
	void givenToken_whenDelete_thenRevoked() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		String body =
				"{\"token\":\"" + FCM_TOKEN + "\",\"platform\":\"IOS\"}";

		String id1 =
				mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
								.header("Authorization", "Bearer " + access)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString();
		String firstId = objectMapper.readTree(id1).get("id").asText();

		mockMvc.perform(delete("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + FCM_TOKEN + "\"}"))
				.andExpect(status().isNoContent());

		String id2 =
				mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
								.header("Authorization", "Bearer " + access)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString();
		String secondId = objectMapper.readTree(id2).get("id").asText();
		org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
	}

	@Test
	@DisplayName("given org when PUT device token wrong org path then 403")
	void givenOrg_whenPutDeviceTokenWrongOrg_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + FCM_TOKEN + "\",\"platform\":\"UNKNOWN\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given org when PUT invalid token body then 400")
	void givenOrg_whenPutInvalidBody_thenBadRequest() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(put("/api/v1/organizations/{organizationId}/device-push-tokens", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"\",\"platform\":\"ANDROID\"}"))
				.andExpect(status().isBadRequest());
	}

	private String bootstrapAndGetOrganizationId() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", BOOTSTRAP_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOTSTRAP_BODY))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
		return json.get("organizationId").asText();
	}

	private String loginAccessToken(String organizationId, String email, String password) throws Exception {
		String body = String.format(
				"{\"organizationId\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
				organizationId, email, password);
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		return loginJson.get("accessToken").asText();
	}
}
