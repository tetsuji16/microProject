package com.microproject.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

class TimeScaleManagerTest {
	@Test
	void canZoomInStopsAtDailyScale() {
		TimeScaleManager manager = managerAtScale(2);

		assertFalse(manager.canZoomIn());
	}

	@Test
	void zoomInDoesNotMovePastDailyScale() {
		TimeScaleManager manager = managerAtScale(2);

		assertFalse(manager.zoomIn());
		assertEquals(2, manager.getCurrentScaleIndex());
	}

	@Test
	void zoomInStillWorksFromCoarserScale() {
		TimeScaleManager manager = managerAtScale(3);

		assertTrue(manager.canZoomIn());
		assertTrue(manager.zoomIn());
		assertEquals(2, manager.getCurrentScaleIndex());
	}

	private static TimeScaleManager managerAtScale(int currentScaleIndex) {
		TimeScaleManager manager = new TimeScaleManager();
		manager.addTimeScale(scale(Calendar.HOUR_OF_DAY, 2));
		manager.addTimeScale(scale(Calendar.HOUR_OF_DAY, 6));
		manager.addTimeScale(scale(Calendar.DAY_OF_WEEK, 1));
		manager.addTimeScale(scale(Calendar.DAY_OF_MONTH, 3));
		manager.setDefaultIndex(2);
		manager.setCurrentScaleIndex(currentScaleIndex);
		return manager;
	}

	private static TimeScale scale(int calendarField1, int number1) {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(calendarField1);
		scale.setNumber1(number1);
		scale.setNormalMinWidth(1);
		return scale;
	}
}
