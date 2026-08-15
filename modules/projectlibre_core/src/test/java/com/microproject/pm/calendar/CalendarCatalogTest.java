package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalendarCatalogTest {
	@Test
	void exposesStableDefaultCalendarAndSelectionManager() {
		CalendarCatalog catalog = CalendarCatalog.getInstance();

		assertSame(catalog.getDefaultCalendar(), catalog.getDefaultCalendar());
		assertNotNull(catalog.getObjectSelectionEventManager());
	}

	@Test
	void tracksGroupDirtyStateForAllChildren() {
		CalendarCatalog catalog = CalendarCatalog.getInstance();
		catalog.setGroupDirty(false);

		catalog.setAllChildrenDirty(true);

		assertTrue(catalog.isGroupDirty());
	}
}
