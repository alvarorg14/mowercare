package com.mowercare.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Invite token TTL for employee provisioning (no email delivery in MVP).
 */
@ConfigurationProperties(prefix = "mowercare.invite")
public record InviteProperties(Duration tokenTtl) {

	public InviteProperties {
		tokenTtl = tokenTtl != null ? tokenTtl : Duration.ofDays(14);
	}
}
