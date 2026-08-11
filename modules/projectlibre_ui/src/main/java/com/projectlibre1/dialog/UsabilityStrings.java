package com.projectlibre1.dialog;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Localized text shared by the modern usability dialogs. */
public final class UsabilityStrings {
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("com.projectlibre1.dialog.usability");
	private UsabilityStrings() { }
	public static String text(String key) {
		try { return BUNDLE.getString(key); }
		catch (MissingResourceException error) { return key; }
	}
}
