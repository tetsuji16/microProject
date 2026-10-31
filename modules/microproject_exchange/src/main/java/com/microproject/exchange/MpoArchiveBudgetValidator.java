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
	private static final String DECOMPRESSED_SIZE_LIMIT = "decompressed-size-limit";

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
		Accounting accounting = new Accounting(budget);
		try (ZipFile zip = new ZipFile(source.toFile(), StandardCharsets.UTF_8)) {
			var iterator = zip.entries();
			while (iterator.hasMoreElements()) {
				ZipEntry entry = iterator.nextElement();
				String rejection = accounting.admit(entry);
				if (rejection != null) return accounting.invalid(rejection);
				if (entry.isDirectory()) continue;
				try (InputStream input = zip.getInputStream(entry)) {
					if (!accounting.drain(input)) return accounting.invalid(DECOMPRESSED_SIZE_LIMIT);
				}
			}
			return accounting.valid();
		} catch (IOException | RuntimeException failure) {
			return accounting.invalid(failure.getClass().getSimpleName());
		}
	}

	/** Validates an already extracted nested MPOF without creating a temporary file. */
	public static Result validate(byte[] source, Budget budget) {
		if (source == null) return invalid("source-null", 0, 0, 0);
		Objects.requireNonNull(budget, "budget");
		Accounting accounting = new Accounting(budget);
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String rejection = accounting.admit(entry);
				if (rejection != null) return accounting.invalid(rejection);
				if (!entry.isDirectory() && !accounting.drain(zip)) return accounting.invalid(DECOMPRESSED_SIZE_LIMIT);
				zip.closeEntry();
			}
			return accounting.valid();
		} catch (IOException | RuntimeException failure) {
			// A stream that cannot be read reports no measurements, unlike the file route.
			return invalid(failure.getClass().getSimpleName(), 0, 0, 0);
		}
	}

	/** The single implementation of the entry-count, path, and decompressed-size limits for both routes. */
	private static final class Accounting {
		private final Budget budget;
		private long bytes;
		private int entries;
		private int depth;

		Accounting(Budget budget) {
			this.budget = budget;
		}

		/** Counts one entry; returns the rejection reason, or null when the entry fits the budget. */
		String admit(ZipEntry entry) {
			entries++;
			if (entries > budget.maxEntries()) return "entry-count-limit";
			int entryDepth = pathDepth(entry.getName());
			if (entryDepth < 0) return "invalid-entry-path";
			depth = Math.max(depth, entryDepth);
			if (depth > budget.maxPathDepth()) return "path-depth-limit";
			return null;
		}

		/** Reads the current entry's decompressed bytes; false when the total would exceed the budget. */
		boolean drain(InputStream input) throws IOException {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				if (read > budget.maxDecompressedBytes() - bytes) return false;
				bytes += read;
			}
			return true;
		}

		Result valid() {
			return new Result(true, "", bytes, entries, depth);
		}

		Result invalid(String reason) {
			return MpoArchiveBudgetValidator.invalid(reason, bytes, entries, depth);
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
