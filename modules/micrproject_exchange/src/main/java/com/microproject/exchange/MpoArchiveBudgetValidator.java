/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Side-effect-free archive-wide decompression, entry-count, and path-depth guard. */
public final class MpoArchiveBudgetValidator {
	/** Conservative limits shared by file, stream, and embedded MPOF readers. */
	public static final Budget DEFAULT_BUDGET = new Budget(128L * 1024L * 1024L, 128, 64);
	private MpoArchiveBudgetValidator() {
	}

	public record Budget(long maxDecompressedBytes, int maxEntries, int maxPathDepth) {
		public Budget {
			if (maxDecompressedBytes <= 0 || maxEntries <= 0 || maxPathDepth <= 0)
				throw new IllegalArgumentException("archive budget limits must be positive");
		}
	}

	public record Result(boolean valid, String reason, long decompressedBytes, int entries, int observedDepth) {
		public Result {
			reason = reason == null ? "" : reason;
		}
	}

	public static Result validate(Path source, Budget budget) {
		if (source == null) return invalid("source-null", 0, 0, 0);
		if (!Files.isRegularFile(source)) return invalid("source-missing", 0, 0, 0);
		Objects.requireNonNull(budget, "budget");
		long bytes = 0;
		int entries = 0;
		int depth = 0;
		try (ZipFile zip = new ZipFile(source.toFile(), StandardCharsets.UTF_8)) {
			var iterator = zip.entries();
			while (iterator.hasMoreElements()) {
				ZipEntry entry = iterator.nextElement();
				entries++;
				if (entries > budget.maxEntries()) return invalid("entry-count-limit", bytes, entries, depth);
				int entryDepth = pathDepth(entry.getName());
				if (entryDepth < 0) return invalid("invalid-entry-path", bytes, entries, depth);
				depth = Math.max(depth, entryDepth);
				if (depth > budget.maxPathDepth()) return invalid("path-depth-limit", bytes, entries, depth);
				if (entry.isDirectory()) continue;
				try (InputStream input = zip.getInputStream(entry)) {
					byte[] buffer = new byte[8192];
					int read;
					while ((read = input.read(buffer)) != -1) {
						if (read > budget.maxDecompressedBytes() - bytes)
							return invalid("decompressed-size-limit", bytes, entries, depth);
						bytes += read;
					}
				}
			}
			return new Result(true, "", bytes, entries, depth);
		} catch (IOException | RuntimeException failure) {
			return invalid(failure.getClass().getSimpleName(), bytes, entries, depth);
		}
	}

	/** Validates an already extracted nested MPOF without creating a temporary file. */
	public static Result validate(byte[] source, Budget budget) {
		if (source == null) return invalid("source-null", 0, 0, 0);
		Objects.requireNonNull(budget, "budget");
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source), StandardCharsets.UTF_8)) {
			long bytes = 0;
			int entries = 0;
			int depth = 0;
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				entries++;
				if (entries > budget.maxEntries()) return invalid("entry-count-limit", bytes, entries, depth);
				int entryDepth = pathDepth(entry.getName());
				if (entryDepth < 0) return invalid("invalid-entry-path", bytes, entries, depth);
				depth = Math.max(depth, entryDepth);
				if (depth > budget.maxPathDepth()) return invalid("path-depth-limit", bytes, entries, depth);
				if (!entry.isDirectory()) {
					byte[] buffer = new byte[8192];
					int read;
					while ((read = zip.read(buffer)) != -1) {
						if (read > budget.maxDecompressedBytes() - bytes)
							return invalid("decompressed-size-limit", bytes, entries, depth);
						bytes += read;
					}
				}
				zip.closeEntry();
			}
			return new Result(true, "", bytes, entries, depth);
		} catch (IOException | RuntimeException failure) {
			return invalid(failure.getClass().getSimpleName(), 0, 0, 0);
		}
	}

	/** Applies the path component of the budget while an importer streams an archive. */
	static void validateEntryName(String name, Budget budget) throws IOException {
		Objects.requireNonNull(budget, "budget");
		int depth = pathDepth(name);
		if (depth < 0) throw new IOException("MPOF archive budget rejected: invalid-entry-path");
		if (depth > budget.maxPathDepth())
			throw new IOException("MPOF archive budget rejected: path-depth-limit");
	}

	private static int pathDepth(String name) {
		if (name == null || name.isBlank() || name.startsWith("/") || name.startsWith("\\")
				|| (name.length() >= 2 && Character.isLetter(name.charAt(0)) && name.charAt(1) == ':')) return -1;
		String normalized = name.replace('\\', '/');
		int depth = 0;
		for (String component : normalized.split("/")) {
			if (component.isEmpty() || component.equals(".")) continue;
			if (component.equals("..")) return -1;
			depth++;
		}
		return depth;
	}

	private static Result invalid(String reason, long bytes, int entries, int depth) {
		return new Result(false, reason, bytes, entries, depth);
	}
}
