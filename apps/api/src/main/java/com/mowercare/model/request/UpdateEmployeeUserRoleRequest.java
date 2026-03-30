package com.mowercare.model.request;

import com.mowercare.model.UserRole;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeUserRoleRequest(@NotNull UserRole role) {}
