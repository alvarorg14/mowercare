package com.mowercare.auth.response;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
