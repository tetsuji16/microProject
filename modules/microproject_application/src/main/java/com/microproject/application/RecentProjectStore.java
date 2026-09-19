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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Persistent recent/pinned projects and the last multi-document desktop session. */
public final class RecentProjectStore {
	public record Entry(Path path, long lastOpened, boolean pinned, boolean exists) { }
	private final Preferences root;

	public RecentProjectStore() {
		this(Preferences.userNodeForPackage(RecentProjectStore.class).node("recentProjects"));
	}
	RecentProjectStore(Preferences root) { this.root = root; }

	public void recordOpened(String fileName) {
		Path path = normalize(fileName); if (path == null || !Files.isRegularFile(path)) return;
		Preferences node = root.node("items").node(nodeKey(path));
		node.put("path", path.toString()); node.putLong("opened", System.currentTimeMillis());
		trim();
	}

	public List<Entry> entries() {
		List<Entry> result = new ArrayList<>();
		try {
			Preferences items = root.node("items");
			List<String> staleEntries = new ArrayList<>();
			for (String child : items.childrenNames()) {
				Preferences node = items.node(child); Path path = normalize(node.get("path", null));
				if (path == null || !Files.isRegularFile(path)) { staleEntries.add(child); continue; }
				result.add(new Entry(path, node.getLong("opened", 0L), node.getBoolean("pinned", false), true));
			}
			for (String child : staleEntries) {
				items.node(child).removeNode();
			}
		} catch (BackingStoreException ignored) { }
		result.sort(Comparator.comparing(Entry::pinned).reversed()
			.thenComparing(Comparator.comparingLong(Entry::lastOpened).reversed()));
		return List.copyOf(result);
	}

	public void setPinned(Path path, boolean pinned) {
		Path normalized = normalize(path == null ? null : path.toString()); if (normalized == null || !Files.isRegularFile(normalized)) return;
		Preferences node = root.node("items").node(nodeKey(normalized)); node.put("path", normalized.toString()); node.putBoolean("pinned", pinned);
	}

	public void remove(Path path) { Path normalized = normalize(path == null ? null : path.toString()); if (normalized == null) return; try { root.node("items").node(nodeKey(normalized)).removeNode(); } catch (BackingStoreException ignored) { } }

	private void trim() {
		List<Entry> removable = entries().stream().filter(entry -> !entry.pinned()).skip(20).toList();
		for (Entry entry : removable) remove(entry.path());
	}
	private static Path normalize(String fileName) { if (fileName == null || fileName.isBlank()) return null; try { return Path.of(fileName).toAbsolutePath().normalize(); } catch (RuntimeException error) { return null; } }
	private static String nodeKey(Path path) {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(path.toString().getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
	}
}
