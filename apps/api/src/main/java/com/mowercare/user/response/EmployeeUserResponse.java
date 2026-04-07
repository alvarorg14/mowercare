package com.mowercare.user.response;

import java.time.Instant;
import java.util.UUID;

import com.mowercare.user.AccountStatus;
import com.mowercare.user.UserRole;

public record EmployeeUserResponse(
		UUID id, String email, UserRole role, AccountStatus accountStatus, Instant createdAt) {}
