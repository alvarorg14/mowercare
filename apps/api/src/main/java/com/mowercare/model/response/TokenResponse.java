package com.mowercare.model.response;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
