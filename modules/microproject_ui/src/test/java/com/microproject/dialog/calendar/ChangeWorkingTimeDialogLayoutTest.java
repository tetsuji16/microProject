/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import com.microproject.contrib.calendar.ContribIntervals;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.strings.Messages;
import com.microproject.testsupport.HeadlessDialogPanels;
import com.microproject.testsupport.HeadlessLayoutAssertions;

class ChangeWorkingTimeDialogLayoutTest {
	@Test
	void bodyKeepsLocalizedTextControlsAtPreferredHeight() throws Exception {
		Locale original = Locale.getDefault();
		Object registry = HeadlessDialogPanels.getStatic(GraphicManager.class, "MANAGER_REGISTRY");
		Object originalManager = HeadlessDialogPanels.get(registry, "activeManager");
		GraphicManager manager = HeadlessDialogPanels.allocate(GraphicManager.class);
		HeadlessDialogPanels.set(registry, "activeManager", manager);
		try {
			for (Locale locale : new Locale[] { Locale.ENGLISH, Locale.JAPANESE }) {
				Locale.setDefault(locale);
				Messages.reset();
				WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
				ChangeWorkingTimeDialogBox dialog = HeadlessDialogPanels.allocate(ChangeWorkingTimeDialogBox.class);
				ChangeWorkingTimeDialogBox.Form form = new ChangeWorkingTimeDialogBox.Form();
				form.setCalendar(calendar);
				HeadlessDialogPanels.set(dialog, "form", form);
				HeadlessDialogPanels.set(dialog, "projectCalendars", new ArrayList<>(java.util.List.of(calendar)));
				HeadlessDialogPanels.set(dialog, "documentCalendars", new ArrayList<>());
				HeadlessDialogPanels.set(dialog, "defaultWorkingHours", WorkingHours.getDefault());
				HeadlessDialogPanels.set(dialog, "lastSelection", new ContribIntervals());
				HeadlessDialogPanels.set(dialog, "lastWeekSelection", new boolean[7]);
				HeadlessDialogPanels.set(dialog, "stagedCalendars", new IdentityHashMap<>());
				HeadlessDialogPanels.set(dialog, "newCalendar", new JButton());
				JComponent panel = dialog.createContentPanel();
				panel.setSize(panel.getPreferredSize());
				assertDoesNotThrow(() -> HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(panel,
					"Change Working Time dialog locale=" + locale));
			}
		} finally {
			HeadlessDialogPanels.set(registry, "activeManager", originalManager);
			Locale.setDefault(original);
			Messages.reset();
		}
	}
}
