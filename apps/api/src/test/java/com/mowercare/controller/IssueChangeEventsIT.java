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
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

class IssueChangeEventsIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String BOOTSTRAP_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

	private static final UUID FOREIGN_ORG = UUID.fromString("00000000-0000-0000-0000-000000000042");

	private static final UUID RANDOM_ISSUE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private IssueChangeEventRepository issueChangeEventRepository;

	@Autowired
	private IssueRepository issueRepository;

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
	@DisplayName("given created issue when GET change-events then 200 with CREATED and actor")
	void givenCreatedIssue_whenGetChangeEvents_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		String createBody =
				"{\"title\":\"History me\",\"description\":null,\"status\":\"OPEN\",\"priority\":\"HIGH\",\"customerLabel\":null,\"siteLabel\":null}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].changeType").value("CREATED"))
				.andExpect(jsonPath("$.items[0].actorLabel").value("admin@acme.test"))
				.andExpect(jsonPath("$.items[0].newValue").value("History me"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.number").value(0));
	}

	@Test
	@DisplayName("given issue when PATCH title then GET change-events ordered with TITLE_CHANGED")
	void givenIssue_whenPatchTitle_whenGetChangeEvents_thenSecondEventTitleChanged() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"title\":\"Before\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(patch("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"After\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].changeType").value("CREATED"))
				.andExpect(jsonPath("$.items[1].changeType").value("TITLE_CHANGED"))
				.andExpect(jsonPath("$.items[1].oldValue").value("Before"))
				.andExpect(jsonPath("$.items[1].newValue").value("After"));
	}

	@Test
	@DisplayName("given issue when PATCH assignee then GET change-events resolves assignee labels")
	void givenIssue_whenPatchAssignee_whenGetChangeEvents_thenAssigneeLabels() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		UUID orgUuid = UUID.fromString(orgId);
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String adminId = userRepository
				.findByOrganization_IdAndEmail(orgUuid, "admin@acme.test")
				.orElseThrow()
				.getId()
				.toString();
		String techId = userRepository
				.findByOrganization_IdAndEmail(orgUuid, "tech@acme.test")
				.orElseThrow()
				.getId()
				.toString();

		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								String.format(
										"{\"title\":\"Reassign me\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\",\"assigneeUserId\":\"%s\"}",
										adminId)))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(patch("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"assigneeUserId\":\"%s\"}", techId)))
				.andExpect(status().isOk());

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].changeType").value("CREATED"))
				.andExpect(jsonPath("$.items[1].changeType").value("ASSIGNEE_CHANGED"))
				.andExpect(jsonPath("$.items[1].oldAssigneeLabel").value("admin@acme.test"))
				.andExpect(jsonPath("$.items[1].newAssigneeLabel").value("tech@acme.test"));
	}

	@Test
	@DisplayName("given unknown issue when GET change-events then 404")
	void givenUnknownIssue_whenGetChangeEvents_thenNotFound() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						RANDOM_ISSUE_ID)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("given wrong org path when GET change-events then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetChangeEvents_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						FOREIGN_ORG,
						RANDOM_ISSUE_ID)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
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
