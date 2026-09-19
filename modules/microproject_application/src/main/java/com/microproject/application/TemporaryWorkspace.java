/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.microproject.temporary.TemporaryCleanupQueue;

/**
 * Owns process-scoped temporary artifacts outside the project directory.
 * Artifacts have a small manifest so startup cleanup can safely distinguish
 * this application's files from unrelated files in the user profile.
 */
public final class TemporaryWorkspace implements AutoCloseable {
	public static final Duration DEFAULT_RETENTION = Duration.ofDays(7);
	private static final String ROOT_PROPERTY = "microproject.temporary-root";
	private static final String FILE_PREFIX = "microProject-temp-";
	private static final String MANIFEST_SUFFIX = ".manifest";
	private static final String RETRY_SUFFIX = ".delete";
	private static final String MPOF_DIRECTORY_PREFIX = FILE_PREFIX + "mpof-";
	private static final String MPOF_PURPOSE = "mpof-extraction";
	private static final long MPOF_MANIFEST_MAX_BYTES = 16L * 1024L;
	private static final Logger LOGGER = Logger.getLogger(TemporaryWorkspace.class.getName());

	private final Path root;
	private final Duration retention;
	private final Map<Path, TempArtifact> owned = new ConcurrentHashMap<>();
	private volatile boolean closed;

	private TemporaryWorkspace(Path root, Duration retention) throws IOException {
		this.root = normalizeRoot(root);
		this.retention = validateRetention(retention);
		Files.createDirectories(this.root);
		TemporaryCleanupQueue.retry(this.root);
		cleanupStale(this.root, Instant.now(), this.retention);
	}

	/** Opens the per-user temporary workspace and runs startup cleanup. */
	public static TemporaryWorkspace openDefault() throws IOException {
		return open(defaultRoot(), DEFAULT_RETENTION);
	}

	/** Opens a workspace rooted at {@code root}; primarily useful for a session or test. */
	public static TemporaryWorkspace open(Path root, Duration retention) throws IOException {
		return new TemporaryWorkspace(root, retention);
	}

	/** The normalized, application-owned root. */
	public Path root() {
		return root;
	}

	/** Creates an empty regular file and its manifest in this workspace. */
	public synchronized TempArtifact createArtifact(String hint, String suffix, Map<String, String> metadata) throws IOException {
		ensureOpen();
		String safeHint = safeComponent(hint, "artifact");
		String safeSuffix = safeSuffix(suffix);
		Path artifact = Files.createTempFile(root, FILE_PREFIX + safeHint + "-", safeSuffix);
		return register(artifact, metadata);
	}

	/** Creates an empty directory and its manifest in this workspace. */
	public synchronized TempArtifact createDirectory(String hint, Map<String, String> metadata) throws IOException {
		ensureOpen();
		Path directory = Files.createTempDirectory(root, FILE_PREFIX + safeComponent(hint, "directory") + "-");
		return register(directory, metadata);
	}

	/**
	 * Deletes an artifact and its manifest. A failed deletion is recorded for
	 * retry by the next startup cleanup.
	 */
	public synchronized void delete(TempArtifact artifact) {
		if (artifact == null || artifact.workspace() != this) return;
		owned.remove(artifact.path());
		deleteArtifact(artifact.path(), artifact.manifest());
		artifact.markClosed();
	}

	/** Retries cleanup markers and removes expired artifacts under this root. */
	public void cleanup() {
		TemporaryCleanupQueue.retry(root);
		cleanupStale(root, Instant.now(), retention);
	}

	@Override
	public synchronized void close() {
		if (closed) return;
		closed = true;
		for (TempArtifact artifact : owned.values().toArray(TempArtifact[]::new)) delete(artifact);
		owned.clear();
	}

	/** Startup cleanup entry point for application bootstrap and tests. */
	public static void cleanupStale(Path root, Instant now, Duration retention) {
		Path normalized;
		try {
			normalized = normalizeRoot(root);
		} catch (RuntimeException ex) {
			LOGGER.log(Level.FINE, "Invalid temporary workspace root", ex);
			return;
		}
		if (now == null || retention == null || retention.isNegative() || retention.isZero()
				|| !Files.isDirectory(normalized)) return;
		Instant cutoff = now.minus(retention);
		try (Stream<Path> files = Files.list(normalized)) {
			files.filter(TemporaryWorkspace::isManagedName)
					.filter(path -> !path.equals(normalized))
					.forEach(path -> cleanupOne(path, cutoff));
		} catch (IOException ex) {
			LOGGER.log(Level.FINE, "Unable to inspect temporary workspace " + normalized, ex);
		}
	}

	private TempArtifact register(Path artifact, Map<String, String> metadata) throws IOException {
		Path safeArtifact = requireInsideRoot(artifact);
		Path manifest = manifestPath(safeArtifact);
		Instant createdAt = Instant.now();
		try {
			writeManifest(manifest, createdAt, metadata);
			TempArtifact result = new TempArtifact(this, safeArtifact, manifest, createdAt);
			owned.put(safeArtifact, result);
			return result;
		} catch (IOException | RuntimeException ex) {
			deleteQuietly(manifest, "failed artifact manifest");
			deleteQuietly(safeArtifact, "failed artifact");
			throw ex;
		}
	}

	private void ensureOpen() {
		if (closed) throw new IllegalStateException("Temporary workspace is closed");
	}

	private Path requireInsideRoot(Path candidate) {
		Path normalized = Objects.requireNonNull(candidate, "candidate").toAbsolutePath().normalize();
		if (!normalized.startsWith(root) || normalized.equals(root)) {
			throw new IllegalArgumentException("Temporary artifact escapes workspace root: " + candidate);
		}
		return normalized;
	}

	private static Path normalizeRoot(Path root) {
		return Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
	}

	private static Duration validateRetention(Duration retention) {
		Objects.requireNonNull(retention, "retention");
		if (retention.isNegative() || retention.isZero()) throw new IllegalArgumentException("retention must be positive");
		return retention;
	}

	private static Path defaultRoot() {
		String override = System.getProperty(ROOT_PROPERTY);
		if (override != null && !override.isBlank()) return Path.of(override);
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "microProject", "temp");
		return Path.of(System.getProperty("user.home", "."), "AppData", "Local", "microProject", "temp");
	}

	private static String safeComponent(String value, String fallback) {
		String candidate = value == null || value.isBlank() ? fallback : value.trim();
		if (candidate.indexOf('/') >= 0 || candidate.indexOf('\\') >= 0 || candidate.indexOf(':') >= 0
				|| candidate.equals(".") || candidate.equals("..")) {
			throw new IllegalArgumentException("Invalid temporary artifact name");
		}
		return candidate.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	private static String safeSuffix(String suffix) {
		if (suffix == null || suffix.isBlank()) return ".tmp";
		if (!suffix.startsWith(".") || suffix.indexOf('/') >= 0 || suffix.indexOf('\\') >= 0 || suffix.indexOf(':') >= 0) {
			throw new IllegalArgumentException("Invalid temporary artifact suffix");
		}
		return suffix;
	}

	private static void writeManifest(Path manifest, Instant createdAt, Map<String, String> metadata) throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("createdAt", createdAt.toString());
		if (metadata != null) {
			metadata.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
				String key = Objects.requireNonNull(entry.getKey(), "metadata key");
				String value = Objects.requireNonNull(entry.getValue(), "metadata value");
				if (key.isBlank() || key.equals("createdAt") || key.indexOf('=') >= 0 || key.indexOf('\n') >= 0 || key.indexOf('\r') >= 0
						|| value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) throw new IllegalArgumentException("Invalid manifest metadata");
				values.put(key, value);
			});
		}
		Path temporary = manifest.resolveSibling(manifest.getFileName() + ".part");
		try {
			StringBuilder content = new StringBuilder();
			values.forEach((key, value) -> content.append(key).append('=').append(value).append('\n'));
			Files.writeString(temporary, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			moveAtomically(temporary, manifest);
		} finally {
			deleteQuietly(temporary, "partial temporary manifest");
		}
	}

	private static void cleanupOne(Path path, Instant cutoff) {
		try {
			if (path.getFileName().toString().endsWith(RETRY_SUFFIX)) {
				Path target = path.resolveSibling(path.getFileName().toString().substring(0,
						path.getFileName().toString().length() - RETRY_SUFFIX.length()));
				if (isMpoFManifest(target)) {
					cleanupMpoFManifestRetry(path, target, cutoff);
					return;
				}
				if (isMpofPath(target)) {
					if (Files.isSymbolicLink(target) || !Files.isDirectory(target)) return;
					Optional<MpofManifest> manifest = readMpofManifest(target);
					if (manifest.isEmpty() || isCurrentProcessOpen(manifest.get())
							|| !manifest.get().createdAt().isBefore(cutoff)) return;
				}
				deleteManagedPath(target, "retry temporary artifact");
				if (!Files.exists(target)) Files.deleteIfExists(path);
			} else if (isMpoFManifest(path)) {
				// MPOF manifests are validated and removed together with their directory.
				// Never delete an orphan, malformed, or unrelated manifest by name alone.
				return;
			} else if (isMpofPath(path)) {
				if (Files.isSymbolicLink(path) || !Files.isDirectory(path)) return;
				Optional<MpofManifest> manifest = readMpofManifest(path);
				if (manifest.isEmpty() || isCurrentProcessOpen(manifest.get())
						|| !manifest.get().createdAt().isBefore(cutoff)) return;
				deleteManagedPath(path, "stale MPOF extraction");
				if (!Files.exists(path)) deleteQuietly(manifestPath(path), "stale MPOF manifest");
			} else if (Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) {
				deleteManagedPath(path, "stale temporary artifact");
				deleteQuietly(manifestPath(path), "stale temporary manifest");
			}
		} catch (IOException ex) {
			LOGGER.log(Level.FINE, "Unable to cleanup temporary artifact " + path, ex);
		}
	}

	private static void cleanupMpoFManifestRetry(Path marker, Path manifest, Instant cutoff) throws IOException {
		Path fileName = manifest.getFileName();
		String name = fileName.toString();
		String directoryName = name.substring(0, name.length() - MANIFEST_SUFFIX.length());
		Path directory = manifest.resolveSibling(directoryName);
		Optional<MpofManifest> metadata = readMpofManifest(directory);
		if (metadata.isEmpty() || isCurrentProcessOpen(metadata.get())
				|| !metadata.get().createdAt().isBefore(cutoff)) return;
		if (Files.isSymbolicLink(directory)) return;
		if (Files.exists(directory)) {
			if (!Files.isDirectory(directory)) return;
			deleteManagedPath(directory, "retry stale MPOF extraction");
		}
		if (!Files.exists(directory)) deleteQuietly(manifest, "retry stale MPOF manifest");
		if (!Files.exists(manifest)) Files.deleteIfExists(marker);
	}

	/** Deletes stale managed directories from the leaves upward; open Windows
	 * handles leave a retry marker for the next startup rather than leaking the
	 * rest of the tree. */
	private static void deleteManagedPath(Path path, String description) {
		if (path == null) return;
		// Existing generic artifacts retain their original one-shot deletion
		// semantics.  MPO extraction sessions are tree-shaped and need leaf-first
		// cleanup so a stale Windows handle does not strand the whole directory.
		if (!Files.isDirectory(path) || !path.getFileName().toString().contains("-mpof-")) {
			deleteQuietly(path, description);
			return;
		}
		try (Stream<Path> children = Files.walk(path)) {
			children.sorted((left, right) -> right.compareTo(left))
					.forEach(child -> deleteQuietly(child, description));
		} catch (IOException ex) {
			LOGGER.log(Level.FINE, "Unable to enumerate " + description + " " + path, ex);
		}
	}

	private static boolean isManagedName(Path path) {
		String name = path.getFileName().toString();
		String base = name.endsWith(RETRY_SUFFIX) ? name.substring(0, name.length() - RETRY_SUFFIX.length()) : name;
		return base.startsWith(FILE_PREFIX) && (base.endsWith(MANIFEST_SUFFIX) || base.contains(FILE_PREFIX));
	}

	private static boolean isMpofPath(Path path) {
		if (path == null) return false;
		String name = path.getFileName().toString();
		if (name.endsWith(RETRY_SUFFIX)) name = name.substring(0, name.length() - RETRY_SUFFIX.length());
		return name.startsWith(MPOF_DIRECTORY_PREFIX) && !name.endsWith(MANIFEST_SUFFIX);
	}

	private static boolean isMpoFManifest(Path path) {
		if (path == null) return false;
		String name = path.getFileName().toString();
		if (name.endsWith(RETRY_SUFFIX)) name = name.substring(0, name.length() - RETRY_SUFFIX.length());
		return name.startsWith(MPOF_DIRECTORY_PREFIX) && name.endsWith(MANIFEST_SUFFIX);
	}

	private static Optional<MpofManifest> readMpofManifest(Path directory) {
		Path manifest = manifestPath(directory);
		if (Files.isSymbolicLink(manifest) || !Files.isRegularFile(manifest)) return Optional.empty();
		try {
			if (Files.size(manifest) > MPOF_MANIFEST_MAX_BYTES) return Optional.empty();
			Map<String, String> values = new LinkedHashMap<>();
			for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
				int separator = line.indexOf('=');
				if (separator <= 0 || values.put(line.substring(0, separator), line.substring(separator + 1)) != null)
					return Optional.empty();
			}
			if (!MPOF_PURPOSE.equals(values.get("purpose"))) return Optional.empty();
			Instant createdAt = Instant.parse(values.get("createdAt"));
			String instanceId = values.get("instanceId");
			if (instanceId == null || instanceId.isBlank()) return Optional.empty();
			UUID.fromString(instanceId);
			long processId = Long.parseLong(values.get("processId"));
			if (processId <= 0L) return Optional.empty();
			Instant processStart = Instant.parse(values.get("processStart"));
			String state = values.get("state");
			if (!"open".equals(state) && !"closed".equals(state)) return Optional.empty();
			return Optional.of(new MpofManifest(createdAt, instanceId, processId, processStart, state));
		} catch (IOException | RuntimeException ex) {
			LOGGER.log(Level.FINE, "Ignoring invalid MPOF extraction manifest " + manifest, ex);
			return Optional.empty();
		}
	}

	private static boolean isCurrentProcessOpen(MpofManifest manifest) {
		if (!"open".equals(manifest.state())) return false;
		ProcessHandle process = ProcessHandle.current();
		Optional<Instant> processStart = process.info().startInstant();
		return manifest.processId() == process.pid()
				&& processStart.isPresent() && processStart.get().equals(manifest.processStart());
	}

	private record MpofManifest(Instant createdAt, String instanceId, long processId,
			Instant processStart, String state) { }

	private void deleteArtifact(Path artifact, Path manifest) {
		Path safeArtifact;
		Path safeManifest;
		try {
			safeArtifact = requireInsideRoot(artifact);
			safeManifest = requireInsideRoot(manifest);
		} catch (RuntimeException ex) {
			LOGGER.log(Level.WARNING, "Refusing to delete artifact outside temporary workspace", ex);
			return;
		}
		if (Files.isDirectory(safeArtifact)) {
			try (Stream<Path> paths = Files.walk(safeArtifact)) {
				paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> deleteQuietly(path, "temporary directory entry"));
			} catch (IOException ex) {
				LOGGER.log(Level.FINE, "Unable to enumerate temporary directory " + safeArtifact, ex);
			}
		} else deleteQuietly(safeArtifact, "temporary artifact");
		deleteQuietly(safeManifest, "temporary manifest");
	}

	private static Path manifestPath(Path artifact) {
		return artifact.resolveSibling(artifact.getFileName() + MANIFEST_SUFFIX);
	}

	private static void moveAtomically(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void deleteQuietly(Path path, String description) {
		if (path == null) return;
		Path marker = path.resolveSibling(path.getFileName() + RETRY_SUFFIX);
		try {
			if (!Files.deleteIfExists(path)) {
				Files.deleteIfExists(marker);
				return;
			}
			Files.deleteIfExists(marker);
		} catch (IOException ex) {
			LOGGER.log(Level.FINE, "Unable to delete " + description + " " + path, ex);
			if (path.getFileName().toString().endsWith(RETRY_SUFFIX)) return;
			try {
				Files.writeString(marker, "retryAt=" + Instant.now() + '\n', StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			} catch (IOException markerFailure) {
				LOGGER.log(Level.FINE, "Unable to persist cleanup retry marker " + marker, markerFailure);
			}
		}
	}

	/** A workspace-owned artifact; closing it is idempotent and deletes its files. */
	public static final class TempArtifact implements AutoCloseable {
		private final TemporaryWorkspace workspace;
		private final Path path;
		private final Path manifest;
		private final Instant createdAt;
		private volatile boolean closed;

		private TempArtifact(TemporaryWorkspace workspace, Path path, Path manifest, Instant createdAt) {
			this.workspace = workspace;
			this.path = path;
			this.manifest = manifest;
			this.createdAt = createdAt;
		}

		public Path path() { return path; }
		public Path manifest() { return manifest; }
		public Instant createdAt() { return createdAt; }

		@Override
		public void close() {
			if (!closed) workspace.delete(this);
		}

		private TemporaryWorkspace workspace() { return workspace; }
		private void markClosed() { closed = true; }
	}
}
