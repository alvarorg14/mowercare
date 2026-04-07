package com.mowercare.user.response;

import java.util.UUID;

import com.mowercare.user.AccountStatus;
import com.mowercare.user.UserRole;

/**
 * Minimal employee row for issue assignment pickers. Only {@link AccountStatus#ACTIVE} members are listed;
 * {@link AccountStatus#PENDING_INVITE} and {@link AccountStatus#DEACTIVATED} are excluded.
 */
public record AssignableUserResponse(UUID id, String email, UserRole role, AccountStatus accountStatus) {}
