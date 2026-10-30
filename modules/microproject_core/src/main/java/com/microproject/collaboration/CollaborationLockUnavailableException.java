/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.collaboration;

/** Raised when a collaboration metadata lock cannot be acquired promptly on the UI thread. */
public final class CollaborationLockUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CollaborationLockUnavailableException(String message) {
		super(message);
	}

	public CollaborationLockUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
