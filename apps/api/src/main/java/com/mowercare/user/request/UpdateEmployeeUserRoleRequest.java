package com.mowercare.user.request;

import com.mowercare.user.UserRole;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeUserRoleRequest(@NotNull UserRole role) {}
