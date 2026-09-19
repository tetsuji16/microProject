/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import com.microproject.session.LocalSession;
import com.microproject.port.PortRegistry;
import com.microproject.port.SessionImporterProvider;
import com.microproject.port.SessionImporterRegistry;

/** Registers the concrete exchange implementations with the core registry. */
public final class DefaultFileImporterProvider implements FileImporterProvider, SessionImporterProvider {
	@Override
	public void register(ImporterRegistry registry) {
		registry.register(LocalSession.MPO_PROJECT_IMPORTER, MpoFileImporter::new);
		registry.register(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ServerLocalFileImporter::new);
		registry.register(LocalSession.MICROSOFT_PROJECT_IMPORTER, MicrosoftImporter::new);
		// Persisted POD options used class names before the typed boundary existed.
		// Keep those aliases beside the concrete format adapter, never in core.
		registry.register("com.microproject.exchange.LocalFileImporter", LocalFileImporter::new);
		registry.register("com.projectlibre1.exchange.LocalFileImporter", LocalFileImporter::new);
		registry.register("com.projectlibre.exchange.LocalFileImporter", LocalFileImporter::new);
	}

	@Override
	public void registerPorts(PortRegistry registry) {
		registry.registerImport(new FileImporterPortAdapter(LocalSession.LOCAL_PROJECT_IMPORTER, LocalFileImporter::new));
		registry.registerExport(new FileImporterPortAdapter(LocalSession.LOCAL_PROJECT_IMPORTER, LocalFileImporter::new));
		registry.registerImport(new FileImporterPortAdapter(LocalSession.MPO_PROJECT_IMPORTER, MpoFileImporter::new));
		registry.registerExport(new FileImporterPortAdapter(LocalSession.MPO_PROJECT_IMPORTER, MpoFileImporter::new));
		registry.registerImport(new FileImporterPortAdapter(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ServerLocalFileImporter::new));
		registry.registerExport(new FileImporterPortAdapter(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ServerLocalFileImporter::new));
		registry.registerImport(new FileImporterPortAdapter(LocalSession.MICROSOFT_PROJECT_IMPORTER, MicrosoftImporter::new));
		registry.registerExport(new FileImporterPortAdapter(LocalSession.MICROSOFT_PROJECT_IMPORTER, MicrosoftImporter::new));
	}

	/** Registers the legacy job-based session adapters for LocalSession. */
	@Override
	public void register(SessionImporterRegistry registry) {
		registry.register(LocalSession.LOCAL_PROJECT_IMPORTER, LocalFileImporter::new);
		registry.register(LocalSession.MPO_PROJECT_IMPORTER, MpoFileImporter::new);
		registry.register(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ServerLocalFileImporter::new);
		registry.register(LocalSession.MICROSOFT_PROJECT_IMPORTER, MicrosoftImporter::new);
		// Persisted POD options used class names before the typed boundary existed.
		// Keep those aliases here, next to the concrete format implementations.
		registry.register("com.microproject.exchange.LocalFileImporter", LocalFileImporter::new);
		registry.register("com.microproject.exchange.MpoFileImporter", MpoFileImporter::new);
		registry.register("com.microproject.exchange.ServerLocalFileImporter", ServerLocalFileImporter::new);
		registry.register("com.microproject.exchange.MicrosoftImporter", MicrosoftImporter::new);
		registry.register("com.projectlibre1.exchange.LocalFileImporter", LocalFileImporter::new);
		registry.register("com.projectlibre.exchange.LocalFileImporter", LocalFileImporter::new);
	}
}
