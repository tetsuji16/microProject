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
import java.util.Objects;
import java.util.Properties;

/**
 * Owns crash-recovery files independently of the currently open project file.
 * Metadata is replaced atomically so an interrupted write never advertises an
 * incomplete project snapshot.
 */
public final class AutoRecoveryStore {
	public static final Duration DEFAULT_RETENTION = Duration.ofDays(14);
	private static final String SNAPSHOT_SUFFIX = ".recovery.pod";
	private static final String METADATA_SUFFIX = ".recovery.properties";

	public record Entry(long projectId, String displayName, String originalFileName,
		Instant savedAt, Path snapshot, Path metadata) {
		public boolean shouldOfferRecovery() {
			if (!Files.isRegularFile(snapshot)) {
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
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + SNAPSHOT_SUFFIX);
	}

	public void recordCompletedSnapshot(long projectId, String displayName,
		String originalFileName, Instant savedAt) throws IOException {
		Path snapshot = snapshotPath(projectId);
		if (!Files.isRegularFile(snapshot)) {
			throw new IOException("Recovery snapshot was not created: " + snapshot);
		}
		Properties properties = new Properties();
		properties.setProperty("projectId", Long.toString(projectId));
		properties.setProperty("displayName", nullToEmpty(displayName));
		properties.setProperty("originalFileName", nullToEmpty(originalFileName));
		properties.setProperty("savedAt", Objects.requireNonNull(savedAt, "savedAt").toString());
		Path metadata = metadataPath(projectId);
		Path temporary = Files.createTempFile(directory, safeId(projectId), ".metadata.tmp");
		try {
			try (OutputStream output = Files.newOutputStream(temporary)) {
				properties.store(output, "ProjectLibre AutoRecovery");
			}
			try {
				Files.move(temporary, metadata, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
				Files.move(temporary, metadata, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public List<Entry> listRecoverable() throws IOException {
		if (!Files.isDirectory(directory)) {
			return List.of();
		}
		List<Entry> result = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
			for (Path metadata : files) {
				Entry entry = read(metadata);
				if (entry != null && entry.shouldOfferRecovery()) {
					result.add(entry);
				}
			}
		}
		result.sort(Comparator.comparing(Entry::savedAt).reversed());
		return List.copyOf(result);
	}

	public void discard(long projectId) throws IOException {
		Files.deleteIfExists(snapshotFile(projectId));
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

	private Entry read(Path metadata) {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(metadata)) {
			properties.load(input);
			long id = Long.parseLong(properties.getProperty("projectId"));
			Instant savedAt = Instant.parse(properties.getProperty("savedAt"));
			return new Entry(id, emptyToNull(properties.getProperty("displayName")),
				emptyToNull(properties.getProperty("originalFileName")), savedAt,
				snapshotFile(id), metadata);
		} catch (IOException | RuntimeException ex) {
			return null;
		}
	}

	private Path metadataPath(long projectId) throws IOException {
		Files.createDirectories(directory);
		return directory.resolve(safeId(projectId) + METADATA_SUFFIX);
	}

	private Path snapshotFile(long projectId) {
		return directory.resolve(safeId(projectId) + SNAPSHOT_SUFFIX);
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
