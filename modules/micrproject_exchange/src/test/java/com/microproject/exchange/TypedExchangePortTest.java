/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import com.microproject.port.PortRegistry;
import com.microproject.port.ImportPort;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.session.LocalSession;

class TypedExchangePortTest {
	@Test
	void exchangeProviderPublishesImportAndExportPorts() {
		PortRegistry registry = new PortRegistry();
		new DefaultFileImporterProvider().registerPorts(registry);
		assertTrue(registry.importKeys().contains(LocalSession.LOCAL_PROJECT_IMPORTER));
		assertTrue(registry.importKeys().contains(LocalSession.MPO_PROJECT_IMPORTER));
		assertTrue(registry.exportKeys().contains(LocalSession.MPO_PROJECT_IMPORTER));
		assertInstanceOf(FileImporterPortAdapter.class, registry.importPort(LocalSession.LOCAL_PROJECT_IMPORTER));
	}

	@Test
	void onlyExplicitLegacyPodAliasesAreAdapted() {
		ImporterRegistry registry = new ImporterRegistry();
		new DefaultFileImporterProvider().register(registry);
		FileImporter importer = registry.create("com.projectlibre1.exchange.LocalFileImporter");
		assertInstanceOf(LocalFileImporter.class, importer);
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> registry.create("com.example.UntrustedImporter"));
	}

	@Test
	void commercialConstructionPodLoadsThroughPortWithoutChangingSource() throws Exception {
		Path sample = Path.of("samples", "Commercial construction project plan.pod");
		if (!Files.isRegularFile(sample)) sample = Path.of("..", "..", sample.toString());
		byte[] before = Files.readAllBytes(sample);
		PortRegistry registry = new PortRegistry();
		new DefaultFileImporterProvider().registerPorts(registry);
		ImportPort importer = registry.importPort(LocalSession.LOCAL_PROJECT_IMPORTER);
		var result = importer.importProject(new ImportPort.ImportRequest(sample, ProjectFactory.getInstance()));
		assertTrue(result.succeeded(), () -> result.diagnostics().toString());
		Project loaded = result.value();
		assertTrue(loaded.getTaskList().size() > 100, "sample POD tasks must be imported");
		assertTrue(loaded.getResourcePool().getResourceList().size() > 0,
				"sample POD resources must be imported");
		int dependencies = 0;
		for (Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (value instanceof Task task) dependencies += task.getSuccessorList().size();
		}
		assertTrue(dependencies > 0, "sample POD dependencies must be imported");
		org.junit.jupiter.api.Assertions.assertArrayEquals(before, Files.readAllBytes(sample),
				"POD import must not modify the source file");
	}
}
