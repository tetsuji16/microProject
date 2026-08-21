/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.Date;

import javax.swing.JPopupMenu;

import org.junit.jupiter.api.Test;

class ProjectLibreDateFieldTest {
	@Test
	void calendarPopupProvidesASevenColumnMonthTable() {
		ProjectLibreDateField field = new ProjectLibreDateField();
		field.setValue(new Date());
		JPopupMenu popup = new JPopupMenu();
		java.awt.Container panel = field.createCalendarPanel(popup);
		java.awt.Container days = (java.awt.Container) panel.getComponent(1);

		assertEquals(7, ((java.awt.GridLayout) days.getLayout()).getColumns());
		assertEquals(49, days.getComponentCount());
		assertTrue(java.util.Arrays.stream(days.getComponents()).anyMatch(Component::isEnabled));
	}

	@Test
	void selectingADayButtonWritesTheDateBackToTheField() {
		ProjectLibreDateField field = new ProjectLibreDateField();
		JPopupMenu popup = new JPopupMenu();
		java.awt.Container panel = field.createCalendarPanel(popup);
		java.awt.Container days = (java.awt.Container) panel.getComponent(1);
		javax.swing.JButton dayOne = null;
		for (Component component : days.getComponents()) {
			if (component instanceof javax.swing.JButton button && "1".equals(button.getText())) {
				dayOne = button;
				break;
			}
		}
		assertTrue(dayOne != null);
		dayOne.doClick();
		assertTrue(field.getDateValue() != null);
	}
}
