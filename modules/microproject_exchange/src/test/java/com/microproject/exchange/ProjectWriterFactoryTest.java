/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
