package com.mowercare.exception;

/** Thrown when {@code scope} on issue list is not {@code open}, {@code all}, or {@code mine}. */
public class InvalidScopeException extends RuntimeException {

	private final String scopeValue;

	public InvalidScopeException(String scopeValue) {
		super("Invalid scope: " + scopeValue);
		this.scopeValue = scopeValue;
	}

	public String getScopeValue() {
		return scopeValue;
	}
}
