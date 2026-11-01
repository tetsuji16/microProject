/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.util.List;
import java.util.function.Supplier;

import com.microproject.port.PortRegistry;
import com.microproject.port.SessionImporterProvider;
import com.microproject.port.SessionImporterRegistry;
import com.microproject.session.LocalSession;

/** Registers the concrete exchange implementations with the core registry. */
public final class DefaultFileImporterProvider implements SessionImporterProvider {
	private static final List<FormatDefinition> FORMATS = List.of(
			new FormatDefinition(LocalSession.LOCAL_PROJECT_IMPORTER, LocalFileImporter::new),
			new FormatDefinition(LocalSession.MPO_PROJECT_IMPORTER, MpoFileImporter::new),
			new FormatDefinition(LocalSession.MICROSOFT_PROJECT_IMPORTER, MicrosoftImporter::new));


	@Override
	public void registerPorts(PortRegistry registry) {
		registry.registerArtifactLifecycle(new MpoProjectArtifactLifecycleAdapter());
		for (FormatDefinition format : FORMATS) {
			registry.registerImport(new FileImporterPortAdapter(format.key(), format.factory()));
			registry.registerExport(new FileImporterPortAdapter(format.key(), format.factory()));
		}
	}

	/** Registers the legacy job-based session adapters for LocalSession. */
	@Override
	public void register(SessionImporterRegistry registry) {
		for (FormatDefinition format : FORMATS)
			registry.register(format.key(), format.factory()::get);
		// Persisted POD options used class names before the typed boundary existed.
		// Keep these aliases separate from stable format keys and resolve their factory
		// through the canonical definition above.
		PodFormatIdentifiers.READ_IMPORTER_ALIASES.forEach((alias, key) ->
			registry.register(alias, factoryFor(key)::get));
	}

	private static Supplier<? extends FileImporter> factoryFor(String key) {
		return FORMATS.stream().filter(format -> format.key().equals(key)).findFirst()
				.orElseThrow(() -> new IllegalStateException("Unknown importer format key: " + key)).factory();
	}

	private record FormatDefinition(String key, Supplier<? extends FileImporter> factory) {}

}
