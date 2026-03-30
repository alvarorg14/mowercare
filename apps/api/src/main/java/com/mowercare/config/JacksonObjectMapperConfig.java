package com.mowercare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Exposes a primary {@link ObjectMapper} for servlet filters and other components that are not wired
 * through MVC {@code HttpMessageConverter}s (Spring Boot 4 does not always register an injectable
 * {@code ObjectMapper} bean by default in this stack).
 */
@Configuration
public class JacksonObjectMapperConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
		return mapper;
	}
}
