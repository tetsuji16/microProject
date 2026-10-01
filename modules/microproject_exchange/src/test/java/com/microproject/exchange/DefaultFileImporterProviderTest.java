/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.microproject.port.PortRegistry;
import com.microproject.port.SessionImporterProvider;
import com.microproject.port.SessionImporterRegistry;
import com.microproject.session.LocalSession;

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

	@Test
	void stableFormatDefinitionsDriveBothRegistryContractsAndPodAliases() {
		DefaultFileImporterProvider provider = new DefaultFileImporterProvider();
		SessionImporterRegistry sessionRegistry = new SessionImporterRegistry();
		PortRegistry portRegistry = new PortRegistry();
		provider.register(sessionRegistry);
		provider.registerPorts(portRegistry);

		Set<String> stableKeys = Set.of(LocalSession.LOCAL_PROJECT_IMPORTER, LocalSession.MPO_PROJECT_IMPORTER,
				LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, LocalSession.MICROSOFT_PROJECT_IMPORTER);
		assertEquals(stableKeys, portRegistry.importKeys());
		assertEquals(stableKeys, portRegistry.exportKeys());
		Set<String> registeredSessionKeys = new HashSet<>(sessionRegistry.keys());
		registeredSessionKeys.removeAll(Set.of("com.microproject.exchange.LocalFileImporter",
				"com.microproject.exchange.MpoFileImporter", "com.microproject.exchange.ServerLocalFileImporter",
				"com.microproject.exchange.MicrosoftImporter", "com.projectlibre1.exchange.LocalFileImporter",
				"com.projectlibre.exchange.LocalFileImporter"));
		assertEquals(stableKeys, registeredSessionKeys);
		assertInstanceOf(LocalFileImporter.class,
				sessionRegistry.create("com.projectlibre.exchange.LocalFileImporter"));
		assertInstanceOf(MicrosoftImporter.class,
				sessionRegistry.create("com.microproject.exchange.MicrosoftImporter"));
	}
}
