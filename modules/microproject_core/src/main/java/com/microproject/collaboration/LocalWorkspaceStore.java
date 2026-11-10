/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.collaboration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.temporary.TemporaryRoot;
import com.microproject.util.SafeFileReplace;

/**
 * Keeps one user's saved view layout on this computer after the shared sidecar
 * has been removed, so closing a project does not leave files beside it and the
 * layout still returns on reopen.  A layout is keyed by the project's path.
 */
final class LocalWorkspaceStore {
	private static final Logger logger = Logger.getLogger(LocalWorkspaceStore.class.getName());
	private static final String SUFFIX = ".workspace";
	private static final long RETENTION_MILLIS = Duration.ofDays(180L).toMillis();

	private final Path directory;
	private final Path file;

	LocalWorkspaceStore(File projectFile, String userKey) {
		directory = TemporaryRoot.defaultRoot().resolveSibling("workspaces");
		file = directory.resolve(key(projectFile, userKey) + SUFFIX);
	}

	void save(String payload) throws IOException {
		ensureFallbacksAgreeWithSidecar(payload);
		persist(payload);
		clearAllFallbacks();
	}

	void saveFallback(String payload, String sessionId, long generation, long savedAt) throws IOException {
		Files.createDirectories(directory);
		Path target = fallbackPath(sessionId, generation, savedAt);
		Path temporary = Files.createTempFile(directory, "workspace-fallback.", ".tmp");
		try {
			Files.write(temporary, Base64.getDecoder().decode(payload));
			SafeFileReplace.replaceDurably(temporary, target, true);
		} finally {
			Files.deleteIfExists(temporary);
		}
		pruneExpired();
	}

	private void persist(String payload) throws IOException {
		Files.createDirectories(directory);
		Path temporary = Files.createTempFile(directory, "workspace.", ".tmp");
		try {
			Files.write(temporary, Base64.getDecoder().decode(payload));
			SafeFileReplace.replaceDurably(temporary, file, true);
		} finally {
			Files.deleteIfExists(temporary);
		}
		pruneExpired();
	}

	String loadFallback() {
		try {
			List<Path> fallbacks = fallbackFiles();
			if (fallbacks.isEmpty()) return null;
			byte[] selected = Files.readAllBytes(fallbacks.get(0));
			for (int i = 1; i < fallbacks.size(); i++) {
				if (!java.util.Arrays.equals(selected, Files.readAllBytes(fallbacks.get(i)))) {
					logger.warning("Conflicting local workspace fallbacks have different contents; preserving all copies for " + file);
					return null;
				}
			}
			return Base64.getEncoder().encodeToString(selected);
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not read local workspace fallback for " + file, failure);
			return null;
		}
	}

	String loadFallbackComparedTo(String sidecarPayload) throws IOException {
		List<Path> fallbacks = fallbackFiles();
		if (fallbacks.isEmpty()) return null;
		byte[] selected = Files.readAllBytes(fallbacks.get(0));
		for (int i = 1; i < fallbacks.size(); i++) {
			if (!java.util.Arrays.equals(selected, Files.readAllBytes(fallbacks.get(i))))
				throw new IOException("Local workspace fallbacks contain divergent values");
		}
		if (sidecarPayload == null) return Base64.getEncoder().encodeToString(selected);
		byte[] sidecarBytes = Base64.getDecoder().decode(sidecarPayload);
		if (java.util.Arrays.equals(selected, sidecarBytes)) return null;
		throw new IOException("Local fallback and shared workspace contain divergent values; file clocks cannot resolve the conflict");
	}

	void ensureFallbacksAgreeWithSidecar(String sidecarPayload) throws IOException {
		List<Path> fallbacks = fallbackFiles();
		if (fallbacks.isEmpty()) return;
		byte[] sidecarBytes = Base64.getDecoder().decode(sidecarPayload);
		for (Path candidate : fallbacks) {
			if (!java.util.Arrays.equals(sidecarBytes, Files.readAllBytes(candidate))) {
				throw new IOException("A local workspace fallback differs from the sidecar; preserving both copies");
			}
		}
	}

	private List<Path> fallbackFiles() throws IOException {
		List<Path> result = new ArrayList<Path>();
		if (!Files.exists(directory)) return result;
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) if (parseFallback(candidate) != null) result.add(candidate);
		}
		return result;
	}


	boolean hasFallback(String sessionId) {
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) {
				FallbackKey key = parseFallback(candidate);
				if (key != null && key.sessionId.equals(sessionId)) return true;
			}
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not inspect local workspace fallback for " + file, failure);
		}
		return false;
	}

	boolean hasAnyFallback() {
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) if (parseFallback(candidate) != null) return true;
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not inspect local workspace fallbacks for " + file, failure);
		}
		return false;
	}

	List<Path> getFallbackFiles() {
		List<Path> result = new ArrayList<Path>();
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) if (parseFallback(candidate) != null) result.add(candidate);
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not list local workspace fallbacks for " + file, failure);
		}
		return List.copyOf(result);
	}

	void clearFallbacksForSessionThrough(String sessionId, long generation) throws IOException {
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) {
				FallbackKey key = parseFallback(candidate);
				if (key != null && key.sessionId.equals(sessionId) && key.generation <= generation) Files.deleteIfExists(candidate);
			}
		}
	}

	void clearFallbacksForSessionBefore(String sessionId, long generation) throws IOException {
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) {
				FallbackKey key = parseFallback(candidate);
				if (key != null && key.sessionId.equals(sessionId) && key.generation < generation) Files.deleteIfExists(candidate);
			}
		}
	}

	private void clearAllFallbacks() throws IOException {
		try (DirectoryStream<Path> fallbacks = Files.newDirectoryStream(directory, fallbackPattern())) {
			for (Path candidate : fallbacks) Files.deleteIfExists(candidate);
		}
	}

	private String fallbackPattern() {
		return file.getFileName().toString() + ".fallback.*";
	}

	private Path fallbackPath(String sessionId, long generation, long savedAt) {
		return directory.resolve(file.getFileName().toString() + ".fallback." + savedAt + "." + sessionId + "." + generation);
	}

	private record FallbackKey(long savedAt, String sessionId, long generation) { }

	private FallbackKey parseFallback(Path path) {
		try {
			String prefix = file.getFileName().toString() + ".fallback.";
			String name = path.getFileName().toString();
			if (!name.startsWith(prefix)) return null;
			String[] parts = name.substring(prefix.length()).split("\\.", 3);
			if (parts.length != 3 || parts[1].isBlank()) return null;
			return new FallbackKey(Long.parseLong(parts[0]), parts[1], Long.parseLong(parts[2]));
		} catch (RuntimeException malformed) {
			return null;
		}
	}

	/** Returns the saved layout as Base64, or {@code null} when there is none. */
	String load() {
		try {
			return Files.isRegularFile(file) ? Base64.getEncoder().encodeToString(Files.readAllBytes(file)) : null;
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not read saved workspace " + file, failure);
			return null;
		}
	}

	private void pruneExpired() {
		long cutoff = System.currentTimeMillis() - RETENTION_MILLIS;
		try (DirectoryStream<Path> saved = Files.newDirectoryStream(directory, "*" + SUFFIX)) {
			for (Path candidate : saved) {
				if (Files.getLastModifiedTime(candidate).toMillis() < cutoff) Files.deleteIfExists(candidate);
			}
		} catch (IOException failure) {
			logger.log(Level.FINE, "Could not prune saved workspaces in " + directory, failure);
		}
	}

	private static String key(File projectFile, String userKey) {
		String path;
		try {
			path = projectFile.getCanonicalPath();
		} catch (IOException failure) {
			path = projectFile.getAbsolutePath();
		}
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest((path + '\0' + userKey).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}
}
