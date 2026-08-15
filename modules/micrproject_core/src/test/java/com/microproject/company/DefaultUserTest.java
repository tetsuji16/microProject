package com.microproject.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DefaultUserTest {
	@Test
	void defaultUserIsAnExplicitAnonymousLocalUser() {
		DefaultUser user = new DefaultUser();

		assertEquals(DefaultUser.DEFAULT_ID, user.getUniqueId());
		assertEquals(DefaultUser.DEFAULT_ID, user.getResourceId());
		assertEquals(DefaultUser.DEFAULT_NAME, user.getName());
		assertFalse(user.isAdministrator());
		assertFalse(user.isExternal());
	}
}
