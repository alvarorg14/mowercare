package com.mowercare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI mowercareOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("Mowercare API").version("v1"))
				.components(new Components()
						.addSecuritySchemes(
								"bearer-jwt",
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("Access JWT from POST /api/v1/auth/login")));
	}
}
