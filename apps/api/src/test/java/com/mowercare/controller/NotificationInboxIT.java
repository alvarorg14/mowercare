package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.NotificationEventRepository;
import com.mowercare.repository.NotificationRecipientRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

class NotificationInboxIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String BOOTSTRAP_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

	private static final UUID FOREIGN_ORG = UUID.fromString("00000000-0000-0000-0000-000000000042");

	private static final UUID RANDOM_RECIPIENT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

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
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		notificationRecipientRepository.deleteAll();
		notificationEventRepository.deleteAll();
		issueChangeEventRepository.deleteAll();
		issueRepository.deleteAll();
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
	@DisplayName("given tech creates issue when admin lists notifications then rows and mark-read idempotent")
	void givenTechCreatesIssue_whenAdminLists_thenMarkRead() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String techAccess = loginAccessToken(orgId, "tech@acme.test", "secret12345");
		String adminAccess = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + techAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Noise\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}"))
				.andExpect(status().isCreated());

		MvcResult listResult =
				mockMvc.perform(get("/api/v1/organizations/{organizationId}/notifications", orgId)
								.header("Authorization", "Bearer " + adminAccess))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.items.length()").value(1))
						.andExpect(jsonPath("$.items[0].read").value(false))
						.andExpect(jsonPath("$.items[0].issueTitle").value("Noise"))
						.andExpect(jsonPath("$.items[0].eventType").value("issue.created"))
						.andExpect(jsonPath("$.totalElements").value(1))
						.andReturn();

		String recipientId =
				objectMapper.readTree(listResult.getResponse().getContentAsString()).get("items").get(0).get("id").asText();

		mockMvc.perform(patch(
						"/api/v1/organizations/{organizationId}/notifications/{recipientId}/read",
						orgId,
						recipientId)
						.header("Authorization", "Bearer " + adminAccess))
				.andExpect(status().isNoContent());

		mockMvc.perform(patch(
						"/api/v1/organizations/{organizationId}/notifications/{recipientId}/read",
						orgId,
						recipientId)
						.header("Authorization", "Bearer " + adminAccess))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/notifications", orgId)
						.header("Authorization", "Bearer " + adminAccess))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].read").value(true));
	}

	@Test
	@DisplayName("given notifications when tech PATCH admin recipient then 404")
	void givenNotifications_whenTechPatchesAdminRecipient_thenNotFound() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String techAccess = loginAccessToken(orgId, "tech@acme.test", "secret12345");
		String adminAccess = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + techAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}"))
				.andExpect(status().isCreated());

		MvcResult listResult =
				mockMvc.perform(get("/api/v1/organizations/{organizationId}/notifications", orgId)
								.header("Authorization", "Bearer " + adminAccess))
						.andExpect(status().isOk())
						.andReturn();
		String recipientId =
				objectMapper.readTree(listResult.getResponse().getContentAsString()).get("items").get(0).get("id").asText();

		mockMvc.perform(patch(
						"/api/v1/organizations/{organizationId}/notifications/{recipientId}/read",
						orgId,
						recipientId)
						.header("Authorization", "Bearer " + techAccess))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("given org when GET notifications wrong org path then 403")
	void givenOrg_whenGetNotificationsWrongOrg_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/notifications", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given org when PATCH notification read wrong org path then 403")
	void givenOrg_whenPatchNotificationReadWrongOrg_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(patch(
						"/api/v1/organizations/{organizationId}/notifications/{recipientId}/read",
						FOREIGN_ORG,
						RANDOM_RECIPIENT)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given org when PATCH unknown recipient then 404")
	void givenOrg_whenPatchUnknownRecipient_thenNotFound() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(patch(
						"/api/v1/organizations/{organizationId}/notifications/{recipientId}/read",
						orgId,
						RANDOM_RECIPIENT)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("given no notifications when GET then empty items")
	void givenNoNotifications_whenGet_thenEmpty() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/notifications", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0))
				.andExpect(jsonPath("$.totalElements").value(0));
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

	private void seedTechnician(String organizationId) {
		UUID orgUuid = UUID.fromString(organizationId);
		Organization org = organizationRepository.findById(orgUuid).orElseThrow();
		User tech = new User(org, "tech@acme.test", passwordEncoder.encode("secret12345"), UserRole.TECHNICIAN);
		userRepository.save(tech);
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
