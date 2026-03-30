package com.mowercare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.mowercare.security.AccountStatusVerificationFilter;

/**
 * JWT bearer enforcement on protected routes; public auth, bootstrap, and OpenAPI paths.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ApiAuthenticationEntryPoint authenticationEntryPoint,
			AccountStatusVerificationFilter accountStatusVerificationFilter)
			throws Exception {
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(
						"/api/v1/auth/**",
						"/api/v1/bootstrap/**",
						"/v3/api-docs",
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html")
				.permitAll()
				.anyRequest()
				.authenticated());
		http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()).authenticationEntryPoint(authenticationEntryPoint));
		http.csrf(csrf -> csrf.disable());
		http.addFilterAfter(accountStatusVerificationFilter, BearerTokenAuthenticationFilter.class);
		return http.build();
	}
}
