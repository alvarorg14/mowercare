package com.mowercare.exception;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Shared RFC 7807 bodies for security filters and {@link ApiExceptionHandler}.
 */
public final class ApiProblemBodies {

	public static final URI TYPE_ACCOUNT_DEACTIVATED = URI.create("urn:mowercare:problem:ACCOUNT_DEACTIVATED");
	public static final URI TYPE_LAST_ADMIN_DEACTIVATION = URI.create("urn:mowercare:problem:LAST_ADMIN_DEACTIVATION");
	public static final URI TYPE_USER_DEACTIVATED = URI.create("urn:mowercare:problem:USER_DEACTIVATED");

	private ApiProblemBodies() {}

	public static ProblemDetail accountDeactivated(HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "This employee account has been deactivated.");
		pd.setType(TYPE_ACCOUNT_DEACTIVATED);
		pd.setTitle("Unauthorized");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "ACCOUNT_DEACTIVATED");
		return pd;
	}

	public static ProblemDetail lastAdminDeactivation(HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"The organization must retain at least one user with the Admin role.");
		pd.setType(TYPE_LAST_ADMIN_DEACTIVATION);
		pd.setTitle("Conflict");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "LAST_ADMIN_DEACTIVATION");
		return pd;
	}

	public static ProblemDetail userDeactivatedManagement(HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, "This employee account is deactivated; management actions are not allowed.");
		pd.setType(TYPE_USER_DEACTIVATED);
		pd.setTitle("Conflict");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "USER_DEACTIVATED");
		return pd;
	}

	static URI requestInstance(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		if (query != null && !query.isEmpty()) {
			return URI.create(uri + "?" + query);
		}
		return URI.create(uri);
	}
}
