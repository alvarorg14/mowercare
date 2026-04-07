package com.mowercare.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

/**
 * Ensures bootstrap is rejected when {@code mowercare.bootstrap.token} is blank (misconfiguration).
 */
class BootstrapOrganizationEmptyTokenIT extends AbstractPostgresIntegrationTest {

	private static final String BODY =
			"{\"organizationName\":\"Acme\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"password123\"}";

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@DynamicPropertySource
	static void emptyBootstrapToken(DynamicPropertyRegistry registry) {
		registry.add("mowercare.bootstrap.token", () -> "");
	}

	@Test
	@DisplayName("given empty configured bootstrap token when POST bootstrap then returns 401")
	void givenEmptyConfiguredToken_whenPostBootstrap_thenReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header("X-Bootstrap-Token", "any-value")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}
}
