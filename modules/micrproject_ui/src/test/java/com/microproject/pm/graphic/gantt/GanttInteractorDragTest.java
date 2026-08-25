/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.ScheduleInterval;

class GanttInteractorDragTest {
	@Test
	void subHourBarDragDoesNotBecomeAConstraintChangingEdit() {
		ScheduleInterval interval = new ScheduleInterval(10L * 60L * 60L * 1000L,
				18L * 60L * 60L * 1000L);

		assertFalse(GanttInteractor.changesIntervalAtHourPrecision(interval,
				10L * 60L * 60L * 1000L + 59L * 60L * 1000L,
				18L * 60L * 60L * 1000L + 59L * 60L * 1000L));
		assertTrue(GanttInteractor.changesIntervalAtHourPrecision(interval,
				11L * 60L * 60L * 1000L,
				19L * 60L * 60L * 1000L));
	}

	@Test
	void verticalDependencyDragIsMeaningfulEvenWhenItsHorizontalPositionDoesNotChange() {
		assertTrue(GanttInteractor.hasMeaningfulDrag(true, 120.0d, 120.0d));
		assertFalse(GanttInteractor.hasMeaningfulDrag(false, 120.0d, 120.0d));
	}

	@Test
	void dependencyLineClickDoesNotRequirePointerMovement() {
		assertTrue(GanttInteractor.canExecutePointerAction(false, true, 120.0d, 120.0d));
		assertFalse(GanttInteractor.canExecutePointerAction(false, false, 120.0d, 120.0d));
	}

	@Test
	void dependencyLineRequiresDoubleClickToOpenItsProperties() {
		assertFalse(GanttInteractor.opensDependencyProperties(1));
		assertTrue(GanttInteractor.opensDependencyProperties(2));
	}
}
