package com.mowercare.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

/**
 * FR27: no public customer registration routes; employee-only interactive access.
 * Under {@code /api/v1/auth/**} (permitAll), unmapped paths yield 404. Other {@code /api/v1/**}
 * paths without JWT yield 401 before MVC dispatch.
 */
class EmployeeOnlyAccessGuardrailsIT extends AbstractPostgresIntegrationTest {

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
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	@Test
	@DisplayName("given no handler when POST fake customer signup paths under permitAll auth then 404")
	void givenNoHandler_whenPostUnderAuthPermitAll_then404() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("given no JWT when POST outside permitAll then 401")
	void givenNoJwt_whenPostOutsidePermitAll_then401() throws Exception {
		mockMvc.perform(post("/api/v1/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/public/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("given login when POST with empty body then 400 — real auth handler present")
	void givenLogin_whenPostEmptyBody_then400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}
}
