package com.mowercare.exception;

/**
 * Cannot remove the last {@link com.mowercare.model.UserRole#ADMIN} in an organization.
 */
public class LastAdminRemovalException extends RuntimeException {}
