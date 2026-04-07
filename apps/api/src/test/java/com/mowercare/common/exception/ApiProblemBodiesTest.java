package com.mowercare.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;

class ApiProblemBodiesTest {

	@Test
	@DisplayName("given request with query when account deactivated then instance includes query")
	void givenRequestWithQuery_whenAccountDeactivated_thenInstanceIncludesQuery() {
		HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/api/v1/x");
		when(req.getQueryString()).thenReturn("a=1");
		ProblemDetail pd = ApiProblemBodies.accountDeactivated(req);
		assertThat(pd.getInstance()).isEqualTo(URI.create("/api/v1/x?a=1"));
		assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(pd.getProperties().get("code")).isEqualTo("ACCOUNT_DEACTIVATED");
	}

	@Test
	@DisplayName("given request when user deactivated management then code and conflict status")
	void givenRequest_whenUserDeactivatedManagement_thenCodeAndConflictStatus() {
		HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/api/v1/orgs/o/users/u");
		when(req.getQueryString()).thenReturn(null);
		ProblemDetail pd = ApiProblemBodies.userDeactivatedManagement(req);
		assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(pd.getProperties().get("code")).isEqualTo("USER_DEACTIVATED");
	}

	@Test
	@DisplayName("given request without query when last admin deactivation then instance is path only")
	void givenRequestWithoutQuery_whenLastAdminDeactivation_thenInstanceIsPathOnly() {
		HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/api/v1/orgs/o/users/u");
		when(req.getQueryString()).thenReturn(null);
		ProblemDetail pd = ApiProblemBodies.lastAdminDeactivation(req);
		assertThat(pd.getInstance()).isEqualTo(URI.create("/api/v1/orgs/o/users/u"));
		assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
	}
}
