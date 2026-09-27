/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ApplicationRestartCoordinatorTest {
	@Test
	void restartIsUnavailableUntilAHandlerIsInstalled() {
		assertFalse(new ApplicationRestartCoordinator().restart());
	}

	@Test
	void restartInvokesTheInstalledApplicationActionOnce() {
		ApplicationRestartCoordinator coordinator = new ApplicationRestartCoordinator();
		AtomicInteger restartCount = new AtomicInteger();
		coordinator.setRestartAction(restartCount::incrementAndGet);

		assertTrue(coordinator.restart());
		assertEquals(1, restartCount.get());
	}
}
