/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Type-safe factory registry for project importers and exporters.
 *
 * The legacy file-format option remains a string at the persistence boundary,
 * but construction is performed through registered typed factories. This
 * prevents callers from loading arbitrary classes by name.
 */
public final class ImporterRegistry {
	private final Map<String, Supplier<? extends FileImporter>> factories = new ConcurrentHashMap<>();

	/** Registers a factory for one stable, backwards-compatible importer key. */
	public void register(String key, Supplier<? extends FileImporter> factory) {
		String normalizedKey = requireKey(key);
		Objects.requireNonNull(factory, "factory");
		Supplier<? extends FileImporter> previous = factories.putIfAbsent(normalizedKey, factory);
		if (previous != null) {
			throw new IllegalStateException("Importer key already registered: " + normalizedKey);
		}
	}

	/** Creates a fresh importer instance for a previously registered key. */
	public FileImporter create(String key) {
		String normalizedKey = requireKey(key);
		Supplier<? extends FileImporter> factory = factories.get(normalizedKey);
		if (factory == null)
			throw new IllegalArgumentException("No importer registered for key: " + normalizedKey);
		FileImporter importer = factory.get();
		return Objects.requireNonNull(importer, "Importer factory returned null: " + normalizedKey);
	}

	/** Returns an immutable view of the registered keys for diagnostics/tests. */
	public Set<String> keys() {
		return Set.copyOf(factories.keySet());
	}

	private static String requireKey(String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Importer key must not be blank");
		}
		return key;
	}

}
