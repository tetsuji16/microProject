/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application;

import com.microproject.session.FileHelper;
import com.microproject.util.SafeFileReplace;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns crash-recovery files independently of the currently open project file.
 * Metadata is replaced atomically so an interrupted write never advertises an
 * incomplete project snapshot.
 */
public final class AutoRecoveryStore {
	public static final Duration DEFAULT_RETENTION = Duration.ofDays(14);
	private static final String SNAPSHOT_SUFFIX = ".recovery.pod";
	private static final String MPO_SNAPSHOT_SUFFIX = ".recovery.mpo";
	private static final String METADATA_SUFFIX = ".recovery.properties";
	private static final String SNAPSHOT_FORMAT_PROPERTY = "snapshotFormat";
	private static final String SNAPSHOT_FILE_PROPERTY = "snapshotFile";
	private static final Logger LOGGER = Logger.getLogger(AutoRecoveryStore.class.getName());

	/** The kind of failure found while inspecting one recovery metadata file. */
	public enum MetadataIssueKind {
		UNREADABLE,
		MALFORMED
	}

	/** A diagnostic that explains why one metadata file was not offered. */
	public record MetadataIssue(Path metadata, MetadataIssueKind kind, String detail) {
		public MetadataIssue {
			Objects.requireNonNull(metadata, "metadata");
			Objects.requireNonNull(kind, "kind");
			if (detail == null || detail.isBlank()) {
				detail = "No further details available";
			}
		}
	}

	/**
	 * Result of one recovery directory scan.  Invalid files are isolated from
	 * valid candidates so one damaged metadata file cannot hide other recovery
	 * candidates.  The immutable issue list is available to callers that need to
	 * surface diagnostics or report them to telemetry.
	 */
	public record RecoveryScan(List<Entry> entries, List<MetadataIssue> issues) {
		public RecoveryScan {
			entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
			issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
		}

		public boolean hasIssues() {
			return !issues.isEmpty();
		}
	}

	public record Entry(long projectId, String displayName, String originalFileName,
		Instant savedAt, Path snapshot, Path metadata, boolean offered) {
		public boolean shouldOfferRecovery() {
			// A newer original may have been saved by another process with different edits.
			return !offered && Files.isRegularFile(snapshot);
		}
	}

	private final Path directory;

	public AutoRecoveryStore(Path directory) {
		this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
	}

	public static AutoRecoveryStore forCurrentUser() {
		String localAppData = System.getenv("LOCALAPPDATA");
		return forUserDirectories(Path.of(System.getProperty("user.home")),
			localAppData == null || localAppData.isBlank() ? null : Path.of(localAppData));
	}

	/** New installations own their recovery data; existing snapshots retain their location. */
	static AutoRecoveryStore forUserDirectories(Path home, Path localAppData) {
		Path base = localAppData == null ? home.resolve(".microproject") : localAppData.resolve("microProject");
		Path legacy = (localAppData == null ? home.resolve(".projectlibre")
			: localAppData.resolve("ProjectLibre")).resolve("recovery");
		// Never abandon an interrupted beta.1 save. Keep its complete directory
		// until recovery/normal shutdown has removed the metadata. No files are moved
		// or overwritten, and concurrent old processes still share the same path.
		if (Files.isDirectory(legacy)) {
			try (DirectoryStream<Path> metadata = Files.newDirectoryStream(legacy, "*" + METADATA_SUFFIX)) {
				if (metadata.iterator().hasNext()) return new AutoRecoveryStore(legacy);
			} catch (IOException | DirectoryIteratorException error) {
				LOGGER.log(Level.WARNING, "Unable to inspect legacy recovery; retaining its location", error);
				return new AutoRecoveryStore(legacy);
			}
		}
		return new AutoRecoveryStore(base.resolve("recovery"));
	}

	public Path snapshotPath(long projectId) throws IOException {
		return snapshotPath(projectId, false);
	}

	/** Returns the recovery path for the requested native container format. */
	public Path snapshotPath(long projectId, boolean mpo) throws IOException {
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + (mpo ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX));
	}

	/** A fresh filename ensures a failed save cannot overwrite the published generation. */
	public Path newSnapshotPath(long projectId, boolean mpo) throws IOException {
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + "." + java.util.UUID.randomUUID()
			+ (mpo ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX));
	}

	/** Selects MPO for an MPO source file while retaining POD compatibility for legacy files. */
	public Path snapshotPath(long projectId, String originalFileName) throws IOException {
		return snapshotPath(projectId, FileHelper.isMpoFile(originalFileName));
	}

	public void recordCompletedSnapshot(long projectId, String displayName,
		String originalFileName, Instant savedAt) throws IOException {
		Path snapshot = snapshotPath(projectId, originalFileName);
		// A caller may have selected MPO because the project contains CCPM data
		// even when its original path used the legacy POD extension.  Prefer the
		// actually written sibling before reporting a missing snapshot.
		if (!Files.isRegularFile(snapshot)) {
			Path alternate = snapshotPath(projectId, !FileHelper.isMpoFile(originalFileName));
			if (Files.isRegularFile(alternate))
				snapshot = alternate;
		}
		if (!Files.isRegularFile(snapshot)) {
			throw new IOException("Recovery snapshot was not created: " + snapshot);
		}
		recordCompletedSnapshot(projectId, displayName, originalFileName, savedAt, snapshot);
	}

	/** Records metadata for the exact snapshot path produced by the save job. */
	public void recordCompletedSnapshot(long projectId, String displayName,
		String originalFileName, Instant savedAt, Path snapshot) throws IOException {
		recordCompletedSnapshot(projectId, displayName, originalFileName, savedAt, snapshot, null);
	}

	/** A series identifies generations produced by one running application instance. */
	public void recordCompletedSnapshot(long projectId, String displayName,
			String originalFileName, Instant savedAt, Path snapshot, String seriesId) throws IOException {
		if (seriesId != null) {
			try { if (!java.util.UUID.fromString(seriesId).toString().equals(seriesId)) throw new IllegalArgumentException(); }
			catch (IllegalArgumentException ex) { throw new IOException("Invalid recovery series", ex); }
		}
		Objects.requireNonNull(snapshot, "snapshot");
		if (!Files.isRegularFile(snapshot)) {
			throw new IOException("Recovery snapshot was not created: " + snapshot);
		}
		Properties properties = new Properties();
		properties.setProperty("projectId", Long.toString(projectId));
		properties.setProperty("displayName", nullToEmpty(displayName));
		properties.setProperty("originalFileName", nullToEmpty(originalFileName));
		properties.setProperty("savedAt", Objects.requireNonNull(savedAt, "savedAt").toString());
		properties.setProperty("offered", Boolean.FALSE.toString());
		if (seriesId != null) properties.setProperty("seriesId", seriesId);
		Path normalized = snapshot.toAbsolutePath().normalize();
		if (!directory.equals(normalized.getParent()) || !validSnapshotName(projectId, normalized.getFileName().toString()))
			throw new IOException("Invalid recovery snapshot path: " + snapshot);
		properties.setProperty(SNAPSHOT_FORMAT_PROPERTY, isMpoSnapshot(snapshot) ? "mpo" : "pod");
		properties.setProperty(SNAPSHOT_FILE_PROPERTY, normalized.getFileName().toString());
		Path metadata = metadataPathForSnapshot(projectId, normalized);
		withLock(() -> {
			writeMetadataAtomically(metadata, projectId, properties);
			if (seriesId != null) {
				try { cleanupSeries(projectId, seriesId, metadata); }
				catch (IOException error) { LOGGER.log(Level.WARNING, "Could not clean prior recovery generations", error); }
			}
			return null;
		});
	}

	/** Called under the directory lock after a new generation is published. */
	private void cleanupSeries(long projectId, String seriesId, Path retained) throws IOException {
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
			for (Path file : files) {
				if (file.equals(retained)) continue;
				Properties properties = new Properties();
				try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
				catch (IOException | IllegalArgumentException unreadable) { continue; }
				if (!seriesId.equals(properties.getProperty("seriesId"))) continue;
				ReadResult result = read(file);
				if (result.entry() == null || result.entry().projectId() != projectId) continue;
				Path snapshot = result.entry().snapshot();
				// Old single-manifest files can refer to a different generation; they
				// are readable for compatibility but cannot authorize reclamation.
				if (!file.equals(metadataPathForSnapshot(projectId, snapshot)) || hasOtherReference(file, snapshot)) continue;
				try (java.nio.channels.FileChannel channel = openLeaseChannel(snapshot)) {
					java.nio.channels.FileLock lease;
					try { lease = channel.tryLock(); }
					catch (java.nio.channels.OverlappingFileLockException busy) { lease = null; }
					if (lease == null) continue;
					try (java.nio.channels.FileLock held = lease) {
						Files.deleteIfExists(file);
						Files.deleteIfExists(snapshot);
					}
				}
				Files.deleteIfExists(leasePath(snapshot));
			}
		}
	}

	/** An unreadable manifest may contain a reference, so reclamation must fail closed. */
	private boolean hasOtherReference(Path owner, Path snapshot) throws IOException {
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
			for (Path file : files) {
				if (file.equals(owner)) continue;
				ReadResult result = read(file);
				if (result.entry() == null || result.entry().snapshot().equals(snapshot)) return true;
			}
		}
		return false;
	}

	/** Extracts an additional possible owner from an invalid manifest without trusting it. */
	private Long declaredProjectId(Path metadata) {
		try (InputStream input = Files.newInputStream(metadata)) {
			Properties properties = new Properties(); properties.load(input);
			return parseProjectId(properties.getProperty("projectId"));
		} catch (IOException | RuntimeException ex) { return null; }
	}

	public List<Entry> listRecoverable() throws IOException {
		RecoveryScan scan = scanRecoverable();
		for (MetadataIssue issue : scan.issues()) {
			LOGGER.log(Level.WARNING, "Ignoring auto-recovery metadata {0} ({1}): {2}",
				new Object[] { issue.metadata(), issue.kind(), issue.detail() });
		}
		return scan.entries();
	}

	/**
	 * Lists recovery candidates and returns parse/I-O failures as structured
	 * diagnostics.  This method does not log, allowing callers to choose their
	 * own reporting policy; {@link #listRecoverable()} logs the same diagnostics
	 * for existing callers.
	 */
	public RecoveryScan scanRecoverable() throws IOException {
		if (!Files.isDirectory(directory)) {
			return new RecoveryScan(List.of(), List.of());
		}
		List<Entry> result = new ArrayList<>();
		List<MetadataIssue> issues = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
			for (Path metadata : files) {
				ReadResult readResult = read(metadata);
				if (readResult.entry() != null && readResult.entry().shouldOfferRecovery()) {
					result.add(readResult.entry());
				}
				if (readResult.issue() != null) {
					issues.add(readResult.issue());
				}
			}
		}
		result.sort(Comparator.comparing(Entry::savedAt).reversed());
		return new RecoveryScan(result, issues);
	}

	/** Holds an operating-system lease for a single snapshot while it is presented or loaded. */
	public final class OfferLease implements AutoCloseable {
		private final Entry candidate;
		private final java.nio.channels.FileChannel channel;
		private final java.nio.channels.FileLock lock;
		private OfferLease(Entry candidate, java.nio.channels.FileChannel channel, java.nio.channels.FileLock lock) {
			this.candidate = candidate; this.channel = channel; this.lock = lock;
		}
		/** Consume only the generation actually presented; a replacement stays eligible. */
		public void complete() throws IOException {
			withLock(() -> {
				ReadResult current = read(candidate.metadata());
				if (candidate.equals(current.entry())) {
					Properties properties = new Properties();
					try (InputStream input = Files.newInputStream(candidate.metadata())) { properties.load(input); }
					properties.setProperty("discarded", Boolean.TRUE.toString());
					writeMetadataAtomically(candidate.metadata(), candidate.projectId(), properties);
				}
				return null;
			});
		}
		@Override public void close() throws IOException { try { lock.close(); } finally { channel.close(); } }
	}

	/** Returns null when the generation has changed or another process is presenting it. */
	public OfferLease tryOffer(Entry candidate) throws IOException {
		return withLock(() -> {
			ReadResult current = read(candidate.metadata());
			if (!candidate.equals(current.entry()) || !candidate.shouldOfferRecovery()) return null;
			java.nio.channels.FileChannel channel = openLeaseChannel(candidate.snapshot());
			try {
				java.nio.channels.FileLock lock;
				try { lock = channel.tryLock(); }
				catch (java.nio.channels.OverlappingFileLockException busy) { lock = null; }
				if (lock == null) { channel.close(); return null; }
				return new OfferLease(candidate, channel, lock);
			} catch (IOException | RuntimeException ex) { channel.close(); throw ex; }
		});
	}

	/** Distinguishes an unchanged, leased candidate from one removed after scanning. */
	public boolean isCurrent(Entry candidate) throws IOException {
		return withLock(() -> candidate.equals(read(candidate.metadata()).entry()));
	}

	private java.nio.channels.FileChannel openLeaseChannel(Path snapshot) throws IOException {
		return java.nio.channels.FileChannel.open(leasePath(snapshot),
			java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
	}

	private static Path leasePath(Path snapshot) {
		return snapshot.resolveSibling(snapshot.getFileName() + ".lease");
	}

	public boolean claimOffered(Entry candidate) throws IOException {
		try (OfferLease lease = tryOffer(candidate)) {
			if (lease == null) return false;
			lease.complete(); return true;
		}
	}
	public void markOffered(long projectId) throws IOException {
		ReadResult result = read(metadataPath(projectId));
		if (result.entry() != null) claimOffered(result.entry());
	}

	/** Removes only an unleased generation owned by this instance. */
	public void discardGeneration(long projectId, Path snapshot) throws IOException {
		withLock(() -> {
			if (!Files.exists(snapshot) && !Files.exists(metadataPathForSnapshot(projectId, snapshot))) return null;
			try (java.nio.channels.FileChannel channel = openLeaseChannel(snapshot)) {
				java.nio.channels.FileLock lease;
				try { lease = channel.tryLock(); }
				catch (java.nio.channels.OverlappingFileLockException busy) { lease = null; }
				if (lease == null) return null;
				try (java.nio.channels.FileLock heldLease = lease) {
					Path metadata = metadataPathForSnapshot(projectId, snapshot);
					ReadResult current = read(metadata);
					if (Files.exists(metadata) && (current.entry() == null || !current.entry().snapshot().equals(snapshot))) return null;
					if (hasOtherReference(metadata, snapshot)) return null;
					if (current.entry() != null && current.entry().snapshot().equals(snapshot))
						Files.deleteIfExists(current.entry().metadata());
					Files.deleteIfExists(snapshot);
				}
			}
			return null;
		});
	}

	@FunctionalInterface private interface LockedOperation<T> { T run() throws IOException; }
	private <T> T withLock(LockedOperation<T> operation) throws IOException {
		Files.createDirectories(directory);
		try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(directory.resolve(".recovery.lock"),
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
			while (true) {
				try (java.nio.channels.FileLock lock = channel.tryLock()) {
					if (lock != null) return operation.run();
				} catch (java.nio.channels.OverlappingFileLockException busy) { /* another local instance */ }
				try { Thread.sleep(25); }
				catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Interrupted waiting for recovery lock", interrupted); }
			}
		}
	}

	/** Reclaims old, unreferenced generations after a crash, without touching leased snapshots. */
	public void cleanupOrphanGenerations(Instant now, Duration retention) throws IOException {
		if (!Files.isDirectory(directory)) return;
		withLock(() -> {
			Instant cutoff = now.minus(retention);
			java.util.Set<Long> protectedIds = new java.util.HashSet<>();
			java.util.Set<Path> current = new java.util.HashSet<>();
			try (DirectoryStream<Path> metadata = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
				for (Path file : metadata) {
					Long id = recoveryFileId(file);
					ReadResult result = read(file);
					// A malformed manifest may name a snapshot belonging to any ID.
					// Do not reclaim any orphan while that reference is unknown.
					if (id == null || result.entry() == null) return null;
					if (result.entry().projectId() != id.longValue()) {
						protectedIds.add(id);
						Long declared = declaredProjectId(file);
						if (declared != null) protectedIds.add(declared);
					} else current.add(result.entry().snapshot());
				}
			}
			try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
				for (Path file : files) {
					if (!Files.exists(file)) continue;
					String name = file.getFileName().toString();
					boolean leaseFile = name.endsWith(".lease");
					Path snapshot = leaseFile ? file.resolveSibling(name.substring(0, name.length() - 6)) : file;
					Long id = recoveryFileId(snapshot);
					if (id == null || protectedIds.contains(id) || current.contains(snapshot)
							|| !validSnapshotName(id, snapshot.getFileName().toString())
							|| !Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) continue;
					try (java.nio.channels.FileChannel channel = openLeaseChannel(snapshot)) {
						java.nio.channels.FileLock acquired;
						try { acquired = channel.tryLock(); }
						catch (java.nio.channels.OverlappingFileLockException busy) { acquired = null; }
						if (acquired == null) continue;
						try (java.nio.channels.FileLock held = acquired) { Files.deleteIfExists(snapshot); }
					}
					Files.deleteIfExists(leasePath(snapshot));
				}
			}
			return null;
		});
	}

	/** Clears recovery state only after the normal shutdown sequence completed. */
	public void discardAll() throws IOException {
		if (!Files.isDirectory(directory)) {
			return;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (Path file : files) {
				String name = file.getFileName().toString();
				if (name.endsWith(SNAPSHOT_SUFFIX) || name.endsWith(MPO_SNAPSHOT_SUFFIX)
						|| name.endsWith(METADATA_SUFFIX)) {
					Files.deleteIfExists(file);
				}
			}
		}
	}

	public void discard(long projectId) throws IOException {
		Files.deleteIfExists(snapshotFile(projectId, false));
		Files.deleteIfExists(snapshotFile(projectId, true));
		Files.deleteIfExists(metadataPath(projectId));
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (Path file : files) {
				if (validSnapshotName(projectId, file.getFileName().toString())) Files.deleteIfExists(file);
			}
		}
	}

	public void cleanup(Instant now, Duration retention) throws IOException {
		if (!Files.isDirectory(directory)) {
			return;
		}
		Instant cutoff = Objects.requireNonNull(now, "now").minus(Objects.requireNonNull(retention, "retention"));
		// Metadata is the publication point. Its timestamp can be newer than the
		// snapshot (for example after marking it offered), so retain both together.
		java.util.Set<Path> retainedSnapshots = new java.util.HashSet<>();
		java.util.Set<Long> protectedIds = new java.util.HashSet<>();
		try (DirectoryStream<Path> metadataFiles = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
			for (Path metadata : metadataFiles) {
				if (!Files.isRegularFile(metadata)) continue;
				Long filenameId = recoveryFileId(metadata);
				ReadResult result = read(metadata);
				if (result.entry() == null || filenameId == null || result.entry().projectId() != filenameId.longValue()) {
					// Unknown ownership cannot be safely isolated. For known ownership,
					// retain every generation because the pointer may be unreadable.
					if (filenameId == null)
						throw new IOException("Cannot identify recovery metadata: " + metadata);
					protectedIds.add(filenameId);
					Long declared = declaredProjectId(metadata);
					if (declared != null) protectedIds.add(declared);
					if (result.entry() != null) {
						protectedIds.add(result.entry().projectId());
						retainedSnapshots.add(result.entry().snapshot());
					}
				} else if (!Files.getLastModifiedTime(metadata).toInstant().isBefore(cutoff)) {
					retainedSnapshots.add(result.entry().snapshot());
				}
			}
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (Path file : files) {
				Long id = recoveryFileId(file);
				if (id != null && !protectedIds.contains(id) && isOwnedRecoveryFile(file)
						&& !retainedSnapshots.contains(file) && Files.isRegularFile(file)
						&& Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
					Files.deleteIfExists(file);
				}
			}
		}
	}

	/** Writes metadata through the common manifest/retry-owned temporary artifact contract. */
	private void writeMetadataAtomically(Path metadata, long projectId, Properties properties) throws IOException {
		try (TemporaryWorkspace workspace = TemporaryWorkspace.open(directory, TemporaryWorkspace.DEFAULT_RETENTION);
			TemporaryWorkspace.TempArtifact temporary = workspace.createArtifact("recovery-" + safeId(projectId), ".metadata.tmp",
				java.util.Map.of("kind", "auto-recovery-metadata", "projectId", Long.toString(projectId)))) {
			try (OutputStream output = Files.newOutputStream(temporary.path())) {
				properties.store(output, "ProjectLibre AutoRecovery");
			}
			SafeFileReplace.moveAtomically(temporary.path(), metadata, true);
		}
	}

	private ReadResult read(Path metadata) {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(metadata)) {
			properties.load(input);
			long id = parseProjectId(properties.getProperty("projectId"));
			Long filenameId = recoveryFileId(metadata);
			if (filenameId == null || filenameId.longValue() != id)
				throw new IllegalArgumentException("Recovery metadata project identity differs from filename");
			Instant savedAt = parseSavedAt(properties.getProperty("savedAt"));
			String originalFileName = emptyToNull(properties.getProperty("originalFileName"));
			boolean mpo = parseMpoFormat(properties.getProperty(SNAPSHOT_FORMAT_PROPERTY), originalFileName);
			String snapshotName = properties.getProperty(SNAPSHOT_FILE_PROPERTY);
			if (snapshotName != null && !validSnapshotName(id, snapshotName))
				throw new IllegalArgumentException("Invalid recovery snapshot filename");
			Path snapshot = snapshotName == null ? snapshotFile(id, mpo) : directory.resolve(snapshotName);
			String metadataName = metadata.getFileName().toString();
			String stem = metadataName.substring(0, metadataName.length() - METADATA_SUFFIX.length());
			if (stem.indexOf('.') >= 0 && !metadata.equals(metadataPathForSnapshot(id, snapshot)))
				throw new IllegalArgumentException("Recovery metadata and snapshot generations differ");
			return ReadResult.success(new Entry(id, emptyToNull(properties.getProperty("displayName")),
				originalFileName, savedAt,
				snapshot, metadata,
				Boolean.parseBoolean(properties.getProperty("discarded", "false"))));
		} catch (IOException | SecurityException ex) {
			return ReadResult.failure(new MetadataIssue(metadata, MetadataIssueKind.UNREADABLE,
				detail(ex)));
		} catch (IllegalArgumentException ex) {
			return ReadResult.failure(new MetadataIssue(metadata, MetadataIssueKind.MALFORMED,
				detail(ex)));
		}
	}

	private static long parseProjectId(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing projectId");
		}
		return Long.parseLong(value);
	}

	private static Instant parseSavedAt(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing savedAt");
		}
		return Instant.parse(value);
	}

	private static String detail(Exception ex) {
		String message = ex.getMessage();
		return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
	}

	private record ReadResult(Entry entry, MetadataIssue issue) {
		static ReadResult success(Entry entry) {
			return new ReadResult(entry, null);
		}

		static ReadResult failure(MetadataIssue issue) {
			return new ReadResult(null, issue);
		}
	}

	private Path metadataPathForSnapshot(long projectId, Path snapshot) throws IOException {
		String name = snapshot.getFileName().toString();
		if (!validSnapshotName(projectId, name) || !directory.equals(snapshot.toAbsolutePath().normalize().getParent()))
			throw new IOException("Invalid recovery snapshot path: " + snapshot);
		String suffix = name.endsWith(MPO_SNAPSHOT_SUFFIX) ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX;
		return directory.resolve(name.substring(0, name.length() - suffix.length()) + METADATA_SUFFIX);
	}

	private Path metadataPath(long projectId) throws IOException {
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + METADATA_SUFFIX);
	}

	private Path snapshotFile(long projectId, boolean mpo) {
		return directory.resolve(safeId(projectId) + (mpo ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX));
	}

	private static boolean validSnapshotName(long projectId, String name) {
		String prefix = safeId(projectId);
		if (name.equals(prefix + SNAPSHOT_SUFFIX) || name.equals(prefix + MPO_SNAPSHOT_SUFFIX)) return true;
		String suffix = name.endsWith(MPO_SNAPSHOT_SUFFIX) ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX;
		if (!name.startsWith(prefix + ".") || !name.endsWith(suffix)) return false;
		String generation = name.substring(prefix.length() + 1, name.length() - suffix.length());
		try { return java.util.UUID.fromString(generation).toString().equals(generation); }
		catch (IllegalArgumentException ex) { return false; }
	}

	private static Long recoveryFileId(Path file) {
		String name = file.getFileName().toString();
		int separator = name.indexOf('.');
		if (separator <= 0) return null;
		try {
			long id = Long.parseUnsignedLong(name.substring(0, separator));
			return name.substring(0, separator).equals(safeId(id)) ? id : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean isOwnedRecoveryFile(Path file) {
		String name = file.getFileName().toString();
		int separator = name.indexOf('.');
		if (separator <= 0) return false;
		try {
			long id = Long.parseUnsignedLong(name.substring(0, separator));
			return name.endsWith(METADATA_SUFFIX) && (name.equals(safeId(id) + METADATA_SUFFIX)
				|| validSnapshotName(id, name.substring(0, name.length() - METADATA_SUFFIX.length()) + SNAPSHOT_SUFFIX))
				|| validSnapshotName(id, name);
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	private static boolean isMpoSnapshot(Path snapshot) {
		Path fileName = snapshot.getFileName();
		return fileName != null && fileName.toString().endsWith(MPO_SNAPSHOT_SUFFIX);
	}

	private static boolean parseMpoFormat(String value, String originalFileName) {
		if (value == null || value.isBlank())
			return FileHelper.isMpoFile(originalFileName);
		if ("mpo".equalsIgnoreCase(value))
			return true;
		if ("pod".equalsIgnoreCase(value))
			return false;
		throw new IllegalArgumentException("Unsupported recovery snapshot format: " + value);
	}

	private static String safeId(long projectId) {
		return Long.toUnsignedString(projectId);
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
