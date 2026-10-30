/* MIT License. Copyright (c) 2026 microProject. */
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoRecoveryLegacyProcessTest {
	@TempDir Path directory;

	@Test
	void legacyOfferedMetadataRemainsClaimableAcrossProcesses() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		Path snapshot = store.snapshotPath(11); Files.writeString(snapshot, "legacy");
		store.recordCompletedSnapshot(11, "Plan", null, Instant.now());
		Path metadata = store.listRecoverable().getFirst().metadata();
		Properties properties = new Properties();
		try (var input = Files.newInputStream(metadata)) { properties.load(input); }
		properties.setProperty("offered", "true");
		try (var output = Files.newOutputStream(metadata)) { properties.store(output, "legacy"); }
		Process child = start("legacy");
		try {
			await("legacy.ready"); signal("legacy.go"); finish(child, "legacy");
			assertEquals("true", Files.readString(directory.resolve("legacy.result")));
			assertTrue(store.listRecoverable().isEmpty());
		} finally { child.destroyForcibly(); }
	}

	@Test
	void staleProcessCannotClaimReplacedLegacyGeneration() throws Exception {
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		Path snapshot = store.snapshotPath(11); Files.writeString(snapshot, "old");
		store.recordCompletedSnapshot(11, "Plan", null, Instant.parse("2026-01-01T00:00:00Z"));
		Process child = start("stale");
		try {
			await("stale.ready"); Files.writeString(snapshot, "new");
			store.recordCompletedSnapshot(11, "Plan", null, Instant.parse("2026-01-02T00:00:00Z"));
			signal("stale.go"); finish(child, "stale");
			assertEquals("false", Files.readString(directory.resolve("stale.result")));
			assertEquals(Instant.parse("2026-01-02T00:00:00Z"), store.listRecoverable().getFirst().savedAt());
		} finally { child.destroyForcibly(); }
	}

	private Process start(String name) throws Exception {
		String separator = System.getProperty("path.separator");
		String cp = location(AutoRecoveryLegacyProcessTest.class) + separator + location(AutoRecoveryStore.class)
			+ separator + location(com.microproject.temporary.TemporaryCleanupQueue.class);
		return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", cp,
			AutoRecoveryLegacyProcessTest.class.getName(), directory.toString(), name)
			.redirectErrorStream(true).redirectOutput(directory.resolve(name + ".log").toFile()).start();
	}
	private static Path location(Class<?> type) throws Exception { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()); }
	private void signal(String file) throws Exception { Files.writeString(directory.resolve(file), "go"); }
	private void await(String file) throws Exception { waitFor(directory.resolve(file)); }
	private static void waitFor(Path file) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!Files.exists(file) && System.nanoTime() < deadline) Thread.sleep(20);
		if (!Files.exists(file)) throw new java.io.IOException("Timed out waiting for " + file);
	}
	private void finish(Process process, String name) throws Exception {
		assertTrue(process.waitFor(10, TimeUnit.SECONDS), "child timed out");
		assertEquals(0, process.exitValue(), Files.readString(directory.resolve(name + ".log")));
	}
	public static void main(String[] args) throws Exception {
		Path directory = Path.of(args[0]); String name = args[1];
		AutoRecoveryStore store = new AutoRecoveryStore(directory);
		var candidate = store.listRecoverable().getFirst();
		Files.writeString(directory.resolve(name + ".ready"), "ready");
		waitFor(directory.resolve(name + ".go"));
		Files.writeString(directory.resolve(name + ".result"), Boolean.toString(store.claimOffered(candidate)));
	}
}
