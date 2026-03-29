package com.mowercare.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mowercare.exception.ApiExceptionHandler;
import com.mowercare.exception.BootstrapAlreadyCompletedException;
import com.mowercare.exception.InvalidBootstrapTokenException;
import com.mowercare.model.response.BootstrapOrganizationResponse;
import com.mowercare.service.BootstrapService;

@ExtendWith(MockitoExtension.class)
class BootstrapControllerTest {

	private static final String HEADER = "X-Bootstrap-Token";
	private static final String TOKEN = "unit-test-token";

	@Mock
	private BootstrapService bootstrapService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		BootstrapController controller = new BootstrapController(bootstrapService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("given valid body and token when POST bootstrap then returns 201 with organization and user ids")
	void givenValidRequest_whenPostBootstrap_thenReturnsCreated() throws Exception {
		UUID orgId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(bootstrapService.bootstrapOrganization(eq(TOKEN), eq("Acme"), eq("a@b.test"), eq("password123")))
				.thenReturn(new BootstrapOrganizationResponse(orgId, userId));

		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header(HEADER, TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationName\":\"Acme\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"password123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.organizationId").value(orgId.toString()))
				.andExpect(jsonPath("$.userId").value(userId.toString()));

		verify(bootstrapService).bootstrapOrganization(TOKEN, "Acme", "a@b.test", "password123");
	}

	@Test
	@DisplayName("given password shorter than 8 characters when POST bootstrap then returns 400 Problem Details")
	void givenPasswordTooShort_whenPostBootstrap_thenReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header(HEADER, TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationName\":\"Acme\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(content().string(containsString("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("given invalid email format when POST bootstrap then returns 400 Problem Details")
	void givenInvalidEmail_whenPostBootstrap_thenReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header(HEADER, TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationName\":\"Acme\",\"adminEmail\":\"not-an-email\",\"adminPassword\":\"password123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("given service rejects token when POST bootstrap then returns 401 Problem Details")
	void givenInvalidTokenFromService_whenPostBootstrap_thenReturnsUnauthorized() throws Exception {
		when(bootstrapService.bootstrapOrganization(anyString(), anyString(), anyString(), anyString()))
				.thenThrow(new InvalidBootstrapTokenException());

		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header(HEADER, TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationName\":\"Acme\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"password123\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(containsString("BOOTSTRAP_UNAUTHORIZED")));
	}

	@Test
	@DisplayName("given bootstrap already completed when POST bootstrap then returns 409 Problem Details")
	void givenBootstrapAlreadyCompleted_whenPostBootstrap_thenReturnsConflict() throws Exception {
		when(bootstrapService.bootstrapOrganization(anyString(), anyString(), anyString(), anyString()))
				.thenThrow(new BootstrapAlreadyCompletedException());

		mockMvc.perform(post("/api/v1/bootstrap/organization")
						.header(HEADER, TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationName\":\"Acme\",\"adminEmail\":\"a@b.test\",\"adminPassword\":\"password123\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().string(containsString("BOOTSTRAP_ALREADY_COMPLETED")));
	}
}
