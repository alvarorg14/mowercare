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
	}
}
