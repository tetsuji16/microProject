package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;

class PortfolioDocumentTest {
	@Test
	void onlyYesReplacesAnAlreadyOpenProject() {
		assertTrue(Portfolio.shouldReplaceExistingProject(JOptionPane.YES_OPTION));
		assertFalse(Portfolio.shouldReplaceExistingProject(JOptionPane.NO_OPTION));
		assertFalse(Portfolio.shouldReplaceExistingProject(JOptionPane.CANCEL_OPTION));
		assertFalse(Portfolio.shouldReplaceExistingProject(JOptionPane.CLOSED_OPTION));
	}

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
