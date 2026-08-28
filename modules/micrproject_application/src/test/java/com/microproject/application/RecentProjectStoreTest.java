/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentProjectStoreTest {
	@TempDir java.nio.file.Path temp;
	@Test void recordsPinsAndRestoresExistingSessionFiles() throws Exception {
		Preferences prefs = Preferences.userRoot().node("projectlibre-test-" + UUID.randomUUID());
		try {
			RecentProjectStore store = new RecentProjectStore(prefs); var file = Files.createFile(temp.resolve("plan.pod"));
			store.recordOpened(file.toString()); store.setPinned(file, true); store.saveSession(List.of(file.toString(), temp.resolve("missing.pod").toString()));
			assertTrue(store.entries().getFirst().pinned()); assertEquals(List.of(file.toAbsolutePath()), store.session());
		} finally { prefs.removeNode(); }
	}

	/** MS Project conformance: pinned items sort above unpinned ones regardless of
	 *  recency, keep their pin across re-open, and are never evicted by trimming. */
	@Test void pinnedEntriesSortFirstSurviveTrimAndKeepPinOnReopen() throws Exception {
		Preferences prefs = Preferences.userRoot().node("projectlibre-test-" + UUID.randomUUID());
		try {
			RecentProjectStore store = new RecentProjectStore(prefs);
			var old = Files.createFile(temp.resolve("old.pod"));
			var newer = Files.createFile(temp.resolve("newer.pod"));
			store.recordOpened(old.toString());
			store.setPinned(old, true);
			store.recordOpened(newer.toString()); // more recent but NOT pinned
			List<RecentProjectStore.Entry> entries = store.entries();
			assertEquals(old.toAbsolutePath(), entries.getFirst().path(), "pinned item must sort above the more recent unpinned one");
			assertTrue(entries.getFirst().pinned());
			// re-opening a pinned file keeps the pin (MSP never unpins on open)
			store.recordOpened(old.toString());
			assertTrue(store.entries().stream().filter(e -> e.path().equals(old)).findFirst().orElseThrow().pinned());
			// trimming keeps pinned entries even when they would fall off the tail
			for (int i = 0; i < 25; i++) store.recordOpened(Files.createFile(temp.resolve("filler" + i + ".pod")).toString());
			final Path oldPath = old;
			assertTrue(store.entries().stream().anyMatch(e -> e.path().equals(oldPath)), "pinned entry must survive trim");
			assertEquals(1, store.entries().stream().filter(RecentProjectStore.Entry::pinned).count());
		} finally { prefs.removeNode(); }
	}

	@Test void excludesAndPurgesMissingRecentFilesIncludingPinnedEntries() throws Exception {
		Preferences prefs = Preferences.userRoot().node("projectlibre-test-" + UUID.randomUUID());
		try {
			RecentProjectStore store = new RecentProjectStore(prefs);
			Path existing = Files.createFile(temp.resolve("existing.pod"));
			store.recordOpened(existing.toString());
			Preferences stale = prefs.node("items").node("stale");
			stale.put("path", temp.resolve("missing.pod").toString());
			stale.putBoolean("pinned", true);

			assertEquals(List.of(existing.toAbsolutePath()), store.entries().stream().map(RecentProjectStore.Entry::path).toList());
			assertEquals(1, prefs.node("items").childrenNames().length, "stale preferences must be removed");
		} finally { prefs.removeNode(); }
	}
}
