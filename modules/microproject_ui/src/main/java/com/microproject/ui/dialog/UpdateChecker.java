/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.dialog;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.microproject.preference.GlobalPreferences;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;
import com.microproject.util.VersionUtils;
import com.microproject.util.temporary.TemporaryCleanupQueue;

/**
 * Lightweight update checker (#338, plan D). Queries the GitHub Releases
 * latest endpoint on a background thread at startup and offers to open the
 * releases page when a newer version is published. Never blocks the EDT,
 * silently skips when offline/headless, and can be disabled from
 * Preferences ({@link GlobalPreferences#isCheckForUpdates()}).
 * <p>
 * The application never downloads or installs an update itself. In the
 * Microsoft Store distribution the Store updates the app, so no network
 * request is made and the manual check opens the Store page instead.
 */
public final class UpdateChecker {

	private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
	static final String RELEASES_URL = "https://github.com/tetsuji16/microProject/releases/latest";
	static final String STORE_PRODUCT_URI = "ms-windows-store://pdp/?ProductId=9N89VPNDHP93";
	private static final String DISTRIBUTION_PROPERTY = "microproject.distribution";
	private static final String API_URL = "https://api.github.com/repos/tetsuji16/microProject/releases/latest";
	private static final int TIMEOUT_MILLIS = 5000;
	private static final int MAX_RELEASE_JSON_BYTES = 1024 * 1024;
	private static final String STAGING_DIRECTORY_PROPERTY = "microproject.update.staging-directory";
	/** Name prefix of every file earlier versions wrote to the update folder. */
	private static final String STAGED_FILE_PREFIX = "microProject-update-";

	/** Shared HTTP client with connection timeout and manual redirect handling. */
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();

	private UpdateChecker() {
	}

	/** Whether this installation was delivered by the Microsoft Store, which updates the app itself. */
	public static boolean isStoreDistribution() {
		return "store".equalsIgnoreCase(System.getProperty(DISTRIBUTION_PROPERTY));
	}

	/** Fire an asynchronous check; returns immediately. */
	public static void checkInBackground(GlobalPreferences preferences) {
		checkInBackground(preferences, isStoreDistribution(), stagingDirectory());
	}

	/**
	 * Testable variant. Returns the started check thread, or null when no network
	 * request is made (Store distribution, preference off, development build, headless).
	 */
	static Thread checkInBackground(GlobalPreferences preferences, boolean store, Path leftoverDirectory) {
		cleanupStaleArtifacts(leftoverDirectory);
		if (store) return null;
		if (preferences == null || !preferences.isCheckForUpdates()) return null;
		if (!isUpdateCheckEnabled()) {
			logger.fine("Skipping update check for a local development build");
			return null;
		}
		if (java.awt.GraphicsEnvironment.isHeadless()) return null;
		return Thread.ofPlatform().name("update-check").daemon(true).start(() -> {
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
		});
	}

	/**
	 * User-requested check. Ignores the startup preference and always reports an outcome.
	 * In the Store distribution it only opens the Store page.
	 */
	public static void checkNow() {
		if (isStoreDistribution()) {
			BrowserControl.displayURL(STORE_PRODUCT_URI);
			return;
		}
		if (java.awt.GraphicsEnvironment.isHeadless()) return;
		if (!isUpdateCheckEnabled()) {
			SwingUtilities.invokeLater(() -> Alert.warn(UsabilityStrings.text("update.developmentBuild")));
			return;
		}
		Thread.ofPlatform().name("update-check-now").daemon(true).start(() -> {
			final String current = VersionUtils.getVersion();
			String latest = null;
			try {
				latest = fetchLatestVersion();
			} catch (Exception e) {
				logger.log(Level.FINE, "Manual update check failed", e);
			}
			final String found = latest == null ? null : latest.trim();
			SwingUtilities.invokeLater(() -> {
				if (found == null) {
					Alert.warn(UsabilityStrings.text("update.checkFailed"));
				} else if (current == null || isNewer(found, current.trim())) {
					offerUpgrade(current, found);
				} else {
					Alert.warn(UsabilityStrings.text("update.upToDate").replace("{0}", current));
				}
			});
		});
	}

	static String fetchLatestReleaseJson() throws IOException {
		return fetchLatestReleaseJson(URI.create(API_URL), TIMEOUT_MILLIS);
	}

	static String fetchLatestReleaseJson(URI endpoint, int timeoutMillis) throws IOException {
		HttpRequest request = HttpRequest.newBuilder(endpoint)
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "microProject-update-checker")
				.timeout(Duration.ofMillis(timeoutMillis))
				.GET()
				.build();
		HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.limiting(
				HttpResponse.BodyHandlers.ofByteArray(), MAX_RELEASE_JSON_BYTES), Duration.ofMillis(timeoutMillis));
		if (response.statusCode() != 200) {
			throw new IOException("GitHub Releases API returned HTTP " + response.statusCode());
		}
		return new String(response.body(), StandardCharsets.UTF_8);
	}

	/**
	 * Sends an HTTP request with a deadline, converting common exceptions to IOExceptions.
	 */
	private static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler, Duration deadline) throws IOException {
		java.util.concurrent.CompletableFuture<HttpResponse<T>> future = HTTP.sendAsync(request, handler);
		try {
			return future.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new java.net.http.HttpTimeoutException("Request timed out after " + deadline);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			throw new IOException("Request failed", cause);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedIOException("Request interrupted");
		}
	}

	/** Folder where earlier versions staged installer downloads. */
	private static Path stagingDirectory() {
		String override = System.getProperty(STAGING_DIRECTORY_PROPERTY);
		if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize();
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "microProject", "updates");
		String userHome = System.getProperty("user.home", ".");
		return Path.of(userHome, "AppData", "Local", "microProject", "updates");
	}

	/**
	 * Deletes the installer files earlier versions left in the update folder,
	 * regardless of age. Only files named with the staging prefix are touched.
	 * This version never writes there.
	 */
	static void cleanupStaleArtifacts(Path directory) {
		if (directory == null || !Files.isDirectory(directory)) return;
		try (var files = Files.list(directory)) {
			files.filter(path -> path.getFileName().toString().startsWith(STAGED_FILE_PREFIX))
					.filter(Files::isRegularFile)
					.forEach(path -> {
						if (!TemporaryCleanupQueue.deleteOrEnqueue(path))
							logger.log(Level.FINE, "Failed to remove leftover update file " + path);
					});
		} catch (IOException e) {
			logger.log(Level.FINE, "Failed to inspect update folder " + directory, e);
		}
	}

	/** Returns the tag_name of the latest GitHub release, or null. */
	static String fetchLatestVersion() throws IOException {
		return extractTagName(fetchLatestReleaseJson());
	}

	static String extractTagName(String json) {
		Matcher matcher = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
		return matcher.find() ? matcher.group(1) : null;
	}

	static boolean isUpdateCheckEnabled() {
		return !VersionUtils.isDevelopmentBuild();
	}

	/**
	 * Compares dotted numeric versions (v-prefix and build suffix tolerated),
	 * e.g. v0.0.23 > 0.0.9, 0.0.23.140 > 0.0.23.
	 */
	static boolean isNewer(String candidate, String current) {
		return VersionUtils.compareVersions(candidate, current) > 0;
	}

	private static void offerUpgrade(String current, String latest) {
		String message = UsabilityStrings.text("update.available")
				.replace("{0}", current == null ? "" : current)
				.replace("{1}", latest);
		if (!Alert.okCancel(message)) return;
		BrowserControl.displayURL(RELEASES_URL);
	}
}
