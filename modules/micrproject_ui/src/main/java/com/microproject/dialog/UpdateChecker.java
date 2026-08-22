/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.microproject.preference.GlobalPreferences;
import com.microproject.util.Alert;
import com.microproject.util.VersionUtils;

/**
 * Lightweight update checker (#338, plan D). Queries the GitHub Releases
 * latest endpoint on a background thread at startup and offers to open the
 * releases page when a newer version is published. Never blocks the EDT,
 * silently skips when offline/headless, and can be disabled from
 * Preferences ({@link GlobalPreferences#isCheckForUpdates()}).
 */
public final class UpdateChecker {
	private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
	static final String RELEASES_URL = "https://github.com/tetsuji16/ProjectLibre/releases/latest";
	private static final String API_URL = "https://api.github.com/repos/tetsuji16/ProjectLibre/releases/latest";
	private static final int TIMEOUT_MILLIS = 5000;

	private UpdateChecker() {
	}

	/** Fire an asynchronous check; returns immediately. */
	public static void checkInBackground(GlobalPreferences preferences) {
		if (preferences == null || !preferences.isCheckForUpdates()) return;
		if (java.awt.GraphicsEnvironment.isHeadless()) return;
		Thread thread = new Thread("update-check") {
			@Override
			public void run() {
				try {
					String latest = fetchLatestVersion();
					if (latest == null) return;
					final String current = VersionUtils.getVersion();
					if (current == null || isNewer(latest.trim(), current.trim())) {
						SwingUtilities.invokeLater(() -> offerUpgrade(current, latest.trim()));
					}
				} catch (Exception e) {
					logger.log(Level.FINE, "Update check skipped", e);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	/** Returns the tag_name of the latest GitHub release, or null. */
	static String fetchLatestVersion() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
		try {
			connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS);
			connection.setRequestProperty("Accept", "application/vnd.github+json");
			connection.setRequestProperty("User-Agent", "microProject-update-checker");
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
			try (InputStream in = connection.getInputStream()) {
				return extractTagName(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
			}
		} finally {
			connection.disconnect();
		}
	}

	static String extractTagName(String json) {
		Matcher matcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
		return matcher.find() ? matcher.group(1) : null;
	}

	/**
	 * Compares dotted numeric versions (v-prefix and build suffix tolerated),
	 * e.g. v0.0.23 > 0.0.9, 0.0.23.140 > 0.0.23.
	 */
	static boolean isNewer(String candidate, String current) {
		int[] a = parse(candidate);
		int[] b = parse(current);
		if (a == null || b == null) return false;
		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			int left = i < a.length ? a[i] : 0;
			int right = i < b.length ? b[i] : 0;
			if (left != right) return left > right;
		}
		return false;
	}

	private static int[] parse(String version) {
		if (version == null) return null;
		String normalized = version.trim().toLowerCase(Locale.ROOT);
		if (normalized.startsWith("v")) normalized = normalized.substring(1);
		String[] parts = normalized.split("[.\\-]");
		int[] numbers = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				numbers[i] = Integer.parseInt(parts[i]);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return numbers;
	}

	private static void offerUpgrade(String current, String latest) {
		String message = UsabilityStrings.text("update.available")
				.replace("{0}", current == null ? "" : current)
				.replace("{1}", latest);
		if (!Alert.okCancel(message)) return;
		try {
			java.awt.Desktop.getDesktop().browse(URI.create(RELEASES_URL));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to open releases page", e);
		}
	}
}
