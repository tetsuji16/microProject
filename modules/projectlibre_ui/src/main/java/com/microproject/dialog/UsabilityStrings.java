package com.microproject.dialog;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Localized text shared by the modern usability dialogs. */
public final class UsabilityStrings {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("com.microproject.dialog.usability");
	private UsabilityStrings() { }
	public static String text(String key) {
		try { return BUNDLE.getString(key); }
		catch (MissingResourceException error) { return key; }
	}
}
