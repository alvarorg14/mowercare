package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class IssueDetailIT extends AbstractPostgresIntegrationTest {

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
	@DisplayName("given created issue when GET by id then 200 with detail shape")
	void givenCreatedIssue_whenGetById_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		String createBody =
				"{\"title\":\"Detail me\",\"description\":\"Full text\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"customerLabel\":\"C1\",\"siteLabel\":\"S1\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(issueId))
				.andExpect(jsonPath("$.title").value("Detail me"))
				.andExpect(jsonPath("$.description").value("Full text"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.customerLabel").value("C1"))
				.andExpect(jsonPath("$.siteLabel").value("S1"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("given unknown issue id when GET then 404")
	void givenUnknownIssue_whenGetById_thenNotFound() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, RANDOM_ISSUE_ID)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("given wrong org path when GET issue then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetIssue_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues/{issueId}", FOREIGN_ORG, RANDOM_ISSUE_ID)
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
