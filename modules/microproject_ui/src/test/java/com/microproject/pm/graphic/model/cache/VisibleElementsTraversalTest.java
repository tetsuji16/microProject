/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VisibleElementsTraversalTest {
	@Test
	void getRowReturnsFirstMatchingEncounterPosition() {
		VisibleNodes nodes = new VisibleNodes("tasks", ignored -> { });
		nodes.getElements().add("first");
		nodes.getElements().add("repeat");
		nodes.getElements().add("repeat");

		assertEquals(1, nodes.getRow("repeat"));
		assertEquals(-1, nodes.getRow("missing"));
	}
}
