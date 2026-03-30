package com.mowercare.controller;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.mowercare.model.UserRole;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

class BootstrapOrganizationIT extends AbstractPostgresIntegrationTest {

	private static final String BOOTSTRAP_TOKEN = "it-bootstrap-token-secret";

	private static final String VALID_BODY =
			"{\"organizationName\":\"Acme Mowers\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"secret12345\"}";

	private static final String MIN_BODY =
			"{\"organizationName\":\"X\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"secret12345\"}";

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
	void cleanDatabase() {
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
	@DisplayName("given empty database and valid token when bootstrap twice then first returns 201 and second returns 409 Problem Details")
	void givenEmptyDbAndValidToken_whenBootstrapTwice_thenCreatedThenConflict() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", BOOTSTRAP_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.organizationId").exists())
				.andExpect(jsonPath("$.userId").exists());

		assertThat(organizationRepository.count()).isEqualTo(1);
		assertThat(userRepository.count()).isEqualTo(1);
		assertThat(userRepository.findAll().getFirst().getRole()).isEqualTo(UserRole.ADMIN);

		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", BOOTSTRAP_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.properties.code").value("BOOTSTRAP_ALREADY_COMPLETED"));
	}

	@Test
	@DisplayName("given wrong bootstrap token when POST bootstrap then returns 401 Problem Details")
	void givenWrongToken_whenPostBootstrap_thenReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", "wrong-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(MIN_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.properties.code").value("BOOTSTRAP_UNAUTHORIZED"));
	}

	@Test
	@DisplayName("given missing X-Bootstrap-Token header when POST bootstrap then returns 401")
	void givenMissingTokenHeader_whenPostBootstrap_thenReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.contentType(MediaType.APPLICATION_JSON)
						.content(MIN_BODY))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("given password shorter than 8 characters when POST bootstrap then returns 400 Problem Details")
	void givenPasswordTooShort_whenPostBootstrap_thenReturnsBadRequest() throws Exception {
		String body =
				"{\"organizationName\":\"Acme\",\"adminEmail\":\"admin@acme.test\",\"adminPassword\":\"short\"}";
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", BOOTSTRAP_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(content().string(containsString("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("given invalid email format when POST bootstrap then returns 400 Problem Details")
	void givenInvalidEmail_whenPostBootstrap_thenReturnsBadRequest() throws Exception {
		String body =
				"{\"organizationName\":\"Acme\",\"adminEmail\":\"not-email\",\"adminPassword\":\"secret12345\"}";
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", BOOTSTRAP_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("VALIDATION_ERROR")));
	}
}
