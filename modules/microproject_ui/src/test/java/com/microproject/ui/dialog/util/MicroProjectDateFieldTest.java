/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JPopupMenu;
import javax.swing.JButton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MicroProjectDateFieldTest {
	private static final Date GMT_DEC_10 = Date.from(Instant.parse("2026-12-10T00:00:00Z"));
	private static final Date GMT_DEC_15 = Date.from(Instant.parse("2026-12-15T00:00:00Z"));

	@ParameterizedTest
	@ValueSource(strings = { "Asia/Tokyo", "UTC", "America/New_York" })
	void clickingADayEntersThatDayRegardlessOfTheDefaultTimeZone(String zone) {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone(zone));
			MicroProjectDateField field = new MicroProjectDateField();
			field.setValue(GMT_DEC_10);
			JButton day15 = dayButton(field.createCalendarPanel(new JPopupMenu()), "15");
			assertTrue(day15 != null);
			day15.doClick();
			assertEquals(field.getDateFormat().format(GMT_DEC_15), field.getFormattedTextField().getText());
			assertEquals(GMT_DEC_15, field.getDateValue());
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "Asia/Tokyo", "UTC", "America/New_York" })
	void openingTheCalendarHighlightsTheStoredGmtDay(String zone) {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone(zone));
			MicroProjectDateField field = new MicroProjectDateField();
			field.setValue(GMT_DEC_15);
			Container days = (Container) field.createCalendarPanel(new JPopupMenu()).getComponent(1);
			String selected = null;
			for (Component component : days.getComponents()) {
				if (component instanceof JButton button && Boolean.TRUE.equals(button.getClientProperty("dateField.selected"))) {
					selected = button.getText();
				}
			}
			assertEquals("15", selected);
		} finally {
			TimeZone.setDefault(original);
		}
	}

	private static JButton dayButton(Container panel, String text) {
		Container days = (Container) panel.getComponent(1);
		for (Component component : days.getComponents()) {
			if (component instanceof JButton button && text.equals(button.getText())) {
				return button;
			}
		}
		return null;
	}

	@Test
	void calendarPopupProvidesASevenColumnMonthTable() {
		MicroProjectDateField field = new MicroProjectDateField();
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
		MicroProjectDateField field = new MicroProjectDateField();
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

	@Test
	void calendarButtonsUseFlatLafToolbarButtonStyle() {
		MicroProjectDateField field = new MicroProjectDateField();
		java.awt.Container panel = field.createCalendarPanel(new JPopupMenu());
		java.awt.Container header = (java.awt.Container) panel.getComponent(0);
		for (Component component : header.getComponents()) {
			if (component instanceof JButton button) {
				assertEquals("toolBarButton", button.getClientProperty("JButton.buttonType"));
				assertTrue(button.getToolTipText() != null);
				assertTrue(button.getAccessibleContext().getAccessibleName() != null);
			}
		}
	}
}
