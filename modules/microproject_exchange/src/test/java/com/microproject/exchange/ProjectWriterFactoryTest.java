/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.exchange.mpxj.ProjectWriterFactory;

class ProjectWriterFactoryTest {
	@Test
	void mppIsNotAdvertisedOrWrittenAsAnotherFormat() {
		assertFalse(ProjectWriterFactory.supportedFileExtensions().contains("MPP"));
		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
			() -> ProjectWriterFactory.forFile("plan.mpp"));
		assertTrue(failure.getMessage().contains("Cannot write files of type: plan.mpp"));
	}

	@Test
	void internalWritersAreRoutedButOnlySupportedDesktopSaveFormatsAreExposed() {
		assertInstanceOf(org.mpxj.mpx.MPXWriter.class, ProjectWriterFactory.forFile("plan.mpx"));
		assertInstanceOf(org.mpxj.planner.PlannerWriter.class, ProjectWriterFactory.forFile("plan.planner"));
		assertInstanceOf(org.mpxj.primavera.PrimaveraPMFileWriter.class, ProjectWriterFactory.forFile("plan.pmxml"));
		assertInstanceOf(org.mpxj.json.JsonWriter.class, ProjectWriterFactory.forFile("plan.json"));
		assertInstanceOf(org.mpxj.sdef.SDEFWriter.class, ProjectWriterFactory.forFile("plan.sdef"));
		assertTrue(ProjectWriterFactory.supportedFileExtensions().containsAll(
			java.util.Set.of("MPX", "PLANNER", "PMXML", "JSON", "SDEF")));
	}
}
