/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RetirableFileLockTest {
	private static final int WRITERS = 3;
	private static final int ROUNDS = 40;

	/** Real processes, since one JVM cannot hold two OS locks on a file. */
	@Test
	void retiringAfterEveryTransactionKeepsWritersMutuallyExclusiveAcrossProcesses(@TempDir Path directory) throws Exception {
		Path marker = directory.resolve("project.mpo.lock");
		Path counter = directory.resolve("counter");
		Files.writeString(counter, "0");
		String java = ProcessHandle.current().info().command().orElseThrow();
		List<Process> writers = new ArrayList<>();
		for (int writer = 0; writer < WRITERS; writer++) {
			writers.add(new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Writer.class.getName(),
				marker.toString(), counter.toString()).redirectErrorStream(true)
				.redirectOutput(directory.resolve("writer" + writer + ".log").toFile()).start());
		}
		for (int writer = 0; writer < WRITERS; writer++) {
			assertTrue(writers.get(writer).waitFor(2, TimeUnit.MINUTES), "writer did not finish");
			assertEquals(0, writers.get(writer).exitValue(), () -> Files.exists(directory.resolve("writer0.log")) ? read(directory.resolve("writer0.log")) : "");
		}
		assertEquals(WRITERS * ROUNDS, Integer.parseInt(Files.readString(counter)), "an update was lost, so two writers overlapped");
		assertFalse(Files.exists(marker), "the last transaction must leave no marker");
	}

	/** The race retirement exists for: a waiter's lock arrives after its file was retired. */
	@Test
	void waiterWhoseMarkerWasRetiredStartsOverOnALiveMarker(@TempDir Path directory) throws Exception {
		Path marker = directory.resolve("project.mpo.lock");
		boolean[] retiredOnce = { false };
		try (RetirableFileLock waiter = RetirableFileLock.acquire(marker, channel -> {
			if (!retiredOnce[0]) {
				retiredOnce[0] = true;
				try (RetirableFileLock holder = RetirableFileLock.acquire(marker, FileChannel::lock)) {
					holder.retire();
				}
			}
			return channel.lock();
		})) {
			assertTrue(Files.isRegularFile(marker), "the winner must hold a marker that newcomers can see");
			assertEquals(0L, Files.size(marker), "a held marker must read as live");
		}
	}

	@Test
	void markerRestoredWithItsRetiredMarkBecomesLiveAgain(@TempDir Path directory) throws Exception {
		Path marker = directory.resolve("project.mpo.lock");
		Files.writeString(marker, "R");
		try (RetirableFileLock lock = RetirableFileLock.acquire(marker, FileChannel::lock)) {
			assertEquals(0L, Files.size(marker));
		}
	}

	@Test
	void onlyAnUnheldMarkerFromAnOlderReleaseIsRemoved(@TempDir Path directory) throws Exception {
		Path marker = directory.resolve("project.mpo.lock");
		assertFalse(RetirableFileLock.retireIfUnheld(marker), "there is nothing to remove");
		Files.createFile(marker);
		try (FileChannel channel = FileChannel.open(marker, StandardOpenOption.WRITE); FileLock held = channel.lock()) {
			assertFalse(RetirableFileLock.retireIfUnheld(marker));
			assertTrue(Files.isRegularFile(marker));
		}
		assertTrue(RetirableFileLock.retireIfUnheld(marker));
		assertFalse(Files.exists(marker));
	}

	private static String read(Path log) {
		try {
			return Files.readString(log);
		} catch (java.io.IOException unreadable) {
			return unreadable.toString();
		}
	}

	public static final class Writer {
		public static void main(String[] arguments) throws Exception {
			Path marker = Path.of(arguments[0]);
			Path counter = Path.of(arguments[1]);
			for (int round = 0; round < ROUNDS; round++) {
				try (RetirableFileLock lock = RetirableFileLock.acquire(marker, FileChannel::lock)) {
					int value = Integer.parseInt(Files.readString(counter));
					// Fixed sleep kept: widens the critical section on purpose so a broken lock loses an update.
					Thread.sleep(1L);
					Files.writeString(counter, Integer.toString(value + 1));
					lock.retire();
				}
			}
		}
	}
}
