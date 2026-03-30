package com.mowercare.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT signing and TTL configuration. Secrets come from the environment — never commit values.
 *
 * @param issuer Issuer claim for access JWTs
 * @param secret Raw secret bytes as UTF-8 string; must be at least 32 bytes for HS256
 * @param accessTokenTtl Access token lifetime
 * @param refreshTokenTtl Refresh token lifetime (opaque token row expiry)
 */
@ConfigurationProperties(prefix = "mowercare.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {}
