package com.mowercare.model.response;

import java.time.Instant;
import java.util.UUID;

import com.mowercare.model.AccountStatus;
import com.mowercare.model.UserRole;

public record EmployeeUserResponse(
		UUID id, String email, UserRole role, AccountStatus accountStatus, Instant createdAt) {}
