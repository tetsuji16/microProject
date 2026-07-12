package com.projectlibre1.menu;

import java.util.MissingResourceException;

@FunctionalInterface
interface StringLookup {
	String get(String key) throws MissingResourceException;
}

final class MenuLookupSupport {
	private MenuLookupSupport() {
	}

	static String getOrNull(StringLookup lookup, String key) {
		try {
			return lookup.get(key);
		} catch (MissingResourceException ex) {
			return null;
		}
	}

	static String getActionStringFromId(StringLookup lookup, String id, String suffix) {
		return getOrNull(lookup, id + suffix);
	}
}
