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
 * move idiom already used by {@code PodxFileImporter} / {@code MpoFileImporter}.
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
		Path tempPath = temp.toPath();
		Path targetPath = target.toPath();
		try {
			Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			return true;
		} catch (AtomicMoveNotSupportedException e) {
			try {
				Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				return true;
			} catch (IOException ioe) {
				logger.log(Level.WARNING, "Safe replace failed: could not move " + temp + " onto " + target, ioe);
				return false;
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Safe replace failed: could not move " + temp + " onto " + target, e);
			return false;
		}
	}
}
