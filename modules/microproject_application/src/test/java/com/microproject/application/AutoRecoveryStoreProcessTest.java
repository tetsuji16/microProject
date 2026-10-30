/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the recovery lock with independent JVMs and operating-system file locks. */
class AutoRecoveryStoreProcessTest {
	@TempDir Path directory;
	private final java.util.Map<Process, Path> logs = new java.util.HashMap<>();

	@Test
	void competingProcessesClaimOnlyOnce() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		publish(store, "first");
		Process a = start("claim", "a");
		Process b = start("claim", "b");
		try {
			await("a.ready"); await("b.ready");
			signal("a.go"); signal("b.go");
			assertEquals(0, exit(a)); assertEquals(0, exit(b));
			assertEquals(java.util.Set.of("true", "false"), java.util.Set.of(Files.readString(directory.resolve("a.result")), Files.readString(directory.resolve("b.result"))));
			assertTrue(store.listRecoverable().isEmpty());
		} finally { a.destroyForcibly(); b.destroyForcibly(); }
	}

	@Test
	void publishedGenerationDoesNotPreventClaimingTheEarlierGeneration() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		publish(store, "first");
		Process claimant = start("claim", "claimant");
		try {
			await("claimant.ready");
			Path newer = publish(store, "newer");
			signal("claimant.go");
			assertEquals(0, exit(claimant));
			assertEquals("true", Files.readString(directory.resolve("claimant.result")));
			assertEquals(newer, store.listRecoverable().getFirst().snapshot());
		} finally { claimant.destroyForcibly(); }
	}

	@Test
	void concurrentPublicationAndClaimPreserveTheNewGeneration() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		publish(store, "initial");
		Process holder = start("hold", "holder");
		Process claimant = null;
		Process publisher = null;
		try {
			await("holder.ready");
			claimant = start("claim", "claimant"); publisher = start("publish", "publisher");
			await("claimant.ready"); await("publisher.ready");
			signal("claimant.go"); signal("publisher.go");
			assertFalse(claimant.waitFor(300, TimeUnit.MILLISECONDS));
			assertFalse(publisher.waitFor(300, TimeUnit.MILLISECONDS));
			holder.destroyForcibly(); assertTrue(holder.waitFor(5, TimeUnit.SECONDS));
			assertEquals(0, exit(claimant)); assertEquals(0, exit(publisher));
			assertTrue(java.util.Set.of("true", "false").contains(Files.readString(directory.resolve("claimant.result"))));
			assertTrue(store.listRecoverable().stream().anyMatch(entry -> { try { return Files.readString(entry.snapshot()).equals("new"); } catch (IOException ex) { throw new java.io.UncheckedIOException(ex); } }));
		} finally {
			holder.destroyForcibly();
			if (claimant != null) claimant.destroyForcibly();
			if (publisher != null) publisher.destroyForcibly();
		}
	}

	@Test
	void blockedPublicationRetriesAfterLockHolderDies() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		Path initial = publish(store, "initial");
		Process holder = start("hold", "holder");
		Process publisher = null;
		try {
			await("holder.ready");
			publisher = start("publish", "publisher");
			await("publisher.ready"); signal("publisher.go");
			assertFalse(publisher.waitFor(300, TimeUnit.MILLISECONDS), "publication must wait for the OS lock");
			assertEquals(initial, store.listRecoverable().getFirst().snapshot());
			holder.destroyForcibly(); assertTrue(holder.waitFor(5, TimeUnit.SECONDS));
			assertEquals(0, exit(publisher));
			assertTrue(store.listRecoverable().stream().anyMatch(entry -> { try { return Files.readString(entry.snapshot()).equals("new"); } catch (IOException ex) { throw new java.io.UncheckedIOException(ex); } }));
		} finally { holder.destroyForcibly(); if (publisher != null) publisher.destroyForcibly(); }
	}

	@Test
	void interruptionWhileWaitingLeavesGenerationAndAllowsRetry() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		Path initial = publish(store, "initial");
		Process holder = start("hold", "holder");
		Process waiter = null;
		try {
			await("holder.ready");
			waiter = start("interrupt", "waiter");
			await("waiter.ready"); signal("waiter.go");
			assertEquals(0, exit(waiter));
			assertEquals("interrupted", Files.readString(directory.resolve("waiter.result")));
			assertEquals(initial, store.listRecoverable().getFirst().snapshot());
			holder.destroyForcibly(); assertTrue(holder.waitFor(5, TimeUnit.SECONDS));
			assertTrue(store.claimOffered(store.listRecoverable().getFirst()));
		} finally { holder.destroyForcibly(); if (waiter != null) waiter.destroyForcibly(); }
	}

	private Path publish(AutoRecoveryStore store, String content) throws Exception {
		Path path = store.newSnapshotPath(11, true);
		Files.writeString(path, content);
		store.recordCompletedSnapshot(11, "Plan", null, Instant.now(), path);
		return path;
	}

	private Process start(String action, String name) throws Exception {
		String cp = Path.of(AutoRecoveryStoreProcessTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
			+ System.getProperty("path.separator") + Path.of(AutoRecoveryStore.class.getProtectionDomain().getCodeSource().getLocation().toURI())
			+ System.getProperty("path.separator") + Path.of(com.microproject.temporary.TemporaryCleanupQueue.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", cp,
			AutoRecoveryStoreProcessTest.class.getName(), directory.toString(), action, name)
			.redirectErrorStream(true).redirectOutput(directory.resolve(name + ".log").toFile()).start();
		logs.put(process, directory.resolve(name + ".log"));
		return process;
	}

	private int exit(Process process) throws Exception {
		assertTrue(process.waitFor(10, TimeUnit.SECONDS), "child timed out");
		assertEquals(0, process.exitValue(), () -> { try { return Files.readString(logs.get(process)); } catch (IOException ex) { return ex.toString(); } });
		return process.exitValue();
	}

	private void signal(String name) throws IOException { Files.writeString(directory.resolve(name), "go"); }
	private void await(String name) throws Exception { waitFor(directory.resolve(name)); }
	private static void waitFor(Path path) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!Files.exists(path) && System.nanoTime() < deadline) Thread.sleep(20);
		if (!Files.exists(path)) throw new IOException("Timed out waiting for " + path);
	}

	public static void main(String[] args) throws Exception {
		Path dir = Path.of(args[0]); String action = args[1]; String name = args[2];
		AutoRecoveryStore store = new AutoRecoveryStore(dir);
		if (action.equals("hold")) {
			try (FileChannel channel = FileChannel.open(dir.resolve(".recovery.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				var lock = channel.lock()) {
				Files.writeString(dir.resolve(name + ".ready"), "ready");
				Thread.sleep(TimeUnit.MINUTES.toMillis(1));
			}
			return;
		}
		AutoRecoveryStore.Entry candidate = action.equals("publish") ? null : store.listRecoverable().getFirst();
		Path snapshot = null;
		if (action.equals("publish")) { snapshot = store.newSnapshotPath(11, true); Files.writeString(snapshot, "new"); }
		Files.writeString(dir.resolve(name + ".ready"), "ready");
		waitFor(dir.resolve(name + ".go"));
		if (action.equals("claim")) Files.writeString(dir.resolve(name + ".result"), Boolean.toString(store.claimOffered(candidate)));
		else if (action.equals("publish")) store.recordCompletedSnapshot(11, "Plan", null, Instant.now(), snapshot);
		else {
			Thread worker = Thread.currentThread();
			Thread interrupter = new Thread(() -> { try { Thread.sleep(300); worker.interrupt(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } });
			interrupter.start();
			try { store.claimOffered(candidate); throw new AssertionError("claim unexpectedly succeeded"); }
			catch (IOException expected) {
				if (!Thread.currentThread().isInterrupted() || !(expected.getCause() instanceof InterruptedException)) throw expected;
				Thread.interrupted();
				Files.writeString(dir.resolve(name + ".result"), "interrupted");
			}
			interrupter.join();
		}
	}
}
