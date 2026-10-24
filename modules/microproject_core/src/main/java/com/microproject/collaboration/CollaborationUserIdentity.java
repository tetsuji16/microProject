/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

/** Canonical local identity rules for collaboration metadata and user-owned data. */
public final class CollaborationUserIdentity {
	public static final String UNKNOWN_USER_KEY = "unknown";

	private CollaborationUserIdentity() {
	}

	public static String currentUserKey() {
		return normalize(System.getProperty("user.name"));
	}

	public static String normalize(String userKey) {
		if (userKey == null || userKey.trim().isEmpty())
			return UNKNOWN_USER_KEY;
		return userKey.trim();
	}

	public static boolean isCurrentUser(String userKey) {
		String currentUserKey = currentUserKey();
		return !UNKNOWN_USER_KEY.equals(currentUserKey)
				&& !UNKNOWN_USER_KEY.equals(normalize(userKey))
				&& currentUserKey.equals(normalize(userKey));
	}
}
