package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

class RbacEnforcementIT extends AbstractPostgresIntegrationTest {

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
	@DisplayName("given technician when POST admin reassign stub then 403 FORBIDDEN_ROLE")
	void givenTechnician_whenAdminReassign_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues/_admin/reassign", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ROLE"));
	}

	@Test
	@DisplayName("given admin when POST admin reassign stub then 204")
	void givenAdmin_whenAdminReassign_thenNoContent() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues/_admin/reassign", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("given admin when GET issues then 200 list")
	void givenAdmin_whenGetIssues_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	@DisplayName("given admin when GET assignable-users then 200 array")
	void givenAdmin_whenGetAssignableUsers_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/assignable-users", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].email").value("admin@acme.test"));
	}

	@Test
	@DisplayName("given technician when GET assignable-users then 200 array")
	void givenTechnician_whenGetAssignableUsers_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/assignable-users", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].email").value("admin@acme.test"))
				.andExpect(jsonPath("$[1].email").value("tech@acme.test"));
	}

	@Test
	@DisplayName("given valid token wrong org path when GET assignable-users then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetAssignableUsers_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/assignable-users", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given technician when GET issues then 200 list")
	void givenTechnician_whenGetIssues_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	@DisplayName("given admin when POST issues then 201 with persisted issue")
	void givenAdmin_whenPostIssues_thenCreated() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		String body =
				"{\"title\":\"Test issue\",\"description\":null,\"status\":\"OPEN\",\"priority\":\"MEDIUM\",\"assigneeUserId\":null,\"customerLabel\":null,\"siteLabel\":null}";

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.title").value("Test issue"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.priority").value("MEDIUM"));
	}

	@Test
	@DisplayName("given technician when POST issues then 201")
	void givenTechnician_whenPostIssues_thenCreated() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		String body =
				"{\"title\":\"Tech issue\",\"status\":\"OPEN\",\"priority\":\"LOW\"}";

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Tech issue"))
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	@DisplayName("given valid token wrong org path when GET issues then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetIssues_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given admin when GET issue by id then 200")
	void givenAdmin_whenGetIssueById_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String body =
				"{\"title\":\"Rbac detail\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Rbac detail"));
	}

	@Test
	@DisplayName("given admin when GET issue change-events then 200")
	void givenAdmin_whenGetIssueChangeEvents_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String body = "{\"title\":\"Hist rbac\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						issueId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items[0].changeType").value("CREATED"));
	}

	@Test
	@DisplayName("given technician when GET issue change-events then 200")
	void givenTechnician_whenGetIssueChangeEvents_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String adminAccess = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String techAccess = loginAccessToken(orgId, "tech@acme.test", "secret12345");
		String body = "{\"title\":\"Tech reads history\",\"status\":\"OPEN\",\"priority\":\"LOW\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + adminAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						orgId,
						issueId)
						.header("Authorization", "Bearer " + techAccess))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].changeType").value("CREATED"));
	}

	@Test
	@DisplayName("given valid token wrong org path when GET issue change-events then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetIssueChangeEvents_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get(
						"/api/v1/organizations/{organizationId}/issues/{issueId}/change-events",
						FOREIGN_ORG,
						"00000000-0000-0000-0000-000000000099")
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given technician when GET issue by id then 200")
	void givenTechnician_whenGetIssueById_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String adminAccess = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String techAccess = loginAccessToken(orgId, "tech@acme.test", "secret12345");
		String body = "{\"title\":\"Tech can read\",\"status\":\"OPEN\",\"priority\":\"LOW\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + adminAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + techAccess))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Tech can read"));
	}

	@Test
	@DisplayName("given technician when PATCH issue then 200")
	void givenTechnician_whenPatchIssue_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String adminAccess = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String techAccess = loginAccessToken(orgId, "tech@acme.test", "secret12345");
		String body = "{\"title\":\"Patch me\",\"status\":\"OPEN\",\"priority\":\"LOW\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + adminAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(patch("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + techAccess)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Patched by tech\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Patched by tech"));
	}

	@Test
	@DisplayName("given admin when PATCH issue then 200")
	void givenAdmin_whenPatchIssue_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");
		String body = "{\"title\":\"Admin patch\",\"status\":\"OPEN\",\"priority\":\"MEDIUM\"}";
		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(patch("/api/v1/organizations/{organizationId}/issues/{issueId}", orgId, issueId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"priority\":\"HIGH\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.priority").value("HIGH"));
	}

	@Test
	@DisplayName("given valid token wrong org path when POST admin reassign stub then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenAdminReassign_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues/_admin/reassign", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	private void seedTechnician(String organizationId) {
		UUID orgUuid = UUID.fromString(organizationId);
		Organization org = organizationRepository.findById(orgUuid).orElseThrow();
		User tech = new User(org, "tech@acme.test", passwordEncoder.encode("secret12345"), UserRole.TECHNICIAN);
		userRepository.save(tech);
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
