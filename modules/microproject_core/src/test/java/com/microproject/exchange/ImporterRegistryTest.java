/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.job.Job;

class ImporterRegistryTest {
	@Test
	void createsRegisteredImporterThroughTypedFactory() {
		ImporterRegistry registry = new ImporterRegistry();
		registry.register("test", TestImporter::new);

		assertInstanceOf(TestImporter.class, registry.create("test"));
		assertEquals(1, registry.keys().size());
	}

	@Test
	void rejectsDuplicateAndUnknownKeys() {
		ImporterRegistry registry = new ImporterRegistry();
		registry.register("test", TestImporter::new);

		assertThrows(IllegalStateException.class, () -> registry.register("test", TestImporter::new));
		assertThrows(IllegalArgumentException.class, () -> registry.create("missing"));
	}

	@Test
	void rejectsBlankKeys() {
		ImporterRegistry registry = new ImporterRegistry();

		assertThrows(IllegalArgumentException.class, () -> registry.register(" ", TestImporter::new));
		assertThrows(IllegalArgumentException.class, () -> registry.create(null));
	}

	private static final class TestImporter extends FileImporter {
		@Override
		public Job getImportFileJob() {
			return null;
		}

		@Override
		public Job getExportFileJob() {
			return null;
		}

		@Override
		public void importFile() {
		}

		@Override
		public void exportFile() {
		}

		@Override
		public boolean saveProject(com.microproject.pm.task.Project project, java.io.OutputStream out) {
			return true;
		}

		@Override
		public com.microproject.pm.task.Project loadProject(java.io.InputStream in) {
			return null;
		}
	}
}
