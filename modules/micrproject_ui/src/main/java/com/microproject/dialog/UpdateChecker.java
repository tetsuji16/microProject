/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

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
	private static final int MAX_RELEASE_JSON_BYTES = 1024 * 1024;
	private static final Object STAGED_UPDATE_LOCK = new Object();
	private static Path stagedInstaller;
	private static String stagedVersion;

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

	/**
	 * Checks for a release when the user opens the About dialog and stages the
	 * Windows installer without blocking the EDT. The installer is not applied
	 * until the user agrees to restart the application.
	 */
	public static void checkAndStageInBackground(Consumer<UpdateResult> callback) {
		if (callback == null || java.awt.GraphicsEnvironment.isHeadless()) return;
		Thread thread = new Thread("about-update-check") {
			@Override
			public void run() {
				UpdateResult result;
				try {
					String current = VersionUtils.getVersion();
					String json = fetchLatestReleaseJson();
					String latest = extractTagName(json);
					if (latest == null || current == null || !isNewer(latest.trim(), current.trim())) {
						result = new UpdateResult(current, latest, null);
					} else {
						MsiAsset asset = extractMsiAsset(json);
						if (asset == null) {
							result = new UpdateResult(current, latest, null);
						} else {
							Path installer = stageInstaller(asset, latest.trim());
							result = new UpdateResult(current, latest.trim(), installer);
						}
					}
				} catch (Exception e) {
					logger.log(Level.FINE, "Update staging skipped", e);
					result = new UpdateResult(VersionUtils.getVersion(), null, null);
				}
				final UpdateResult completed = result;
				SwingUtilities.invokeLater(() -> callback.accept(completed));
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	/** Applies the staged MSI after the current process exits. */
	public static boolean applyStagedUpdate() {
		final Path installer;
		synchronized (STAGED_UPDATE_LOCK) {
			installer = stagedInstaller;
		}
		if (installer == null || !Files.isRegularFile(installer)) return false;
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return false;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				new ProcessBuilder("msiexec.exe", "/i", installer.toAbsolutePath().toString(),
						"/passive", "/norestart").start();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to start staged installer", e);
			}
		}, "microProject-installer"));
		System.exit(0);
		return true;
	}

	static String extractMsiDownloadUrl(String json) {
		MsiAsset asset = extractMsiAsset(json);
		return asset == null ? null : asset.url();
	}

	static MsiAsset extractMsiAsset(String json) {
		if (json == null) return null;
		Matcher assets = Pattern.compile("\\{[^{}]*\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+\\.msi)\\\"[^{}]*\\}", Pattern.DOTALL).matcher(json);
		MsiAsset fallback = null;
		while (assets.find()) {
			String object = assets.group();
			Matcher digest = Pattern.compile("\\\"digest\\\"\\s*:\\s*\\\"sha256:([0-9a-fA-F]{64})\\\"").matcher(object);
			if (!digest.find()) continue;
			MsiAsset asset = new MsiAsset(assets.group(1), digest.group(1));
			if (asset.url().contains("microProject-latest.msi")) return asset;
			if (fallback == null) fallback = asset;
		}
		return fallback;
	}

	static String fetchLatestReleaseJson() throws IOException {
		return fetchLatestReleaseJson(URI.create(API_URL).toURL(), TIMEOUT_MILLIS);
	}

	static String fetchLatestReleaseJson(URL endpoint, int timeoutMillis) throws IOException {
		HttpURLConnection connection = openReleaseConnection(endpoint, timeoutMillis);
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new IOException("GitHub Releases API returned HTTP " + responseCode);
			}
			try (InputStream in = connection.getInputStream()) {
				return readReleaseJson(in);
			}
		} finally {
			connection.disconnect();
		}
	}

	private static HttpURLConnection openReleaseConnection(URL endpoint, int timeoutMillis) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
		connection.setConnectTimeout(timeoutMillis);
		connection.setReadTimeout(timeoutMillis);
		connection.setRequestProperty("Accept", "application/vnd.github+json");
		connection.setRequestProperty("User-Agent", "microProject-update-checker");
		return connection;
	}

	private static String readReleaseJson(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int total = 0;
		int count;
		while ((count = input.read(buffer)) != -1) {
			if (count > MAX_RELEASE_JSON_BYTES - total) {
				throw new IOException("GitHub Releases API response exceeds " + MAX_RELEASE_JSON_BYTES + " bytes");
			}
			output.write(buffer, 0, count);
			total += count;
		}
		return output.toString(StandardCharsets.UTF_8);
	}

	private static Path stageInstaller(MsiAsset asset, String version) throws IOException {
		synchronized (STAGED_UPDATE_LOCK) {
			if (version.equals(stagedVersion) && stagedInstaller != null && Files.isRegularFile(stagedInstaller)) return stagedInstaller;
		}
		Path part = Files.createTempFile("microProject-update-", ".msi.part");
		Path installer = Files.createTempFile("microProject-update-", ".msi");
		try {
			HttpURLConnection connection = (HttpURLConnection) URI.create(asset.url()).toURL().openConnection();
			try {
				connection.setConnectTimeout(TIMEOUT_MILLIS);
				connection.setReadTimeout(30000);
				connection.setInstanceFollowRedirects(true);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Installer download returned HTTP " + connection.getResponseCode());
				try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(part)) {
					in.transferTo(out);
				}
			} finally {
				connection.disconnect();
			}
			if (Files.size(part) == 0) throw new IOException("Downloaded installer is empty");
			if (!asset.sha256().equalsIgnoreCase(sha256(part))) throw new IOException("Downloaded installer checksum mismatch");
			Files.move(part, installer, StandardCopyOption.REPLACE_EXISTING);
			synchronized (STAGED_UPDATE_LOCK) {
				stagedInstaller = installer;
				stagedVersion = version;
			}
			return installer;
		} finally {
			Files.deleteIfExists(part);
		}
	}

	private static String sha256(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			in.transferTo(new OutputStream() {
				@Override public void write(int b) { digest.update((byte) b); }
				@Override public void write(byte[] b, int off, int len) { digest.update(b, off, len); }
			});
			StringBuilder result = new StringBuilder(64);
			for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value));
			return result.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 is unavailable", e);
		}
	}

	record MsiAsset(String url, String sha256) {
	}

	public record UpdateResult(String currentVersion, String latestVersion, Path stagedInstaller) {
		public boolean updateAvailable() { return stagedInstaller != null; }
	}

	/** Returns the tag_name of the latest GitHub release, or null. */
	static String fetchLatestVersion() throws IOException {
		return extractTagName(fetchLatestReleaseJson());
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
		return VersionUtils.compareVersions(candidate, current) > 0;
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
