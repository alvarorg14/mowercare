package com.mowercare.exception;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final URI TYPE_BOOTSTRAP_UNAUTHORIZED = URI.create("urn:mowercare:problem:BOOTSTRAP_UNAUTHORIZED");
	private static final URI TYPE_BOOTSTRAP_ALREADY_COMPLETED =
			URI.create("urn:mowercare:problem:BOOTSTRAP_ALREADY_COMPLETED");
	private static final URI TYPE_VALIDATION_ERROR = URI.create("urn:mowercare:problem:VALIDATION_ERROR");
	private static final URI TYPE_AUTH_INVALID_CREDENTIALS = URI.create("urn:mowercare:problem:AUTH_INVALID_CREDENTIALS");
	private static final URI TYPE_AUTH_REFRESH_INVALID = URI.create("urn:mowercare:problem:AUTH_REFRESH_INVALID");

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

	private static URI requestInstance(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		if (query != null && !query.isEmpty()) {
			return URI.create(uri + "?" + query);
		}
		return URI.create(uri);
	}
}
