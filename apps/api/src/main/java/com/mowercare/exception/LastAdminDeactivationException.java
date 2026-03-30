package com.mowercare.exception;

/**
 * Cannot deactivate the last active {@link com.mowercare.model.UserRole#ADMIN} in an organization.
 */
public class LastAdminDeactivationException extends RuntimeException {}
