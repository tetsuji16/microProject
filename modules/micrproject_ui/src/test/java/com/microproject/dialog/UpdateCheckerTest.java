/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.util.VersionUtils;

class UpdateCheckerTest {
	@TempDir
	Path temporaryDirectory;
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) server.stop(0);
		UpdateChecker.discardStagedUpdate();
	}

	@Test
	void extractsTagNameFromGitHubJson() {
		String json = "{\"url\":\"x\",\"tag_name\": \"v0.0.24\",\"name\":\"release\"}";
		assertEquals("v0.0.24", UpdateChecker.extractTagName(json));
	}

	@Test
	void prefersTheStableLatestMsiAsset() {
		String digest = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
		String json = "{\"assets\":[{\"browser_download_url\":\"https://x/microProject-0.0.24.msi\",\"digest\":\"sha256:" + digest + "\"},{\"browser_download_url\":\"https://x/microProject-latest.msi\",\"digest\":\"sha256:" + digest + "\"}]}";
		assertEquals("https://x/microProject-latest.msi", UpdateChecker.extractMsiDownloadUrl(json));
	}

	@Test
	void returnsNullWhenTagNameMissing() {
		assertNull(UpdateChecker.extractTagName("{\"name\":\"release\"}"));
	}

	@Test
	void detectsNewerVersions() {
		assertTrue(UpdateChecker.isNewer("v0.0.24", "0.0.23"));
		assertTrue(UpdateChecker.isNewer("0.1.0", "v0.0.99"));
		assertTrue(UpdateChecker.isNewer("0.0.23.140", "0.0.23"));
		assertTrue(UpdateChecker.isNewer("1.0", "0.9.9"));
	}

	@Test
	void sameOrOlderVersionsAreNotNewer() {
		assertFalse(UpdateChecker.isNewer("v0.0.23", "0.0.23"));
		assertFalse(UpdateChecker.isNewer("0.0.22", "0.0.23"));
		assertFalse(UpdateChecker.isNewer("0.0.23", "0.0.23.140"));
	}

	@Test
	void unparsableVersionsNeverTriggerUpgrade() {
		assertFalse(UpdateChecker.isNewer("snapshot", "0.0.23"));
		assertFalse(UpdateChecker.isNewer("0.0.24", "dev-build"));
	}

	@Test
	void enablesReleaseChecksOnlyForReleaseBuilds() {
		assertEquals(!VersionUtils.isDevelopmentBuild(), UpdateChecker.isUpdateCheckEnabled());
	}

	@Test
	void sharedTransportReadsReleaseJsonAndUsesCommonHeaders() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/release", exchange -> {
			assertEquals("application/vnd.github+json", exchange.getRequestHeaders().getFirst("Accept"));
			assertEquals("microProject-update-checker", exchange.getRequestHeaders().getFirst("User-Agent"));
			byte[] response = "{\"tag_name\":\"v0.0.24\"}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			try (var output = exchange.getResponseBody()) {
				output.write(response);
			}
		});
		server.start();

		String json = UpdateChecker.fetchLatestReleaseJson(endpoint("/release"), 1000);

		assertEquals("v0.0.24", UpdateChecker.extractTagName(json));
	}

	@Test
	void sharedTransportRejectsHttpErrors() throws Exception {
		startResponse(404, "not found", 0);

		assertThrows(IOException.class, () -> UpdateChecker.fetchLatestReleaseJson(endpoint("/release"), 1000));
	}

	@Test
	void sharedTransportRejectsOversizedResponses() throws Exception {
		startResponse(200, "x".repeat(1024 * 1024 + 1), 0);

		assertThrows(IOException.class, () -> UpdateChecker.fetchLatestReleaseJson(endpoint("/release"), 1000));
	}

	@Test
	void malformedJsonIsReturnedToSharedParserWithoutTriggeringAnUpdate() throws Exception {
		startResponse(200, "{\"name\":\"release\"}", 0);

		assertNull(UpdateChecker.extractTagName(UpdateChecker.fetchLatestReleaseJson(endpoint("/release"), 1000)));
	}

	@Test
	void sharedTransportHonorsReadTimeout() throws Exception {
		startResponse(200, "{\"tag_name\":\"v0.0.24\"}", 250);

		assertThrows(IOException.class, () -> UpdateChecker.fetchLatestReleaseJson(endpoint("/release"), 50));
	}

	@Test
	void stagesCompleteInstallerInDedicatedDirectoryAndLeavesNoPartFile() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		byte[] installer = "fake-msi".getBytes(StandardCharsets.UTF_8);
		String digest = sha256(installer);
		startBinaryResponse(200, installer);

		Path staged = UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), digest), "v0.0.24", staging);

		assertEquals(staging, staged.getParent());
		assertTrue(Files.isRegularFile(staged));
		String manifest = Files.readString(staged.resolveSibling(staged.getFileName() + ".manifest"));
		assertTrue(manifest.contains("version=v0.0.24\n"));
		assertTrue(manifest.contains("sha256=" + digest + "\n"));
		assertTrue(manifest.contains("createdAt=") && manifest.endsWith("\n"));
		try (var files = Files.list(staging)) {
			assertEquals(1, files.filter(path -> path.getFileName().toString().endsWith(".msi")).count());
		}
		assertFalse(Files.exists(staged.resolveSibling(staged.getFileName() + ".part")));
	}

	@Test
	void failedDownloadRemovesBothPartialAndPublishedArtifacts() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		startBinaryResponse(200, "not-the-expected-installer".getBytes(StandardCharsets.UTF_8));

		assertThrows(IOException.class, () -> UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), "0".repeat(64)), "v0.0.24", staging));
		try (var files = Files.list(staging)) {
			assertEquals(0, files.count());
		}
	}

	@Test
	void successfulInstallerProcessCleansPublishedArtifacts() throws Exception {
		Path staging = temporaryDirectory.resolve("staging-success");
		byte[] installer = "fake-msi-success".getBytes(StandardCharsets.UTF_8);
		startBinaryResponse(200, installer);
		Path staged = UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), sha256(installer)), "v0.0.24", staging);

		assertTrue(UpdateChecker.applyStagedUpdateForTest(path -> completedProcess(0)));
		assertFalse(Files.exists(staged));
		assertFalse(Files.exists(staged.resolveSibling(staged.getFileName() + ".manifest")));
	}

	@Test
	void installerLaunchFailureCleansPublishedArtifacts() throws Exception {
		Path staging = temporaryDirectory.resolve("staging-launch-failure");
		byte[] installer = "fake-msi-launch-failure".getBytes(StandardCharsets.UTF_8);
		startBinaryResponse(200, installer);
		Path staged = UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), sha256(installer)), "v0.0.24", staging);

		assertFalse(UpdateChecker.applyStagedUpdateForTest(path -> {
			throw new IOException("Defender denied installer launch");
		}));
		assertFalse(Files.exists(staged));
		assertFalse(Files.exists(staged.resolveSibling(staged.getFileName() + ".manifest")));
	}

	@Test
	void nonzeroInstallerExitRetainsArtifactsForRetryAndCancellationCanRemoveThem() throws Exception {
		Path staging = temporaryDirectory.resolve("staging-nonzero");
		byte[] installer = "fake-msi-nonzero".getBytes(StandardCharsets.UTF_8);
		startBinaryResponse(200, installer);
		Path staged = UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), sha256(installer)), "v0.0.24", staging);

		assertFalse(UpdateChecker.applyStagedUpdateForTest(path -> completedProcess(1603)));
		assertTrue(Files.exists(staged), "failed MSI must remain retryable");
		assertTrue(Files.exists(staged.resolveSibling(staged.getFileName() + ".manifest")));
		UpdateChecker.discardStagedUpdate();
		assertFalse(Files.exists(staged));
	}

	private static Process completedProcess(int exitCode) throws IOException {
		if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"))
			return new ProcessBuilder("cmd.exe", "/c", "exit " + exitCode).start();
		return new ProcessBuilder("sh", "-c", "exit " + exitCode).start();
	}

	@Test
	void staleArtifactsAreRemovedButFreshArtifactsAreRetained() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		Files.createDirectories(staging);
		Path stalePart = Files.createFile(staging.resolve("microProject-update-old.msi.part"));
		Path staleInstaller = Files.createFile(staging.resolve("microProject-update-old.msi"));
		Path fresh = Files.createFile(staging.resolve("microProject-update-fresh.msi.part"));
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.setLastModifiedTime(stalePart, FileTime.from(now.minus(Duration.ofDays(8))));
		Files.setLastModifiedTime(staleInstaller, FileTime.from(now.minus(Duration.ofDays(8))));
		Files.setLastModifiedTime(fresh, FileTime.from(now.minus(Duration.ofDays(1))));

		UpdateChecker.cleanupStaleArtifacts(staging, now, Duration.ofDays(7));

		assertFalse(Files.exists(stalePart));
		assertFalse(Files.exists(staleInstaller));
		assertTrue(Files.exists(fresh));
	}

	@Test
	void decliningUpdateDeletesStagedInstaller() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		byte[] installer = "fake-msi".getBytes(StandardCharsets.UTF_8);
		startBinaryResponse(200, installer);
		Path staged = UpdateChecker.stageInstaller(
				new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), sha256(installer)), "v0.0.24", staging);

		UpdateChecker.discardStagedUpdate();

		assertFalse(Files.exists(staged));
		assertFalse(Files.exists(staged.resolveSibling(staged.getFileName() + ".manifest")));
	}

	@Test
	void concurrentStagingPublishesOneConsistentState() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		byte[] installer = "fake-msi".getBytes(StandardCharsets.UTF_8);
		startBinaryResponse(200, installer);
		var asset = new UpdateChecker.MsiAsset(endpoint("/installer").toExternalForm(), sha256(installer));
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Path> first = executor.submit(() -> UpdateChecker.stageInstaller(asset, "v0.0.24", staging));
			Future<Path> second = executor.submit(() -> UpdateChecker.stageInstaller(asset, "v0.0.24", staging));
			assertEquals(first.get(), second.get());
		} finally {
			executor.shutdownNow();
		}
		try (var files = Files.list(staging)) {
			assertEquals(1, files.filter(path -> path.getFileName().toString().endsWith(".msi")).count());
		}
		try (var files = Files.list(staging)) {
			assertEquals(1, files.filter(path -> path.getFileName().toString().endsWith(".manifest")).count());
		}
	}

	@Test
	void failedDeletionLeavesMarkerAndStartupCleanupRetriesIt() throws Exception {
		Path staging = temporaryDirectory.resolve("staging");
		Files.createDirectories(staging);
		Path blocked = Files.createDirectory(staging.resolve("microProject-update-blocked.msi"));
		Files.writeString(blocked.resolve("still-open"), "data");
		Instant now = Instant.parse("2026-09-12T00:00:00Z");
		Files.setLastModifiedTime(blocked, FileTime.from(now.minus(Duration.ofDays(8))));

		UpdateChecker.cleanupStaleArtifacts(staging, now, Duration.ofDays(7));

		Path marker = staging.resolve("microProject-update-blocked.msi.delete");
		assertTrue(Files.exists(blocked));
		assertTrue(Files.exists(marker));
		Files.delete(blocked.resolve("still-open"));

		UpdateChecker.cleanupStaleArtifacts(staging, now, Duration.ofDays(7));

		assertFalse(Files.exists(blocked));
		assertFalse(Files.exists(marker));
	}

	private void startResponse(int status, String body, long delayMillis) throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/release", exchange -> {
			try {
				if (delayMillis > 0) Thread.sleep(delayMillis);
				byte[] response = body.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(status, response.length);
				try (var output = exchange.getResponseBody()) {
					output.write(response);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		server.start();
	}

	private void startBinaryResponse(int status, byte[] body) throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/installer", exchange -> {
			exchange.sendResponseHeaders(status, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();
	}

	private static String sha256(byte[] bytes) throws Exception {
		var digest = java.security.MessageDigest.getInstance("SHA-256");
		byte[] result = digest.digest(bytes);
		var hex = new StringBuilder(result.length * 2);
		for (byte value : result) hex.append(String.format(java.util.Locale.ROOT, "%02x", value));
		return hex.toString();
	}

	private URL endpoint(String path) throws Exception {
		return new URL("http://localhost:" + server.getAddress().getPort() + path);
	}
}
