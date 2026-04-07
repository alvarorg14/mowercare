package com.mowercare.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApiAuthenticationEntryPointTest {

	private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint();
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	@DisplayName("given no bearer header when commence then auth required code")
	void givenNoBearerHeader_whenCommence_thenAuthRequiredCode() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/orgs/x");
		MockHttpServletResponse response = new MockHttpServletResponse();
		entryPoint.commence(request, response, null);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(response.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		JsonNode body = mapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
		assertThat(body.get("code").asText()).isEqualTo("AUTH_REQUIRED");
	}

	@Test
	@DisplayName("given bearer prefix when commence then invalid token code")
	void givenBearerPrefix_whenCommence_thenInvalidTokenCode() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/orgs/x");
		request.addHeader("Authorization", "Bearer x.y.z");
		MockHttpServletResponse response = new MockHttpServletResponse();
		entryPoint.commence(request, response, null);
		JsonNode body = mapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
		assertThat(body.get("code").asText()).isEqualTo("AUTH_INVALID_TOKEN");
	}
}
