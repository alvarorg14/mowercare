package com.mowercare.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

	protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@DynamicPropertySource
	static void registerDatasource(DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("mowercare.jwt.secret", () -> "test-jwt-secret-must-be-at-least-32-bytes-long!");
		registry.add("mowercare.jwt.issuer", () -> "https://test.mowercare.local");
		registry.add("mowercare.jwt.access-token-ttl", () -> "PT15M");
		registry.add("mowercare.jwt.refresh-token-ttl", () -> "P7D");
	}
}
