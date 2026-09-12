/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Thread-safe registry used by core workflows to obtain format adapters. */
public final class SessionImporterRegistry {
	private final Map<String, Supplier<? extends SessionImporter>> factories = new ConcurrentHashMap<>();

	public void register(String key, Supplier<? extends SessionImporter> factory) {
		String normalized = requireKey(key);
		Objects.requireNonNull(factory, "factory");
		if (factories.putIfAbsent(normalized, factory) != null)
			throw new IllegalStateException("Session importer key already registered: " + normalized);
	}

	public SessionImporter create(String key) {
		String normalized = requireKey(key);
		Supplier<? extends SessionImporter> factory = factories.get(normalized);
		if (factory == null)
			throw new IllegalArgumentException("No session importer registered for key: " + normalized);
		return Objects.requireNonNull(factory.get(), "Session importer factory returned null: " + normalized);
	}

	public Set<String> keys() {
		return Set.copyOf(factories.keySet());
	}

	private static String requireKey(String key) {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("Session importer key must not be blank");
		return key;
	}
}
