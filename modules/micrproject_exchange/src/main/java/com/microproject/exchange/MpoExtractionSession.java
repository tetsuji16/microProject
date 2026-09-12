/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Owns one MPO embedded-project extraction lifetime.
 *
 * <p>The application layer may pass {@code TemporaryWorkspace.root()} to
 * {@link #open(Path)}.  This keeps exchange independent of application while
 * making ownership explicit: the importer must close the session after all
 * embedded projects have been closed.</p>
 */
public final class MpoExtractionSession implements AutoCloseable {
	private static final String ROOT_PROPERTY = "microproject.temporary-root";
	private static final String DIRECTORY_PREFIX = "microProject-temp-mpof-";
	private static final String MANIFEST_SUFFIX = ".manifest";
	private static final String RETRY_SUFFIX = ".delete";
	private final Path directory;
	private final Path manifest;
	private final Instant createdAt;
	private final String instanceId;
	private boolean closed;

	private MpoExtractionSession(Path directory) {
		this.directory = directory;
		this.manifest = directory.resolveSibling(directory.getFileName() + MANIFEST_SUFFIX);
		this.createdAt = Instant.now();
		this.instanceId = UUID.randomUUID().toString();
	}

	/** Returns the same per-user root convention used by TemporaryWorkspace. */
	public static Path defaultWorkspaceRoot() {
		String override = System.getProperty(ROOT_PROPERTY);
		if (override != null && !override.isBlank()) return Path.of(override);
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "microProject", "temp");
		return Path.of(System.getProperty("user.home", "."), "AppData", "Local", "microProject", "temp");
	}

	/** Creates an extraction directory below the supplied application workspace root. */
	public static MpoExtractionSession open(Path workspaceRoot) throws IOException {
		Path root = Objects.requireNonNull(workspaceRoot, "workspaceRoot").toAbsolutePath().normalize();
		Files.createDirectories(root);
		Path directory = Files.createTempDirectory(root, DIRECTORY_PREFIX);
		MpoExtractionSession session = new MpoExtractionSession(directory);
		Path part = session.manifest.resolveSibling(session.manifest.getFileName() + ".part");
		try {
			Files.writeString(part, session.manifestContents("open"),
					java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			try {
				Files.move(part, session.manifest, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
				Files.move(part, session.manifest, StandardCopyOption.REPLACE_EXISTING);
			}
			return session;
		} catch (IOException | RuntimeException exception) {
			deleteWithRetry(part);
			session.deleteTree(directory);
			deleteWithRetry(session.manifest);
			throw exception;
		}
	}

	/** The session-owned directory in which extracted entries may be opened. */
	public Path directory() {
		return directory;
	}

	/** Extracts one regular entry while rejecting path traversal and collisions. */
	public synchronized Path extract(String entryName, byte[] contents) throws IOException {
		ensureOpen();
		Objects.requireNonNull(entryName, "entryName");
		Objects.requireNonNull(contents, "contents");
		Path entry = Path.of(entryName);
		if (entry.isAbsolute() || entry.normalize().startsWith(".."))
			throw new IOException("Invalid embedded entry name: " + entryName);
		Path target = directory.resolve(entry.getFileName()).normalize();
		if (!target.startsWith(directory) || target.equals(directory))
			throw new IOException("Invalid embedded entry name: " + entryName);
		Files.write(target, contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		return target;
	}

	private void ensureOpen() {
		if (closed) throw new IllegalStateException("MPO extraction session is closed");
	}

	@Override
	public synchronized void close() {
		if (closed) return;
		closed = true;
		markClosed();
		deleteTree(directory);
		deleteWithRetry(manifest);
	}

	private void markClosed() {
		Path part = manifest.resolveSibling(manifest.getFileName() + ".part");
		try {
			Files.writeString(part, manifestContents("closed"), java.nio.charset.StandardCharsets.UTF_8,
					StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			try {
				Files.move(part, manifest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
				Files.move(part, manifest, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			// Deletion still proceeds. If the manifest remains open, its process
			// identity prevents this process from reclaiming it; a later process can
			// safely reclaim it after the identity no longer matches.
			deleteWithRetry(part);
		}
	}

	private String manifestContents(String state) {
		StringBuilder contents = new StringBuilder();
		contents.append("createdAt=").append(createdAt).append('\n');
		contents.append("purpose=mpof-extraction\n");
		contents.append("instanceId=").append(instanceId).append('\n');
		contents.append("processId=").append(ProcessHandle.current().pid()).append('\n');
		ProcessHandle.current().info().startInstant()
				.ifPresent(start -> contents.append("processStart=").append(start).append('\n'));
		contents.append("state=").append(state).append('\n');
		return contents.toString();
	}

	private void deleteTree(Path target) {
		try (Stream<Path> paths = Files.walk(target)) {
			paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> deleteWithRetry(path));
		} catch (IOException exception) {
			deleteWithRetry(target);
		}
	}

	private static void deleteWithRetry(Path target) {
		if (target == null) return;
		Path marker = target.resolveSibling(target.getFileName() + RETRY_SUFFIX);
		try {
			if (!Files.deleteIfExists(target)) {
				Files.deleteIfExists(marker);
				return;
			}
			Files.deleteIfExists(marker);
		} catch (IOException exception) {
			try {
				Files.writeString(marker, "retryAt=" + Instant.now() + "\n",
						java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			} catch (IOException ignored) {
				// The next startup still scans the managed directory when possible.
			}
		}
	}
}
