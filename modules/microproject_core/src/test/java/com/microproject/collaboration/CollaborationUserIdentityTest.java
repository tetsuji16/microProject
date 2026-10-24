/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class CollaborationUserIdentityTest {
	@Test
	void trimsSystemIdentityAndMatchesUserAccounts() {
		String previous = System.getProperty("user.name");
		try {
			System.setProperty("user.name", "  alice  ");

			assertEquals("alice", CollaborationUserIdentity.currentUserKey());
			assertTrue(CollaborationUserIdentity.isCurrentUser("alice"));
			assertTrue(CollaborationUserIdentity.isCurrentUser(" alice "));
			assertFalse(CollaborationUserIdentity.isCurrentUser("bob"));
			assertFalse(CollaborationUserIdentity.isCurrentUser(null));
			assertFalse(CollaborationUserIdentity.isCurrentUser("  "));
		} finally {
			restoreUserName(previous);
		}
	}

	@Test
	void usesUnknownWhenSystemIdentityIsMissingOrBlank() {
		String previous = System.getProperty("user.name");
		try {
			System.clearProperty("user.name");
			assertEquals(CollaborationUserIdentity.UNKNOWN_USER_KEY, CollaborationUserIdentity.currentUserKey());
			assertFalse(CollaborationUserIdentity.isCurrentUser("unknown"));

			System.setProperty("user.name", "   ");
			assertEquals(CollaborationUserIdentity.UNKNOWN_USER_KEY, CollaborationUserIdentity.currentUserKey());
		} finally {
			restoreUserName(previous);
		}
	}

	private static void restoreUserName(String value) {
		if (value == null)
			System.clearProperty("user.name");
		else
			System.setProperty("user.name", value);
	}
}
