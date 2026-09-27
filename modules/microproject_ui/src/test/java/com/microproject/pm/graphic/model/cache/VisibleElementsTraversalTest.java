/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import org.junit.jupiter.api.Test;

class VisibleElementsTraversalTest {
	@Test
	void getRowReturnsFirstMatchingEncounterPosition() {
		VisibleNodes nodes = new VisibleNodes("tasks", ignored -> { });
		GraphicNode first = new GraphicNode(null, 0);
		GraphicNode repeated = new GraphicNode(null, 0);
		nodes.getElements().add(first);
		nodes.getElements().add(repeated);
		nodes.getElements().add(repeated);

		assertEquals(1, nodes.getRow(repeated));
		assertEquals(-1, nodes.getRow(new GraphicNode(null, 0)));
	}
}
