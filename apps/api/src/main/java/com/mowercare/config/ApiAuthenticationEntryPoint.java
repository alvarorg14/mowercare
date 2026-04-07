package com.mowercare.config;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Maps OAuth2 resource server authentication failures to RFC 7807 Problem Details with stable {@code code}
 * values ({@code AUTH_REQUIRED}, {@code AUTH_INVALID_TOKEN}), consistent with {@link com.mowercare.common.exception.ApiExceptionHandler}.
 * Response JSON is built as a {@link Map} so {@code code} is a top-level field (aligned with MVC {@code ProblemDetail} serialization).
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final URI TYPE_AUTH_REQUIRED = URI.create("urn:mowercare:problem:AUTH_REQUIRED");
	private static final URI TYPE_AUTH_INVALID_TOKEN = URI.create("urn:mowercare:problem:AUTH_INVALID_TOKEN");

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		boolean hasBearer = hasBearerToken(request);
		String code = hasBearer ? "AUTH_INVALID_TOKEN" : "AUTH_REQUIRED";
		URI type = hasBearer ? TYPE_AUTH_INVALID_TOKEN : TYPE_AUTH_REQUIRED;
		String detail = hasBearer
				? "Access token is invalid, expired, or malformed."
				: "Bearer access token is required.";

		URI instance = requestInstance(request);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", type.toString());
		body.put("title", "Unauthorized");
		body.put("status", HttpStatus.UNAUTHORIZED.value());
		body.put("detail", detail);
		body.put("instance", instance.toString());
		body.put("code", code);

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
	}

	private static boolean hasBearerToken(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");
		return auth != null && auth.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
	}

	private static URI requestInstance(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		if (query != null && !query.isEmpty()) {
			return URI.create(uri + "?" + query);
		}
		return URI.create(uri);
	}
}
