package com.mowercare.user.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mowercare.user.AccountStatus;
import com.mowercare.user.UserRole;

/**
 * Result of admin create/invite. {@code inviteToken} is returned once for pending invites (admin channel only).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateEmployeeUserResponse(
		UUID id, String email, UserRole role, AccountStatus accountStatus, String inviteToken) {}
