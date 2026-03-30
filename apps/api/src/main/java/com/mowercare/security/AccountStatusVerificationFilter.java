package com.mowercare.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.exception.ApiProblemBodies;
import com.mowercare.model.AccountStatus;
import com.mowercare.model.User;
import com.mowercare.repository.UserRepository;

/**
 * Blocks protected API access when the authenticated user is {@link AccountStatus#DEACTIVATED}.
 * Runs after JWT authentication; writes RFC 7807 Problem Details (same shape as {@code ApiExceptionHandler}).
 */
@Component
public class AccountStatusVerificationFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public AccountStatusVerificationFilter(UserRepository userRepository, ObjectMapper objectMapper) {
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		if (shouldSkip(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
			filterChain.doFilter(request, response);
			return;
		}
		Object principal = jwtAuth.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			filterChain.doFilter(request, response);
			return;
		}
		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException ex) {
			filterChain.doFilter(request, response);
			return;
		}
		User user = userRepository.findById(userId).orElse(null);
		if (user == null || user.getAccountStatus() != AccountStatus.DEACTIVATED) {
			filterChain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.parseMediaType("application/problem+json").toString());
		objectMapper.writeValue(response.getOutputStream(), ApiProblemBodies.accountDeactivated(request));
	}

	private static boolean shouldSkip(HttpServletRequest request) {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		String relative = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
		return relative.startsWith("/api/v1/auth/")
				|| relative.startsWith("/api/v1/bootstrap/")
				|| relative.startsWith("/v3/api-docs")
				|| relative.startsWith("/swagger-ui")
				|| relative.equals("/swagger-ui.html");
	}
}
