/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.temporary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Process-local retry queue for temporary files whose delete raced a cloud
 * sync client or a Windows file handle.  A small adjacent marker makes the
 * failure observable to the next save/startup without retaining arbitrary
 * paths in a long-lived global collection.
 */
public final class TemporaryCleanupQueue {
	private static final Logger LOGGER = Logger.getLogger(TemporaryCleanupQueue.class.getName());
	private static final String MARKER_SUFFIX = ".delete";
	private static final Set<Path> PENDING = ConcurrentHashMap.newKeySet();

	private TemporaryCleanupQueue() {
	}

	/** Attempts deletion and records a bounded retry marker when it fails. */
	public static boolean deleteOrEnqueue(Path target) {
		if (target == null) return true;
		Path normalized = target.toAbsolutePath().normalize();
		try {
			boolean deleted = !Files.exists(normalized) || Files.deleteIfExists(normalized);
			if (deleted) {
				PENDING.remove(normalized);
				Files.deleteIfExists(marker(normalized));
			}
			return deleted;
		} catch (IOException exception) {
			PENDING.add(normalized);
			try {
				Files.writeString(marker(normalized), "retryAt=" + Instant.now() + "\n",
						StandardCharsets.UTF_8, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			} catch (IOException markerFailure) {
				LOGGER.log(Level.FINE, "Unable to persist temporary cleanup marker " + normalized, markerFailure);
			}
			return false;
		}
	}

	/** Retries queued paths below the supplied safe root. */
	public static int retry(Path root) {
		if (root == null) return 0;
		Path normalizedRoot = root.toAbsolutePath().normalize();
		int removed = 0;
		for (Path target : PENDING) {
			if (!target.startsWith(normalizedRoot)) continue;
			if (deleteOrEnqueue(target)) removed++;
		}
		// Recover markers left by a previous JVM.  Only recognizable temporary
		// names are considered; arbitrary user .delete files are never touched.
		try (Stream<Path> paths = Files.list(normalizedRoot)) {
			for (Path marker : paths.filter(path -> path.getFileName().toString().endsWith(MARKER_SUFFIX)).toList()) {
				String targetName = marker.getFileName().toString();
				targetName = targetName.substring(0, targetName.length() - MARKER_SUFFIX.length());
				if (!(targetName.endsWith(".tmp") || targetName.startsWith("microProject-temp-"))) continue;
				Path target = marker.resolveSibling(targetName);
				if (deleteOrEnqueue(target)) removed++;
			}
		} catch (IOException exception) {
			LOGGER.log(Level.FINE, "Unable to inspect temporary cleanup markers " + normalizedRoot, exception);
		}
		return removed;
	}

	static Path marker(Path target) {
		return target.resolveSibling(Objects.requireNonNull(target.getFileName()) + MARKER_SUFFIX);
	}
}
