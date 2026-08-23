/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

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
}
