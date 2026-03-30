package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
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
	@DisplayName("given admin when GET issues stub then 200 empty list")
	void givenAdmin_whenGetIssues_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	@DisplayName("given technician when GET issues stub then 200 empty list")
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
	@DisplayName("given admin when POST issues stub then 201")
	void givenAdmin_whenPostIssues_thenCreated() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.status").value("stub"));
	}

	@Test
	@DisplayName("given technician when POST issues stub then 201")
	void givenTechnician_whenPostIssues_thenCreated() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/issues", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("stub"));
	}

	@Test
	@DisplayName("given valid token wrong org path when GET issues stub then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenGetIssues_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/issues", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
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
