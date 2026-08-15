package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.BarStyles;

class TaskGanttSyncSupportTest {
	@Test
	void calculateRowHeightIncludesAllBaselineRows() {
		SortedSet<Integer> baseLines = new TreeSet<>(List.of(1, 3));

		assertEquals(120, TaskGanttSyncSupport.calculateRowHeight(baseLines, 60, 15));
	}

	@Test
	void annotationFieldHelpersFallBackWhenNoAnnotationExists() {
		BarStyles styles = new BarStyles();
		assertEquals("fallback", TaskGanttSyncSupport.getAnnotationFieldId(styles, "fallback"));
		assertEquals(null, TaskGanttSyncSupport.getAnnotationFieldId(null, null));
	}
}
