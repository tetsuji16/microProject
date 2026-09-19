/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.microproject.preference.GlobalPreferences;
import com.microproject.util.Alert;
import com.microproject.util.VersionUtils;
import com.microproject.temporary.TemporaryCleanupQueue;

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
	private static final String STAGING_DIRECTORY_PROPERTY = "microproject.update.staging-directory";
	private static final String STAGING_FILE_PREFIX = "microProject-update-";
	private static final String MANIFEST_SUFFIX = ".manifest";
	private static final String RETRY_SUFFIX = ".delete";
	private static final Duration STAGED_FILE_TTL = Duration.ofDays(7);
	private static final Object STAGED_UPDATE_LOCK = new Object();
	private static StagedUpdate stagedUpdate;
	private static Path installerScheduledForApply;

	private UpdateChecker() {
	}

	/** Fire an asynchronous check; returns immediately. */
	public static void checkInBackground(GlobalPreferences preferences) {
		cleanupStaleArtifacts();
		if (preferences == null || !preferences.isCheckForUpdates()) return;
		if (!isUpdateCheckEnabled()) {
			logger.fine("Skipping update check for a local development build");
			return;
		}
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
		cleanupStaleArtifacts();
		if (callback == null || java.awt.GraphicsEnvironment.isHeadless()) return;
		if (!isWindows()) {
			String current = VersionUtils.getVersion();
			SwingUtilities.invokeLater(() -> callback.accept(new UpdateResult(current, null, null)));
			return;
		}
		if (!isUpdateCheckEnabled()) {
			logger.fine("Skipping update staging for a local development build");
			String current = VersionUtils.getVersion();
			SwingUtilities.invokeLater(() -> callback.accept(new UpdateResult(current, null, null)));
			return;
		}
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
		final StagedUpdate update = claimStagedUpdate();
		final boolean valid = update != null;
		if (!valid) {
			discardStagedUpdate();
			return false;
		}
		if (!isWindows()) {
			clearScheduledInstaller(update.installer());
			deleteUpdateFiles(update);
			return false;
		}
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				runInstaller(update, UpdateChecker::launchInstaller);
			}, "microProject-installer"));
		} catch (IllegalStateException | SecurityException e) {
			clearScheduledInstaller(update.installer());
			deleteUpdateFiles(update);
			logger.log(Level.WARNING, "Failed to register staged installer shutdown hook", e);
			return false;
		}
		System.exit(0);
		return true;
	}

	/** Test seam for installer launch and cleanup outcomes without terminating the JVM. */
	static boolean applyStagedUpdateForTest(InstallerLauncher launcher) {
		StagedUpdate update = claimStagedUpdate();
		if (update == null) return false;
		return runInstaller(update, launcher);
	}

	@FunctionalInterface
	interface InstallerLauncher {
		Process launch(Path installer) throws IOException;
	}

	private static Process launchInstaller(Path installer) throws IOException {
		return new ProcessBuilder("msiexec.exe", "/i", installer.toAbsolutePath().toString(),
				"/passive", "/norestart").start();
	}

	private static StagedUpdate claimStagedUpdate() {
		final StagedUpdate update;
		final boolean valid;
		synchronized (STAGED_UPDATE_LOCK) {
			update = stagedUpdate;
			valid = update != null && isValidStagedUpdate(update);
			if (valid) {
				stagedUpdate = null;
				installerScheduledForApply = update.installer();
			}
		}
		return valid ? update : null;
	}

	private static boolean runInstaller(StagedUpdate update, InstallerLauncher launcher) {
		try {
			Process process = launcher.launch(update.installer());
			if (process == null || !process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)) {
				logger.warning("Installer did not finish before shutdown cleanup deadline");
				return false;
			}
			if (process.exitValue() == 0) {
				deleteUpdateFiles(update);
				clearScheduledInstaller(update.installer());
				return true;
			}
			logger.warning("Installer exited with code " + process.exitValue() + "; retaining staged files for retry");
			clearScheduledInstaller(update.installer());
			synchronized (STAGED_UPDATE_LOCK) {
				if (stagedUpdate == null && isValidStagedUpdate(update)) stagedUpdate = update;
			}
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.log(Level.WARNING, "Interrupted while waiting for staged installer", e);
			return false;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to start staged installer", e);
			clearScheduledInstaller(update.installer());
			deleteUpdateFiles(update);
			return false;
	}
	}

	/** Removes a downloaded installer when the user declines or closes the prompt. */
	public static void discardStagedUpdate() {
		final StagedUpdate update;
		synchronized (STAGED_UPDATE_LOCK) {
			update = stagedUpdate;
			stagedUpdate = null;
		}
		if (update != null) deleteUpdateFiles(update);
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

	static Path stageInstaller(MsiAsset asset, String version) throws IOException {
		return stageInstaller(asset, version, stagingDirectory());
	}

	static Path stageInstaller(MsiAsset asset, String version, Path stagingDirectory) throws IOException {
		if (asset == null || version == null || version.isBlank() || version.indexOf('\n') >= 0 || version.indexOf('\r') >= 0) {
			throw new IllegalArgumentException("asset/version");
		}
		synchronized (STAGED_UPDATE_LOCK) {
			if (stagedUpdate != null && version.equals(stagedUpdate.version()) && isValidStagedUpdate(stagedUpdate)) {
				return stagedUpdate.installer();
			}
		}
		Files.createDirectories(stagingDirectory);
		Path part = Files.createTempFile(stagingDirectory, STAGING_FILE_PREFIX, ".msi.part");
		Path installer = part.resolveSibling(part.getFileName().toString().substring(0,
				part.getFileName().toString().length() - ".part".length()));
		Path manifest = manifestPath(installer);
		String checksum = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) URI.create(asset.url()).toURL().openConnection();
			try {
				connection.setConnectTimeout(TIMEOUT_MILLIS);
				connection.setReadTimeout(30000);
				connection.setInstanceFollowRedirects(true);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Installer download returned HTTP " + connection.getResponseCode());
				try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(part)) {
					copyDownload(in, out);
				}
			} finally {
				connection.disconnect();
			}
			if (Files.size(part) == 0) throw new IOException("Downloaded installer is empty");
			checksum = sha256(part);
			if (!asset.sha256().equalsIgnoreCase(checksum)) throw new IOException("Downloaded installer checksum mismatch");
			moveAtomically(part, installer);
			writeManifest(manifest, version, checksum, Instant.now());
			StagedUpdate previous;
			synchronized (STAGED_UPDATE_LOCK) {
				if (stagedUpdate != null && version.equals(stagedUpdate.version()) && isValidStagedUpdate(stagedUpdate)) {
					previous = new StagedUpdate(installer, manifest, version);
					deleteUpdateFiles(previous);
					return stagedUpdate.installer();
				}
				previous = stagedUpdate;
				stagedUpdate = new StagedUpdate(installer, manifest, version);
			}
			if (previous != null && !isProtectedArtifact(previous.installer())) deleteUpdateFiles(previous);
			return installer;
		} finally {
			deleteQuietly(part, "partial installer");
			if (!isCurrentStagedInstaller(installer)) deleteUpdateFiles(new StagedUpdate(installer, manifest, version));
		}
	}

	private static void copyDownload(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		int count;
		while ((count = in.read(buffer)) != -1) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedIOException("Installer download interrupted");
			}
			out.write(buffer, 0, count);
		}
	}

	private static void moveAtomically(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static boolean isCurrentStagedInstaller(Path installer) {
		synchronized (STAGED_UPDATE_LOCK) {
			return stagedUpdate != null && installer.equals(stagedUpdate.installer());
		}
	}

	private static boolean isValidStagedUpdate(StagedUpdate update) {
		if (update == null || !Files.isRegularFile(update.installer()) || !Files.isRegularFile(update.manifest())) return false;
		try {
			String manifest = Files.readString(update.manifest(), StandardCharsets.UTF_8);
			String version = manifestValue(manifest, "version");
			String checksum = manifestValue(manifest, "sha256");
			String createdAt = manifestValue(manifest, "createdAt");
			return update.version().equals(version) && checksum != null && checksum.matches("[0-9a-fA-F]{64}")
					&& createdAt != null && !createdAt.isBlank() && Instant.parse(createdAt) != null
					&& checksum.equalsIgnoreCase(sha256(update.installer()));
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}

	private static String manifestValue(String manifest, String key) {
		for (String line : manifest.split("\\R")) {
			if (line.startsWith(key + "=")) return line.substring(key.length() + 1);
		}
		return null;
	}

	private static Path manifestPath(Path installer) {
		return installer.resolveSibling(installer.getFileName() + MANIFEST_SUFFIX);
	}

	private static void writeManifest(Path manifest, String version, String checksum, Instant createdAt) throws IOException {
		Path partial = manifest.resolveSibling(manifest.getFileName() + ".part");
		try {
			String content = "version=" + version + "\n"
					+ "sha256=" + checksum + "\n"
					+ "createdAt=" + createdAt + "\n";
			Files.writeString(partial, content, StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
			moveAtomically(partial, manifest);
		} finally {
			deleteQuietly(partial, "partial update manifest");
		}
	}

	private static void deleteUpdateFiles(StagedUpdate update) {
		if (update == null) return;
		deleteQuietly(update.manifest(), "update manifest");
		deleteQuietly(update.installer(), "staged installer");
	}

	private static void clearScheduledInstaller(Path installer) {
		synchronized (STAGED_UPDATE_LOCK) {
			if (installer != null && installer.equals(installerScheduledForApply)) installerScheduledForApply = null;
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static Path stagingDirectory() {
		String override = System.getProperty(STAGING_DIRECTORY_PROPERTY);
		if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize();
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "microProject", "updates");
		String userHome = System.getProperty("user.home", ".");
		return Path.of(userHome, "AppData", "Local", "microProject", "updates");
	}

	private static void cleanupStaleArtifacts() {
		cleanupStaleArtifacts(stagingDirectory(), Instant.now(), STAGED_FILE_TTL);
	}

	static void cleanupStaleArtifacts(Path directory, Instant now, Duration ttl) {
		if (directory == null || now == null || ttl == null || ttl.isNegative() || ttl.isZero()) return;
		if (!Files.isDirectory(directory)) return;
		Instant cutoff = now.minus(ttl);
		try (Stream<Path> files = Files.list(directory)) {
			files.filter(UpdateChecker::isUpdateArtifact)
					.filter(path -> !isProtectedArtifact(path))
					.forEach(path -> {
						try {
							if (path.getFileName().toString().endsWith(RETRY_SUFFIX)) {
								Path target = path.resolveSibling(path.getFileName().toString()
										.substring(0, path.getFileName().toString().length() - RETRY_SUFFIX.length()));
								deleteQuietly(target, "retry update artifact");
								if (!Files.exists(target)) Files.deleteIfExists(path);
							} else if (Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) {
								deleteQuietly(path, "stale update artifact");
							}
						} catch (IOException e) {
							logger.log(Level.FINE, "Failed to remove stale update artifact " + path, e);
						}
					});
		} catch (IOException e) {
			logger.log(Level.FINE, "Failed to inspect update staging directory " + directory, e);
		}
	}

	private static boolean isUpdateArtifact(Path path) {
		String name = path.getFileName().toString();
		if (name.endsWith(RETRY_SUFFIX)) {
			name = name.substring(0, name.length() - RETRY_SUFFIX.length());
		}
		return name.startsWith(STAGING_FILE_PREFIX)
				&& (name.endsWith(".msi") || name.endsWith(".msi.part")
						|| name.endsWith(MANIFEST_SUFFIX) || name.endsWith(MANIFEST_SUFFIX + ".part")
						|| name.endsWith(RETRY_SUFFIX));
	}

	private static boolean isProtectedArtifact(Path path) {
		synchronized (STAGED_UPDATE_LOCK) {
			return (stagedUpdate != null && (path.equals(stagedUpdate.installer()) || path.equals(stagedUpdate.manifest())))
					|| (installerScheduledForApply != null
							&& (path.equals(installerScheduledForApply) || path.equals(manifestPath(installerScheduledForApply))));
		}
	}

	private static void deleteQuietly(Path path, String description) {
		if (path == null) return;
		if (!TemporaryCleanupQueue.deleteOrEnqueue(path))
			logger.log(Level.FINE, "Failed to remove " + description + " " + path);
	}

	private record StagedUpdate(Path installer, Path manifest, String version) {
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
		try {
			java.awt.Desktop.getDesktop().browse(URI.create(RELEASES_URL));
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to open releases page", e);
		}
	}
}
