/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.microproject.session.LocalSession;
import com.microproject.port.SessionImporterProvider;
import com.microproject.port.SessionImporterRegistry;

class DefaultFileImporterProviderTest {
	@Test
	void registersConcreteExchangeFactoriesAndIsDiscoverableAsAService() {
		SessionImporterRegistry registry = new SessionImporterRegistry();
		new DefaultFileImporterProvider().register(registry);

		assertInstanceOf(MpoFileImporter.class, registry.create(LocalSession.MPO_PROJECT_IMPORTER));
		assertInstanceOf(ServerLocalFileImporter.class, registry.create(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER));
		assertInstanceOf(MicrosoftImporter.class, registry.create(LocalSession.MICROSOFT_PROJECT_IMPORTER));
		assertInstanceOf(LocalFileImporter.class, registry.create(LocalSession.LOCAL_PROJECT_IMPORTER));
		assertNotSame(registry.create(LocalSession.MPO_PROJECT_IMPORTER),
				registry.create(LocalSession.MPO_PROJECT_IMPORTER));
		assertTrue(ServiceLoader.load(SessionImporterProvider.class).stream()
			.anyMatch(provider -> provider.type().equals(DefaultFileImporterProvider.class)));
	}

	@Test
	void sessionRegistryRejectsDuplicateBlankAndUnknownKeys() {
		SessionImporterRegistry registry = new SessionImporterRegistry();
		registry.register("local", LocalFileImporter::new);
		assertThrows(IllegalStateException.class, () -> registry.register("local", LocalFileImporter::new));
		assertThrows(IllegalArgumentException.class, () -> registry.register(" ", LocalFileImporter::new));
		assertThrows(IllegalArgumentException.class, () -> registry.create("missing"));
	}
}
