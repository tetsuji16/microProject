/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GraphicManagerContextualRibbonDescriptorTest {
	@Test
	void networkAndCalendarExposeOnlyTheirImplementedContextualFormatSurface() {
		var network = GraphicManager.contextualRibbonDescriptor("Network");
		assertEquals("NetworkFormatRibbonTask", network.tabId());
		assertEquals("NetworkFormat.contextualTitle", network.titleKey());
		assertTrue(network.implemented());

		var calendar = GraphicManager.contextualRibbonDescriptor("CalendarView");
		assertEquals("CalendarFormatRibbonTask", calendar.tabId());
		assertEquals("CalendarFormat.contextualTitle", calendar.titleKey());
		assertTrue(calendar.implemented());

		var report = GraphicManager.contextualRibbonDescriptor("Report");
		assertFalse(report.implemented(), "unsupported report format must not be advertised");
	}

	@Test
	void resourceGraphPaneHasItsOwnContextualTab() {
		var graph = GraphicManager.contextualRibbonDescriptor("Histogram");
		assertTrue(graph.implemented());
		assertEquals("ResourceGraphFormatRibbonTask", graph.tabId());
		assertEquals("ResourceGraphFormat.contextualTitle", graph.titleKey());
	}

	@Test
	void contextualMatrixDoesNotAdvertiseUnsupportedReportSurfaces() {
		var expected = java.util.Map.of(
			"Gantt", "FormatRibbonTask",
			"TrackingGantt", "FormatRibbonTask",
			"Network", "NetworkFormatRibbonTask",
			"CalendarView", "CalendarFormatRibbonTask",
			"Timeline", "TimelineFormatRibbonTask",
			"DelegateTasks", "TeamPlannerFormatRibbonTask",
			"TaskSheet", "TaskSheetFormatRibbonTask",
			"TaskUsageDetail", "TaskUsageFormatRibbonTask",
			"Resources", "ResourceSheetFormatRibbonTask",
			"ResourceUsageDetail", "ResourceUsageFormatRibbonTask");
		expected.forEach((view, tab) -> {
			var descriptor = GraphicManager.contextualRibbonDescriptor(view);
			assertTrue(descriptor.implemented(), view);
			assertEquals(tab, descriptor.tabId(), view);
		});
		for (String unsupported : java.util.List.of("Report", "ResourceSheet", "TaskUsage", "")) {
			assertFalse(GraphicManager.contextualRibbonDescriptor(unsupported).implemented(), unsupported);
		}
	}
}
