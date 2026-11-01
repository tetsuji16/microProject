/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An OS file lock on a marker file that can be removed once it is not needed.
 *
 * <p>Deleting a lock file is unsafe on its own: a process already waiting on
 * the old file would get its lock while a newcomer creates and locks a new file
 * at the same path.  {@link #retire()} therefore writes a retired mark before
 * deleting, and {@link #acquire} checks for that mark after it gets the lock and
 * starts over on a retired file.  Empty markers, including those from older
 * releases, are live.
 */
public final class RetirableFileLock implements AutoCloseable {
	private static final Logger logger = Logger.getLogger(RetirableFileLock.class.getName());
	private static final int ACQUIRE_ATTEMPTS = 100;
	private static final long RETRY_MILLIS = 10L;
	private static final byte RETIRED_MARK = 'R';

	/** Takes the OS lock, which lets each caller apply its own waiting policy. */
	public interface Acquirer {
		FileLock acquire(FileChannel channel) throws IOException;
	}

	private final Path path;
	private final FileChannel channel;
	private final FileLock lock;

	private RetirableFileLock(Path path, FileChannel channel, FileLock lock) {
		this.path = path;
		this.channel = channel;
		this.lock = lock;
	}

	public static RetirableFileLock acquire(Path path, Acquirer acquirer) throws IOException {
		for (int attempt = 1; ; attempt++) {
			FileChannel channel;
			try {
				channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
			} catch (AccessDeniedException | NoSuchFileException retiring) {
				// Windows refuses new handles while a deleted marker is still open.
				if (attempt >= ACQUIRE_ATTEMPTS) throw retiring;
				pause();
				continue;
			}
			try {
				FileLock lock = acquirer.acquire(channel);
				if (channel.size() > 0L) {
					if (attempt < ACQUIRE_ATTEMPTS) {
						channel.close();
						pause();
						continue;
					}
					// The mark outlived every retry, so this is a copy restored by a
					// sync client rather than a retirement in progress.
					channel.truncate(0L);
				}
				return new RetirableFileLock(path, channel, lock);
			} catch (IOException | RuntimeException failure) {
				closeQuietly(channel);
				throw failure;
			}
		}
	}

	/** Removes an unheld marker, such as one left by an older release. */
	public static boolean retireIfUnheld(Path path) {
		if (!Files.isRegularFile(path)) return false;
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			FileLock lock = channel.tryLock();
			if (lock == null || channel.size() > 0L) return false;
			return new RetirableFileLock(path, channel, lock).retire();
		} catch (IOException | OverlappingFileLockException busy) {
			logger.log(Level.FINE, "Lock marker stays in place: " + path, busy);
			return false;
		}
	}

	/** Removes the marker while still holding it. A marker that cannot be deleted stays live. */
	public boolean retire() {
		try {
			channel.write(ByteBuffer.wrap(new byte[] { RETIRED_MARK }), 0L);
			try {
				Files.deleteIfExists(path);
				return true;
			} catch (IOException deleteFailure) {
				channel.truncate(0L);
				logger.log(Level.FINE, "Lock marker stays in place: " + path, deleteFailure);
				return false;
			}
		} catch (IOException failure) {
			logger.log(Level.FINE, "Lock marker stays in place: " + path, failure);
			return false;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			lock.release();
		} finally {
			channel.close();
		}
	}

	private static void pause() throws IOException {
		try {
			TimeUnit.MILLISECONDS.sleep(RETRY_MILLIS);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for lock marker", interrupted);
		}
	}

	private static void closeQuietly(FileChannel channel) {
		try {
			channel.close();
		} catch (IOException closeFailure) {
			logger.log(Level.FINE, "Could not close lock marker", closeFailure);
		}
	}
}
