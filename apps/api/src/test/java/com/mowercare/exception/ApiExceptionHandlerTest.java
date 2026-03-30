package com.mowercare.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

	private static final URI EXPECTED_TYPE_UNAUTHORIZED = URI.create("urn:mowercare:problem:BOOTSTRAP_UNAUTHORIZED");
	private static final URI EXPECTED_TYPE_CONFLICT = URI.create("urn:mowercare:problem:BOOTSTRAP_ALREADY_COMPLETED");
	private static final URI EXPECTED_TYPE_VALIDATION = URI.create("urn:mowercare:problem:VALIDATION_ERROR");
	private static final URI EXPECTED_TYPE_AUTH_CREDENTIALS = URI.create("urn:mowercare:problem:AUTH_INVALID_CREDENTIALS");
	private static final URI EXPECTED_TYPE_AUTH_REFRESH = URI.create("urn:mowercare:problem:AUTH_REFRESH_INVALID");
	private static final URI EXPECTED_TYPE_TENANT_DENIED = URI.create("urn:mowercare:problem:TENANT_ACCESS_DENIED");
	private static final URI EXPECTED_TYPE_AUTH_INVALID_TOKEN = URI.create("urn:mowercare:problem:AUTH_INVALID_TOKEN");

	private ApiExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new ApiExceptionHandler();
	}

	@Test
	@DisplayName("given InvalidBootstrapTokenException when handle then returns 401 problem json with BOOTSTRAP_UNAUTHORIZED")
	void givenInvalidBootstrapToken_whenHandle_thenUnauthorizedProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/bootstrap/organization");

		ResponseEntity<ProblemDetail> response =
				handler.invalidBootstrapToken(new InvalidBootstrapTokenException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().getContentType()).hasToString(MediaType.parseMediaType("application/problem+json").toString());
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("BOOTSTRAP_UNAUTHORIZED");
		assertThat(response.getBody().getStatus()).isEqualTo(401);
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_UNAUTHORIZED);
		assertThat(response.getBody().getInstance()).isEqualTo(URI.create("/api/v1/bootstrap/organization"));
	}

	@Test
	@DisplayName("given BootstrapAlreadyCompletedException when handle then returns 409 problem json with BOOTSTRAP_ALREADY_COMPLETED")
	void givenBootstrapAlreadyCompleted_whenHandle_thenConflictProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/bootstrap/organization");

		ResponseEntity<ProblemDetail> response =
				handler.bootstrapAlreadyCompleted(new BootstrapAlreadyCompletedException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("BOOTSTRAP_ALREADY_COMPLETED");
		assertThat(response.getBody().getStatus()).isEqualTo(409);
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_CONFLICT);
		assertThat(response.getBody().getInstance()).isEqualTo(URI.create("/api/v1/bootstrap/organization"));
	}

	@Test
	@DisplayName("given MethodArgumentNotValidException when handle then returns 400 problem json with VALIDATION_ERROR")
	void givenValidationErrors_whenHandle_thenBadRequestProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/bootstrap/organization");

		Object target = new Object();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
		bindingResult.addError(new FieldError("target", "adminEmail", "must be a well-formed email address"));
		MethodArgumentNotValidException ex =
				new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ProblemDetail> response = handler.validation(ex, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("VALIDATION_ERROR");
		assertThat(response.getBody().getDetail()).contains("adminEmail");
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_VALIDATION);
		assertThat(response.getBody().getInstance()).isEqualTo(URI.create("/api/v1/bootstrap/organization"));
	}

	@Test
	@DisplayName("given InvalidCredentialsException when handle then returns 401 AUTH_INVALID_CREDENTIALS")
	void givenInvalidCredentials_whenHandle_thenUnauthorizedProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/auth/login");

		ResponseEntity<ProblemDetail> response =
				handler.invalidCredentials(new InvalidCredentialsException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("AUTH_INVALID_CREDENTIALS");
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_AUTH_CREDENTIALS);
		assertThat(response.getBody().getInstance()).isEqualTo(URI.create("/api/v1/auth/login"));
	}

	@Test
	@DisplayName("given InvalidAccessTokenClaimsException when handle then returns 401 AUTH_INVALID_TOKEN")
	void givenInvalidAccessTokenClaims_whenHandle_thenUnauthorizedProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/organizations/00000000-0000-0000-0000-000000000001/tenant-scope");

		ResponseEntity<ProblemDetail> response =
				handler.invalidAccessTokenClaims(new InvalidAccessTokenClaimsException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("AUTH_INVALID_TOKEN");
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_AUTH_INVALID_TOKEN);
		assertThat(response.getBody().getInstance())
				.isEqualTo(URI.create("/api/v1/organizations/00000000-0000-0000-0000-000000000001/tenant-scope"));
	}

	@Test
	@DisplayName("given TenantAccessDeniedException when handle then returns 403 TENANT_ACCESS_DENIED")
	void givenTenantAccessDenied_whenHandle_thenForbiddenProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/organizations/00000000-0000-0000-0000-000000000001/tenant-scope");

		ResponseEntity<ProblemDetail> response =
				handler.tenantAccessDenied(new TenantAccessDeniedException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("TENANT_ACCESS_DENIED");
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_TENANT_DENIED);
		assertThat(response.getBody().getInstance())
				.isEqualTo(URI.create("/api/v1/organizations/00000000-0000-0000-0000-000000000001/tenant-scope"));
	}

	@Test
	@DisplayName("given InvalidRefreshTokenException when handle then returns 401 AUTH_REFRESH_INVALID")
	void givenInvalidRefresh_whenHandle_thenUnauthorizedProblem() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/auth/refresh");

		ResponseEntity<ProblemDetail> response =
				handler.invalidRefresh(new InvalidRefreshTokenException(), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody().getProperties().get("code")).isEqualTo("AUTH_REFRESH_INVALID");
		assertThat(response.getBody().getType()).isEqualTo(EXPECTED_TYPE_AUTH_REFRESH);
		assertThat(response.getBody().getInstance()).isEqualTo(URI.create("/api/v1/auth/refresh"));
	}

	@Test
	@DisplayName("given multiple field errors when handle validation then detail lists all violations")
	void givenMultipleFieldErrors_whenValidation_thenDetailContainsAll() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/bootstrap/organization");

		Object target = new Object();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
		bindingResult.addError(new FieldError("target", "adminEmail", "must be a well-formed email address"));
		bindingResult.addError(new FieldError("target", "adminPassword", "size must be between 8 and 128"));
		MethodArgumentNotValidException ex =
				new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ProblemDetail> response = handler.validation(ex, request);

		assertThat(response.getBody().getDetail())
				.contains("adminEmail")
				.contains("adminPassword")
				.contains("size must be between 8 and 128");
	}
}
