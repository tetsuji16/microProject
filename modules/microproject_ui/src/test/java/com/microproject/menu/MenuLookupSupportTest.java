/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.MissingResourceException;

import org.junit.jupiter.api.Test;

class MenuLookupSupportTest {
	@Test
	void actionRegistrationUsesDeclaredAliasOrFallsBackToCanonicalActionId() {
		Map<String, String> strings = Map.of("RibbonLevelAll.action", "LevelAllAction");
		StringLookup lookup = key -> {
			String value = strings.get(key);
			if (value == null) throw new MissingResourceException("missing", "test", key);
			return value;
		};

		assertEquals("LevelAllAction", MenuLookupSupport.getActionStringOrId(
			lookup, "RibbonLevelAll", ExtMenuFactory.ACTION_SUFFIX));
		assertEquals("LevelAll", MenuLookupSupport.getActionStringOrId(
			lookup, "LevelAll", ExtMenuFactory.ACTION_SUFFIX));
	}
}
