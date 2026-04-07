package com.mowercare.user;

/**
 * Cannot deactivate the last active {@link com.mowercare.user.UserRole#ADMIN} in an organization.
 */
public class LastAdminDeactivationException extends RuntimeException {}
