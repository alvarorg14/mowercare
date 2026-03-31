package com.mowercare.exception;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mowercare.common.persistence.DataIntegrityViolations;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static final URI TYPE_BOOTSTRAP_UNAUTHORIZED = URI.create("urn:mowercare:problem:BOOTSTRAP_UNAUTHORIZED");
	private static final URI TYPE_BOOTSTRAP_ALREADY_COMPLETED =
			URI.create("urn:mowercare:problem:BOOTSTRAP_ALREADY_COMPLETED");
	private static final URI TYPE_VALIDATION_ERROR = URI.create("urn:mowercare:problem:VALIDATION_ERROR");
	private static final URI TYPE_AUTH_INVALID_CREDENTIALS = URI.create("urn:mowercare:problem:AUTH_INVALID_CREDENTIALS");
	private static final URI TYPE_AUTH_REFRESH_INVALID = URI.create("urn:mowercare:problem:AUTH_REFRESH_INVALID");
	private static final URI TYPE_AUTH_INVALID_TOKEN = URI.create("urn:mowercare:problem:AUTH_INVALID_TOKEN");
	private static final URI TYPE_TENANT_ACCESS_DENIED = URI.create("urn:mowercare:problem:TENANT_ACCESS_DENIED");
	private static final URI TYPE_FORBIDDEN_ROLE = URI.create("urn:mowercare:problem:FORBIDDEN_ROLE");
	private static final URI TYPE_USER_EMAIL_CONFLICT = URI.create("urn:mowercare:problem:USER_EMAIL_CONFLICT");
	private static final URI TYPE_INVITE_TOKEN_INVALID = URI.create("urn:mowercare:problem:INVITE_TOKEN_INVALID");
	private static final URI TYPE_NOT_FOUND = URI.create("urn:mowercare:problem:NOT_FOUND");
	private static final URI TYPE_LAST_ADMIN_REMOVAL = URI.create("urn:mowercare:problem:LAST_ADMIN_REMOVAL");

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ProblemDetail> resourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setType(TYPE_NOT_FOUND);
		pd.setTitle("Not Found");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(InvalidBootstrapTokenException.class)
	public ResponseEntity<ProblemDetail> invalidBootstrapToken(
			InvalidBootstrapTokenException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED,
				"Bootstrap token is missing, invalid, or bootstrap is not configured.");
		pd.setType(TYPE_BOOTSTRAP_UNAUTHORIZED);
		pd.setTitle("Unauthorized");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "BOOTSTRAP_UNAUTHORIZED");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(BootstrapAlreadyCompletedException.class)
	public ResponseEntity<ProblemDetail> bootstrapAlreadyCompleted(
			BootstrapAlreadyCompletedException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"An organization already exists; bootstrap cannot run again.");
		pd.setType(TYPE_BOOTSTRAP_ALREADY_COMPLETED);
		pd.setTitle("Conflict");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "BOOTSTRAP_ALREADY_COMPLETED");
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "Invalid organization, email, or password.");
		pd.setType(TYPE_AUTH_INVALID_CREDENTIALS);
		pd.setTitle("Unauthorized");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "AUTH_INVALID_CREDENTIALS");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(InvalidAccessTokenClaimsException.class)
	public ResponseEntity<ProblemDetail> invalidAccessTokenClaims(
			InvalidAccessTokenClaimsException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "Access token is invalid, expired, or malformed.");
		pd.setType(TYPE_AUTH_INVALID_TOKEN);
		pd.setTitle("Unauthorized");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "AUTH_INVALID_TOKEN");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(ForbiddenRoleException.class)
	public ResponseEntity<ProblemDetail> forbiddenRole(ForbiddenRoleException ex, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.FORBIDDEN,
				"This action requires the " + ex.getRequiredRole().name() + " role.");
		pd.setType(TYPE_FORBIDDEN_ROLE);
		pd.setTitle("Forbidden");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "FORBIDDEN_ROLE");
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(TenantAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> tenantAccessDenied(TenantAccessDeniedException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.FORBIDDEN,
				"Access to the requested organization is not allowed for this token.");
		pd.setType(TYPE_TENANT_ACCESS_DENIED);
		pd.setTitle("Forbidden");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "TENANT_ACCESS_DENIED");
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ProblemDetail> invalidRefresh(InvalidRefreshTokenException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired, or revoked.");
		pd.setType(TYPE_AUTH_REFRESH_INVALID);
		pd.setTitle("Unauthorized");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "AUTH_REFRESH_INVALID");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(UserEmailConflictException.class)
	public ResponseEntity<ProblemDetail> userEmailConflict(UserEmailConflictException ignored, HttpServletRequest request) {
		return conflictUserEmail(request);
	}

	@ExceptionHandler(LastAdminRemovalException.class)
	public ResponseEntity<ProblemDetail> lastAdminRemoval(LastAdminRemovalException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"The organization must retain at least one user with the Admin role.");
		pd.setType(TYPE_LAST_ADMIN_REMOVAL);
		pd.setTitle("Conflict");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "LAST_ADMIN_REMOVAL");
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(LastAdminDeactivationException.class)
	public ResponseEntity<ProblemDetail> lastAdminDeactivation(
			LastAdminDeactivationException ignored, HttpServletRequest request) {
		ProblemDetail pd = ApiProblemBodies.lastAdminDeactivation(request);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(UserDeactivatedManagementException.class)
	public ResponseEntity<ProblemDetail> userDeactivatedManagement(
			UserDeactivatedManagementException ignored, HttpServletRequest request) {
		ProblemDetail pd = ApiProblemBodies.userDeactivatedManagement(request);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		if (DataIntegrityViolations.isDuplicateOrgEmail(ex)) {
			return conflictUserEmail(request);
		}
		log.warn("Unhandled data integrity violation", ex);
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR, "A database constraint was violated.");
		pd.setTitle("Internal Server Error");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "DATA_CONSTRAINT_VIOLATION");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(InviteTokenInvalidException.class)
	public ResponseEntity<ProblemDetail> inviteTokenInvalid(InviteTokenInvalidException ignored, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "Invite token is invalid, expired, or already used.");
		pd.setType(TYPE_INVITE_TOKEN_INVALID);
		pd.setTitle("Bad Request");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "INVITE_TOKEN_INVALID");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> messageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.debug("Failed to read HTTP message: {}", ex.getMessage());
		String detail = "Request body could not be parsed. Check JSON syntax and enum values.";
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		pd.setType(TYPE_VALIDATION_ERROR);
		pd.setTitle("Bad Request");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "VALIDATION_ERROR");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String violations = String.join(
				"; ",
				ex.getBindingResult().getFieldErrors().stream()
						.map(err -> err.getField() + ": " + err.getDefaultMessage())
						.toList());
		String detail = violations.isEmpty() ? "Request validation failed." : "Request validation failed: " + violations;
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		pd.setType(TYPE_VALIDATION_ERROR);
		pd.setTitle("Bad Request");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "VALIDATION_ERROR");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
	}

	private ResponseEntity<ProblemDetail> conflictUserEmail(HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, "An employee with this email already exists for this organization.");
		pd.setType(TYPE_USER_EMAIL_CONFLICT);
		pd.setTitle("Conflict");
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", "USER_EMAIL_CONFLICT");
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.contentType(MediaType.parseMediaType("application/problem+json"))
				.body(pd);
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
