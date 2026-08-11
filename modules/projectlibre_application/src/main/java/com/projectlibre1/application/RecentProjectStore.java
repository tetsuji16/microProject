package com.projectlibre1.application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
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
		Path path = normalize(fileName); if (path == null) return;
		Preferences node = root.node("items").node(nodeKey(path));
		node.put("path", path.toString()); node.putLong("opened", System.currentTimeMillis());
		trim();
	}

	public List<Entry> entries() {
		List<Entry> result = new ArrayList<>();
		try {
			Preferences items = root.node("items");
			for (String child : items.childrenNames()) {
				Preferences node = items.node(child); Path path = normalize(node.get("path", null)); if (path == null) continue;
				result.add(new Entry(path, node.getLong("opened", 0L), node.getBoolean("pinned", false), Files.isRegularFile(path)));
			}
		} catch (BackingStoreException ignored) { }
		result.sort(Comparator.comparing(Entry::pinned).reversed()
			.thenComparing(Comparator.comparingLong(Entry::lastOpened).reversed()));
		return List.copyOf(result);
	}

	public void setPinned(Path path, boolean pinned) {
		Path normalized = normalize(path == null ? null : path.toString()); if (normalized == null) return;
		Preferences node = root.node("items").node(nodeKey(normalized)); node.put("path", normalized.toString()); node.putBoolean("pinned", pinned);
	}

	public void remove(Path path) { Path normalized = normalize(path == null ? null : path.toString()); if (normalized == null) return; try { root.node("items").node(nodeKey(normalized)).removeNode(); } catch (BackingStoreException ignored) { } }

	public void saveSession(List<String> fileNames) {
		List<String> encoded = new ArrayList<>();
		for (String name : fileNames) { Path path = normalize(name); if (path != null && Files.isRegularFile(path)) encoded.add(encodePath(path)); }
		root.put("session", String.join(",", encoded));
	}

	public List<Path> session() {
		List<Path> result = new ArrayList<>(); String raw = root.get("session", ""); if (raw.isBlank()) return result;
		for (String encoded : raw.split(",")) try { Path path = Path.of(new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)); if (Files.isRegularFile(path)) result.add(path); } catch (RuntimeException ignored) { }
		return List.copyOf(result);
	}

	public boolean isRestoreSessionEnabled() { return root.getBoolean("restoreSession", true); }
	public void setRestoreSessionEnabled(boolean enabled) { root.putBoolean("restoreSession", enabled); }

	private void trim() {
		List<Entry> removable = entries().stream().filter(entry -> !entry.pinned()).skip(20).toList();
		for (Entry entry : removable) remove(entry.path());
	}
	private static Path normalize(String fileName) { if (fileName == null || fileName.isBlank()) return null; try { return Path.of(fileName).toAbsolutePath().normalize(); } catch (RuntimeException error) { return null; } }
	private static String encodePath(Path path) { return Base64.getUrlEncoder().withoutPadding().encodeToString(path.toString().getBytes(StandardCharsets.UTF_8)); }
	private static String nodeKey(Path path) {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(path.toString().getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
	}
}
