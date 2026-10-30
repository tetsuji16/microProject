/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

/** Escapes text for use in XML and HTML markup. */
public final class MarkupEscape {
	private MarkupEscape() {
	}

	/** Escapes {@code & < >} and the double quote; {@code null} becomes the empty string. */
	public static String escape(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
