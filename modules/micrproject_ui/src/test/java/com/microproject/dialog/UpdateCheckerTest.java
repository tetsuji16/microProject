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

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.VersionUtils;

class UpdateCheckerTest {
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

	private URL endpoint(String path) throws Exception {
		return new URL("http://localhost:" + server.getAddress().getPort() + path);
	}
}
