package com.mowercare.user;

/**
 * Cannot change role or similar admin actions on a deactivated account.
 */
public class UserDeactivatedManagementException extends RuntimeException {}
