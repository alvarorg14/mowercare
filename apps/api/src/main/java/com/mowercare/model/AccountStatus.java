package com.mowercare.model;

/**
 * Employee account lifecycle for invite vs active login.
 */
public enum AccountStatus {
	/** Invited employee; must complete invite acceptance before login. */
	PENDING_INVITE,
	/** Normal employee account; may authenticate. */
	ACTIVE
}
