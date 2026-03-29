package com.mowercare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mowercare.bootstrap")
public record BootstrapProperties(String token) {
}
