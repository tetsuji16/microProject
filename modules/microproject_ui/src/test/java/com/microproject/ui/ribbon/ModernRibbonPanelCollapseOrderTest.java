/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.SwingRibbonModel;

class ModernRibbonPanelCollapseOrderTest {
	private static SwingRibbonModel.RibbonBand band(String id, int rank) {
		return new SwingRibbonModel.RibbonBand(id, id, List.of()).withKeepRank(rank);
	}

	@Test
	void unrankedBandsCollapseRightToLeftAndKeepTheFirstBand() {
		assertEquals(List.of(3, 2, 1),
			ModernRibbonPanel.collapseOrder(List.of(band("a", 0), band("b", 0), band("c", 0), band("d", 0))));
	}

	@Test
	void rankedBandOutlastsTheFirstBand() {
		// View tab shape: Task Views, Resource Views, Data (ranked), Zoom, Split View, Window.
		assertEquals(List.of(5, 4, 3, 1, 0, 2), ModernRibbonPanel.collapseOrder(List.of(
			band("task", 0), band("resource", 0), band("data", 1), band("zoom", 0), band("split", 0), band("window", 0))));
	}
}
