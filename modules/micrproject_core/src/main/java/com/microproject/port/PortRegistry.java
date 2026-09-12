/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry for format ports, independent of exchange classes. */
public final class PortRegistry {
	private final Map<String, ImportPort> imports = new ConcurrentHashMap<>();
	private final Map<String, ExportPort> exports = new ConcurrentHashMap<>();

	public void registerImport(ImportPort port) {
		Objects.requireNonNull(port, "port");
		register(imports, port.formatKey(), port);
	}

	public void registerExport(ExportPort port) {
		Objects.requireNonNull(port, "port");
		register(exports, port.formatKey(), port);
	}

	public ImportPort importPort(String formatKey) {
		return imports.get(requireKey(formatKey));
	}

	public ExportPort exportPort(String formatKey) {
		return exports.get(requireKey(formatKey));
	}

	public Set<String> importKeys() { return Set.copyOf(imports.keySet()); }
	public Set<String> exportKeys() { return Set.copyOf(exports.keySet()); }

	private static <T> void register(Map<String, T> values, String key, T value) {
		String normalized = requireKey(key);
		if (values.putIfAbsent(normalized, value) != null)
			throw new IllegalStateException("Port key already registered: " + normalized);
	}

	private static String requireKey(String key) {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("Port key must not be blank");
		return key;
	}
}
