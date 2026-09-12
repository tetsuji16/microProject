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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
			if (offered || !Files.isRegularFile(snapshot)) {
				return false;
			}
			if (originalFileName == null || originalFileName.isBlank()) {
				return true;
			}
			try {
				Path original = Path.of(originalFileName);
				return !Files.exists(original) || Files.getLastModifiedTime(snapshot).toInstant()
					.isAfter(Files.getLastModifiedTime(original).toInstant());
			} catch (IOException | RuntimeException ex) {
				return true;
			}
		}
	}

	private final Path directory;

	public AutoRecoveryStore(Path directory) {
		this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
	}

	public static AutoRecoveryStore forCurrentUser() {
		String localAppData = System.getenv("LOCALAPPDATA");
		Path base = localAppData == null || localAppData.isBlank()
			? Path.of(System.getProperty("user.home"), ".projectlibre")
			: Path.of(localAppData, "ProjectLibre");
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

	/** Selects MPO for an MPO source file while retaining POD compatibility for legacy files. */
	public Path snapshotPath(long projectId, String originalFileName) throws IOException {
		return snapshotPath(projectId, isMpoFileName(originalFileName));
	}

	public void recordCompletedSnapshot(long projectId, String displayName,
		String originalFileName, Instant savedAt) throws IOException {
		Path snapshot = snapshotPath(projectId, originalFileName);
		// A caller may have selected MPO because the project contains CCPM data
		// even when its original path used the legacy POD extension.  Prefer the
		// actually written sibling before reporting a missing snapshot.
		if (!Files.isRegularFile(snapshot)) {
			Path alternate = snapshotPath(projectId, !isMpoFileName(originalFileName));
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
		properties.setProperty(SNAPSHOT_FORMAT_PROPERTY, isMpoSnapshot(snapshot) ? "mpo" : "pod");
		Path metadata = metadataPath(projectId);
		writeMetadataAtomically(metadata, projectId, properties);
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

	/** Marks a recovery round as presented without modifying the snapshot file. */
	public void markOffered(long projectId) throws IOException {
		Path metadata = metadataPath(projectId);
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(metadata)) {
			properties.load(input);
		}
		properties.setProperty("offered", Boolean.TRUE.toString());
		writeMetadataAtomically(metadata, projectId, properties);
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
	}

	public void cleanup(Instant now, Duration retention) throws IOException {
		if (!Files.isDirectory(directory)) {
			return;
		}
		Instant cutoff = Objects.requireNonNull(now, "now").minus(Objects.requireNonNull(retention, "retention"));
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (Path file : files) {
				if (Files.isRegularFile(file) && Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
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
			try {
				Files.move(temporary.path(), metadata, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
				Files.move(temporary.path(), metadata, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private ReadResult read(Path metadata) {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(metadata)) {
			properties.load(input);
			long id = parseProjectId(properties.getProperty("projectId"));
			Instant savedAt = parseSavedAt(properties.getProperty("savedAt"));
			String originalFileName = emptyToNull(properties.getProperty("originalFileName"));
			boolean mpo = parseMpoFormat(properties.getProperty(SNAPSHOT_FORMAT_PROPERTY), originalFileName);
			return ReadResult.success(new Entry(id, emptyToNull(properties.getProperty("displayName")),
				originalFileName, savedAt,
				snapshotFile(id, mpo), metadata,
				Boolean.parseBoolean(properties.getProperty("offered", "false"))));
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

	private Path metadataPath(long projectId) throws IOException {
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + METADATA_SUFFIX);
	}

	private Path snapshotFile(long projectId, boolean mpo) {
		return directory.resolve(safeId(projectId) + (mpo ? MPO_SNAPSHOT_SUFFIX : SNAPSHOT_SUFFIX));
	}

	private static boolean isMpoFileName(String fileName) {
		return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".mpo");
	}

	private static boolean isMpoSnapshot(Path snapshot) {
		Path fileName = snapshot.getFileName();
		return fileName != null && fileName.toString().endsWith(MPO_SNAPSHOT_SUFFIX);
	}

	private static boolean parseMpoFormat(String value, String originalFileName) {
		if (value == null || value.isBlank())
			return isMpoFileName(originalFileName);
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
