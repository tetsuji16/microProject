package com.projectlibre1.util;

public final class TimeInputParser {
	private TimeInputParser() {
	}

	public static int parseHour(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return fallback;
		}
		int colon = trimmed.indexOf(':');
		if (colon >= 0) {
			trimmed = trimmed.substring(0, colon);
		}
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
