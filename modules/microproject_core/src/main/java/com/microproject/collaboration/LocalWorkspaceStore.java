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
import java.util.HexFormat;
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
		Files.createDirectories(directory);
		Path temporary = Files.createTempFile(directory, "workspace.", ".tmp");
		try {
			Files.write(temporary, Base64.getDecoder().decode(payload));
			SafeFileReplace.moveAtomically(temporary, file, true);
		} finally {
			Files.deleteIfExists(temporary);
		}
		pruneExpired();
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
