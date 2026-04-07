package com.mowercare.user;

/**
 * Cannot remove the last {@link com.mowercare.user.UserRole#ADMIN} in an organization.
 */
public class LastAdminRemovalException extends RuntimeException {}
