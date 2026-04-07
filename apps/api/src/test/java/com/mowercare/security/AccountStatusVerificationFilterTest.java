package com.mowercare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mowercare.model.AccountStatus;
import com.mowercare.model.User;
import com.mowercare.repository.UserRepository;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class AccountStatusVerificationFilterTest {

	@Mock
	private UserRepository userRepository;

	private AccountStatusVerificationFilter filter;

	private final UUID userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@BeforeEach
	void setUp() {
		filter = new AccountStatusVerificationFilter(userRepository, new ObjectMapper());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("given deactivated user when filter then unauthorized and chain not called")
	void givenDeactivatedUser_whenFilter_thenUnauthorizedAndChainNotCalled() throws ServletException, IOException {
		User u = mock(User.class);
		when(u.getAccountStatus()).thenReturn(AccountStatus.DEACTIVATED);
		when(userRepository.findById(userId)).thenReturn(Optional.of(u));

		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/organizations/o/issues");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = Mockito.mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		verify(chain, never()).doFilter(any(), any());
	}


	@Test
	@DisplayName("given auth path when filter then skipped and chain called")
	void givenAuthPath_whenFilter_thenSkippedAndChainCalled() throws ServletException, IOException {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/auth/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = Mockito.mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
	}

	@Test
	@DisplayName("given active user when filter on protected path then chain continues")
	void givenActiveUser_whenFilterOnProtectedPath_thenChainContinues() throws ServletException, IOException {
		User u = mock(User.class);
		when(u.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(u));

		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/organizations/o/issues");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = Mockito.mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	@DisplayName("given user id not found when filter on protected path then chain continues")
	void givenUserIdNotFound_whenFilterOnProtectedPath_thenChainContinues() throws ServletException, IOException {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/organizations/o/issues");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = Mockito.mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
	}
}
