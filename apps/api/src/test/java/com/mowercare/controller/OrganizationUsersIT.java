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

class OrganizationUsersIT extends AbstractPostgresIntegrationTest {

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
	@DisplayName("given admin when POST user with password then login succeeds")
	void givenAdmin_whenCreateWithPassword_thenLogin() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"email\":\"newtech@acme.test\",\"role\":\"TECHNICIAN\",\"initialPassword\":\"secret12345\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.inviteToken").doesNotExist());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"organizationId\":\"%s\",\"email\":\"newtech@acme.test\",\"password\":\"secret12345\"}",
								orgId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("given admin when POST invite without password then login fails until accept-invite")
	void givenAdmin_whenInvite_thenLoginAfterAccept() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		MvcResult created = mockMvc.perform(post("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"invited@acme.test\",\"role\":\"TECHNICIAN\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accountStatus").value("PENDING_INVITE"))
				.andExpect(jsonPath("$.inviteToken").exists())
				.andReturn();

		JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
		String token = json.get("inviteToken").asText();

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"organizationId\":\"%s\",\"email\":\"invited@acme.test\",\"password\":\"secret12345\"}",
								orgId)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));

		mockMvc.perform(post("/api/v1/auth/accept-invite")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"token\":\"%s\",\"password\":\"secret12345\"}", token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"organizationId\":\"%s\",\"email\":\"invited@acme.test\",\"password\":\"secret12345\"}",
								orgId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("given technician when GET users then 403 FORBIDDEN_ROLE")
	void givenTechnician_whenListUsers_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ROLE"));
	}

	@Test
	@DisplayName("given technician when POST users then 403 FORBIDDEN_ROLE")
	void givenTechnician_whenCreateUser_thenForbidden() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		seedTechnician(orgId);
		String access = loginAccessToken(orgId, "tech@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"email\":\"newtech2@acme.test\",\"role\":\"TECHNICIAN\",\"initialPassword\":\"secret12345\"}"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("FORBIDDEN_ROLE"));
	}

	@Test
	@DisplayName("given admin when duplicate email then 409 USER_EMAIL_CONFLICT")
	void givenDuplicateEmail_whenCreate_thenConflict() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"email\":\"dup@acme.test\",\"role\":\"TECHNICIAN\",\"initialPassword\":\"secret12345\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"email\":\"dup@acme.test\",\"role\":\"TECHNICIAN\",\"initialPassword\":\"secret12345\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_EMAIL_CONFLICT"));
	}

	@Test
	@DisplayName("given valid token wrong org path when GET users then 403 TENANT_ACCESS_DENIED")
	void givenWrongOrg_whenListUsers_thenTenantDenied() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/users", FOREIGN_ORG)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));
	}

	@Test
	@DisplayName("given admin when GET users then 200 with account statuses")
	void givenAdmin_whenListUsers_thenOk() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();
		String access = loginAccessToken(orgId, "admin@acme.test", "secret12345");

		mockMvc.perform(get("/api/v1/organizations/{organizationId}/users", orgId)
						.header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("admin@acme.test"))
				.andExpect(jsonPath("$[0].accountStatus").value("ACTIVE"));
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
