/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atomic, data-safe replacement of a target file with a freshly written
 * temporary file.
 *
 * <p>Unlike the legacy {@code file.delete(); tmpFile.renameTo(file);} idiom,
 * this never deletes the original target before the move has actually
 * succeeded. A failed move therefore leaves the user's existing file intact
 * instead of silently discarding it and reporting success. This is the same
 * move idiom already used by the MPOF file importer.
 *
 * <p>Root cause of issue #354: the old save paths deleted the original and then
 * ignored the boolean result of {@link File#renameTo(File)}. When the rename
 * failed (file locked by an anti-virus scanner, the Windows Explorer preview
 * pane, OneDrive/Dropbox, or a cross-volume save), the original was already
 * gone, the method still reported success, and each subsequent save spawned a
 * new {@code _tmpN} file &mdash; unbounded growth. Centralizing the replace here
 * removes the duplicated, lossy responsibility.
 */
public final class SafeFileReplace {
	private static final Logger logger = Logger.getLogger(SafeFileReplace.class.getName());

	private SafeFileReplace() {
	}

	/**
	 * Move {@code temp} onto {@code target}, replacing any existing target.
	 *
	 * @return {@code true} if the move succeeded (the temp file no longer
	 *         exists and the target now holds its contents); {@code false} if
	 *         the move could not be performed, in which case {@code target} is
	 *         left untouched and the caller is responsible for the temp file.
	 */
	public static boolean replace(File temp, File target) {
		if (temp == null || target == null || temp.equals(target)) {
			return true;
		}
		try {
			moveAtomically(temp.toPath(), target.toPath(), true);
			return true;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Safe replace failed: could not move " + temp + " onto " + target, e);
			return false;
		}
	}

	/**
	 * Moves {@code source} to {@code target}, preferring an atomic move and falling
	 * back to a plain move when the file store cannot move atomically.
	 *
	 * @param replaceExisting whether an existing target may be overwritten; when
	 *        {@code false} the atomic move itself is platform-defined for an
	 *        existing target (Windows overwrites it), so pass {@code false} only
	 *        for targets that are expected not to exist
	 */
	public static void moveAtomically(Path source, Path target, boolean replaceExisting) throws IOException {
		StandardCopyOption[] options = replaceExisting
			? new StandardCopyOption[] { StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING }
			: new StandardCopyOption[] { StandardCopyOption.ATOMIC_MOVE };
		try {
			Files.move(source, target, options);
		} catch (AtomicMoveNotSupportedException e) {
			logger.log(Level.WARNING, "Atomic move not supported for " + target + "; using plain move", e);
			if (replaceExisting) {
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.move(source, target);
			}
		}
	}
}
