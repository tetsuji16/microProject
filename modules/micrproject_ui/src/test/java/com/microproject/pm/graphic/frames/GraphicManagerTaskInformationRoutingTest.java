package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GraphicManagerTaskInformationRoutingTest {
	@Test
	void clearedSelectionIsSafeForInformationRouting() {
		assertTrue(GraphicManager.isEmptySelection(null));
		assertTrue(GraphicManager.isEmptySelection(List.of()));
		assertFalse(GraphicManager.isEmptySelection(List.of("selected")));
	}
}
