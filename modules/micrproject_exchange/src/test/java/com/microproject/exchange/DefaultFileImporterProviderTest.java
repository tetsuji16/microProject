/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.microproject.session.LocalSession;

class DefaultFileImporterProviderTest {
	@Test
	void registersConcreteExchangeFactoriesAndIsDiscoverableAsAService() {
		ImporterRegistry registry = new ImporterRegistry();
		new DefaultFileImporterProvider().register(registry);

		assertInstanceOf(MpoFileImporter.class, registry.create(LocalSession.MPO_PROJECT_IMPORTER));
		assertInstanceOf(ServerLocalFileImporter.class, registry.create(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER));
		assertInstanceOf(MicrosoftImporter.class, registry.create(LocalSession.MICROSOFT_PROJECT_IMPORTER));
		assertTrue(ServiceLoader.load(FileImporterProvider.class).stream()
			.anyMatch(provider -> provider.type().equals(DefaultFileImporterProvider.class)));
	}
}
