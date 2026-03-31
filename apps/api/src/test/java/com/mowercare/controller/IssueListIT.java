package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

class IssueListIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String BOOTSTRAP_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

	private static final UUID FOREIGN_ORG = UUID.fromString("00000000-0000-0000-0000-000000000042");

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
	@DisplayName("given issues when GET scope=open then excludes resolved/closed")
	void givenSeededIssues_whenGetOpen_thenOnlyNonTerminal() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String adminUserId = userRepository.findAll().getFirst().getId().toString();

		postIssue(orgId, access, "{\"title\":\"Open A\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}");
		postIssue(orgId, access, "{\"title\":\"Resolved B\",\"status\":\"RESOLVED\",\"priority\":\"LOW\"}");
		postIssue(
				orgId,
				access,
				String.format(
						"{\"title\":\"Mine Open\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"assigneeUserId\":\"%s\"}",
						adminUserId));

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.param("scope", "open")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[?(@.status == 'RESOLVED')]", hasSize(0)));
	}

	@Test
	@DisplayName("given issues when GET scope=all then includes all statuses")
	void givenSeededIssues_whenGetAll_thenIncludesResolved() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		postIssue(orgId, access, "{\"title\":\"Open A\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}");
		postIssue(orgId, access, "{\"title\":\"Resolved B\",\"status\":\"RESOLVED\",\"priority\":\"LOW\"}");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.param("scope", "all")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	@DisplayName("given issues when GET scope=mine then only assignee=sub")
	void givenSeededIssues_whenGetMine_thenFiltered() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String adminUserId = userRepository.findAll().getFirst().getId().toString();

		postIssue(orgId, access, "{\"title\":\"Unassigned\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}");
		postIssue(
				orgId,
				access,
				String.format(
						"{\"title\":\"Assigned to me\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"assigneeUserId\":\"%s\"}",
						adminUserId));

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.param("scope", "mine")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Assigned to me"));
	}

	@Test
	@DisplayName("given no scope param when GET issues then defaults to open")
	void givenNoScope_whenGetIssues_thenDefaultsOpen() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		postIssue(orgId, access, "{\"title\":\"Open A\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}");
		postIssue(orgId, access, "{\"title\":\"Resolved B\",\"status\":\"RESOLVED\",\"priority\":\"LOW\"}");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Open A"));
	}

	@Test
	@DisplayName("given invalid scope when GET issues then 400 VALIDATION_ERROR")
	void givenBadScope_whenGetIssues_thenBadRequest() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.param("scope", "nope")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("given wrong org path when GET issues then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetIssues_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given list item when GET then includes assigneeLabel and timestamps")
	void givenIssueWithAssignee_whenGetList_thenShape() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String adminUserId = userRepository.findAll().getFirst().getId().toString();

		postIssue(
				orgId,
				access,
				String.format(
						"{\"title\":\"L\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\",\"assigneeUserId\":\"%s\"}",
						adminUserId));

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.param("scope", "all")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].assigneeLabel").value("admin@acme.test"))
				.andExpect(jsonPath("$.items[0].assigneeUserId").value(adminUserId))
				.andExpect(jsonPath("$.items[0].updatedAt").exists())
				.andExpect(jsonPath("$.items[0].createdAt").exists());
	}

	private void postIssue(String orgId, String access, String json) throws Exception {
		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated());
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
