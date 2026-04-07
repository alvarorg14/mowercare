package com.mowercare.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mowercare.common.exception.ApiExceptionHandler;
import com.mowercare.auth.response.TokenResponse;
import com.mowercare.auth.AuthService;
import com.mowercare.user.OrganizationUserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private AuthService authService;

	@Mock
	private OrganizationUserService organizationUserService;

	private MockMvc mockMvc;

	private final UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	@BeforeEach
	void setUp() throws Exception {
		var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, organizationUserService))
				.setControllerAdvice(new ApiExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	@DisplayName("given valid login json when post login then returns tokens")
	void givenValidLoginJson_whenPostLogin_thenReturnsTokens() throws Exception {
		when(authService.login(eq(orgId), eq("u@test.local"), eq("password12345")))
				.thenReturn(new TokenResponse("access", "refresh", "Bearer", 900));
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"email\":\"u@test.local\",\"password\":\"password12345\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	@DisplayName("given refresh body when post refresh then delegates to auth service")
	void givenRefreshBody_whenPostRefresh_thenDelegatesToAuthService() throws Exception {
		when(authService.refresh("opaque")).thenReturn(new TokenResponse("a2", "r2", "Bearer", 900));
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"opaque\"}"))
				.andExpect(status().isOk());
		verify(authService).refresh("opaque");
	}

	@Test
	@DisplayName("given logout body when post logout then delegates")
	void givenLogoutBody_whenPostLogout_thenDelegates() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"opaque\"}"))
				.andExpect(status().isNoContent());
		verify(authService).logout("opaque");
	}

	@Test
	@DisplayName("given accept invite body when post then delegates")
	void givenAcceptInviteBody_whenPost_thenDelegates() throws Exception {
		mockMvc.perform(post("/api/v1/auth/accept-invite")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"inv\",\"password\":\"password12345\"}"))
				.andExpect(status().isNoContent());
		verify(organizationUserService).acceptInvite("inv", "password12345");
	}

	@Test
	@DisplayName("given password too short when post login then bad request")
	void givenPasswordTooShort_whenPostLogin_thenBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"email\":\"u@test.local\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("given malformed email when post login then bad request")
	void givenMalformedEmail_whenPostLogin_thenBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
								"{\"organizationId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"email\":\"not-an-email\",\"password\":\"password12345\"}"))
				.andExpect(status().isBadRequest());
	}
}
