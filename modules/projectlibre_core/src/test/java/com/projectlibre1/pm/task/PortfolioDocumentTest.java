package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PortfolioDocumentTest {
	@Test
	void exposesDefaultCalendarAndSelectionManager() {
		Portfolio portfolio = ProjectFactory.createInstance().portfolio;

		assertNotNull(portfolio.getDefaultCalendar());
		assertSame(portfolio.getObjectSelectionEventManager(), portfolio.getObjectSelectionEventManager());
	}

	@Test
	void marksPortfolioAndChildrenDirtyTogether() {
		Portfolio portfolio = ProjectFactory.createInstance().portfolio;

		portfolio.setAllChildrenDirty(true);

		assertTrue(portfolio.isGroupDirty());
	}
}
