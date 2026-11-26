/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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

import com.microproject.preference.GlobalPreferences;
import com.microproject.util.VersionUtils;

class UpdateCheckerTest {
	@TempDir
	Path temporaryDirectory;
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) server.stop(0);
	}

	@Test
	void extractsTagNameFromGitHubJson() {
		String json = "{\"url\":\"x\",\"tag_name\": \"v0.0.24\",\"name\":\"release\"}";
		assertEquals("v0.0.24", UpdateChecker.extractTagName(json));
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
	void storeDistributionIsRecognizedFromTheSystemProperty() {
		String previous = System.getProperty("microproject.distribution");
		try {
			System.setProperty("microproject.distribution", "store");
			assertTrue(UpdateChecker.isStoreDistribution());
			System.setProperty("microproject.distribution", "STORE");
			assertTrue(UpdateChecker.isStoreDistribution());
			System.setProperty("microproject.distribution", "msi");
			assertFalse(UpdateChecker.isStoreDistribution());
			System.clearProperty("microproject.distribution");
			assertFalse(UpdateChecker.isStoreDistribution());
		} finally {
			if (previous == null) System.clearProperty("microproject.distribution");
			else System.setProperty("microproject.distribution", previous);
		}
	}

	@Test
	void storeDistributionStartsNoNetworkCheckButStillCleansLeftovers() throws Exception {
		Path updates = Files.createDirectories(temporaryDirectory.resolve("updates"));
		Path leftover = Files.createFile(updates.resolve("microProject-update-old.msi"));

		Thread check = UpdateChecker.checkInBackground(new GlobalPreferences(), true, updates);

		assertNull(check);
		assertFalse(Files.exists(leftover));
	}

	@Test
	void cleanupDeletesEveryLeftoverInstallerFileRegardlessOfAge() throws Exception {
		Path updates = Files.createDirectories(temporaryDirectory.resolve("updates"));
		Path fresh = Files.createFile(updates.resolve("microProject-update-fresh.msi.part"));
		Path old = Files.createFile(updates.resolve("microProject-update-old.msi"));
		Path manifest = Files.createFile(updates.resolve("microProject-update-old.msi.manifest"));
		Path unrelated = Files.createFile(updates.resolve("notes.txt"));
		Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(Duration.ofDays(30))));

		UpdateChecker.cleanupStaleArtifacts(updates);

		assertFalse(Files.exists(fresh));
		assertFalse(Files.exists(old));
		assertFalse(Files.exists(manifest));
		assertTrue(Files.exists(unrelated));
		assertTrue(Files.isDirectory(updates));
	}

	@Test
	void cleanupIgnoresAMissingFolder() {
		UpdateChecker.cleanupStaleArtifacts(temporaryDirectory.resolve("missing"));
		UpdateChecker.cleanupStaleArtifacts(null);
	}

	private void startResponse(int status, String body, long delayMillis) throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/release", exchange -> {
			try {
				// Fixed sleep kept: simulates a slow server response.
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

	private URI endpoint(String path) throws Exception {
			return URI.create("http://localhost:" + server.getAddress().getPort() + path);
		}
	}
