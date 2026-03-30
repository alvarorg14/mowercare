package com.mowercare.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

class AuthIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String BOOTSTRAP_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

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

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
		organizationRepository.deleteAll();
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@DynamicPropertySource
	static void bootstrapToken(DynamicPropertyRegistry registry) {
		registry.add("mowercare.bootstrap.token", () -> BOOTSTRAP_TOKEN);
	}

	@Test
	@DisplayName("given bootstrapped admin when login refresh logout then tokens rotate and refresh is revoked after logout")
	void givenBootstrappedAdmin_whenLoginRefreshLogout_thenFlowSucceeds() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(orgId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").exists())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String refresh1 = loginJson.get("refreshToken").asText();

		MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh1 + "\"}"))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
		String refresh2 = refreshJson.get("refreshToken").asText();
		assertThat(refresh2).isNotEqualTo(refresh1);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh1 + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh2 + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh2 + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));
	}

	@Test
	@DisplayName("given wrong password when login then returns 401 AUTH_INVALID_CREDENTIALS")
	void givenWrongPassword_whenLogin_thenUnauthorized() throws Exception {
		String orgId = bootstrapAndGetOrganizationId();

		String body = String.format(
				"{\"organizationId\":\"%s\",\"email\":\"admin@acme.test\",\"password\":\"wrongpassword\"}", orgId);

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("given unknown organization when login then returns 401")
	void givenUnknownOrg_whenLogin_thenUnauthorized() throws Exception {
		bootstrapAndGetOrganizationId();

		String body =
				"{\"organizationId\":\"00000000-0000-0000-0000-000000000001\",\"email\":\"admin@acme.test\",\"password\":\"secret12345\"}";

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("given garbage refresh token when refresh then returns 401")
	void givenGarbageRefresh_whenRefresh_thenUnauthorized() throws Exception {
		bootstrapAndGetOrganizationId();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"not-a-real-token\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALID"));
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

	private String loginBody(String organizationId) {
		return String.format(
				"{\"organizationId\":\"%s\",\"email\":\"admin@acme.test\",\"password\":\"secret12345\"}",
				organizationId);
	}
}
