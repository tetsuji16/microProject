/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.view.views.DockableProjectToolView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/** The pure decisions behind the Calendar Format tab and their workspace round trip. */
class CalendarViewSettingsTest {
	@Test
	void bestFitHeightGrowsWithTheBusiestDayAndNeverDropsBelowTheMinimum() {
		assertEquals(CalendarViewSettings.MIN_ROW_HEIGHT, CalendarViewSettings.bestFitRowHeight(0, false));
		int three = CalendarViewSettings.bestFitRowHeight(3, false);
		int six = CalendarViewSettings.bestFitRowHeight(6, false);
		assertEquals(3 * CalendarViewSettings.BAR_PITCH, six - three);
		assertTrue(CalendarViewSettings.bestFitRowHeight(3, true) > three, "the companion day line needs room");
	}

	@Test
	void bestFitRowShowsEveryTaskOfTheBusiestDay() {
		for (int tasks = 1; tasks <= 12; tasks++)
			for (boolean companion : new boolean[] { false, true })
				assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.bestFitRowHeight(tasks, companion), companion) >= tasks,
					tasks + " tasks, companion " + companion);
		assertEquals(3, CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false));
		assertEquals(2, CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, true));
	}

	@Test
	void barGeometryGrowsWithTheTextSize() {
		int smallHeight = CalendarViewSettings.barHeight(12);
		assertEquals(CalendarViewSettings.BAR_PITCH - 4, smallHeight, "ordinary text keeps the default 19px bar");
		int bigFont = 36 + 9;
		assertTrue(CalendarViewSettings.barHeight(bigFont) >= bigFont + 5, "the bar holds the big text");
		int pitch = CalendarViewSettings.barPitch(bigFont);
		assertTrue(pitch > CalendarViewSettings.BAR_PITCH);
		assertTrue(CalendarViewSettings.bestFitRowHeight(3, false, pitch) > CalendarViewSettings.bestFitRowHeight(3, false));
		for (int tasks = 1; tasks <= 8; tasks++)
			assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.bestFitRowHeight(tasks, true, pitch), true, pitch) >= tasks);
		assertTrue(CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false, pitch)
			< CalendarViewSettings.visibleTaskCount(CalendarViewSettings.DEFAULT_ROW_HEIGHT, false));
	}

	@Test
	void aRowTooShortForOneBarShowsNoBar() {
		int pitch = CalendarViewSettings.barPitch(45);
		assertEquals(0, CalendarViewSettings.visibleTaskCount(60, false, pitch), "a 60px row cannot hold a bar for 45px text");
		assertEquals(0, CalendarViewSettings.visibleTaskCount(60, true, pitch));
	}

	@Test
	void everyOptionSurvivesASaveAndRestore() {
		CalendarViewSettings settings = new CalendarViewSettings();
		settings.setRowHeight(180);
		settings.setBarColor(CalendarViewSettings.BarType.CRITICAL, 0xC0392B);
		settings.setBarColor(CalendarViewSettings.BarType.MILESTONE, 0x123456);
		settings.setTextStyle(CalendarViewSettings.TextItem.BAR_TEXT, new CalendarViewSettings.TextStyle(14, true));
		settings.setTextStyle(CalendarViewSettings.TextItem.DAY_NUMBERS, new CalendarViewSettings.TextStyle(9, false));
		settings.setFitAsManyTasksAsPossible(true);
		settings.setAutomaticLayout(false);
		settings.setTaskListVisible(true);

		CalendarViewSettings restored = new CalendarViewSettings();
		restored.restoreSettings(settings.saveSettings());

		assertEquals(180, restored.getRowHeight());
		assertEquals(0xC0392B, restored.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		assertEquals(0x123456, restored.getBarColor(CalendarViewSettings.BarType.MILESTONE));
		assertEquals(null, restored.getBarColor(CalendarViewSettings.BarType.NORMAL));
		assertEquals(new CalendarViewSettings.TextStyle(14, true),
			restored.getTextStyle(CalendarViewSettings.TextItem.BAR_TEXT));
		assertEquals(new CalendarViewSettings.TextStyle(9, false),
			restored.getTextStyle(CalendarViewSettings.TextItem.DAY_NUMBERS));
		assertTrue(restored.isFitAsManyTasksAsPossible());
		assertFalse(restored.isAutomaticLayout());
		assertTrue(restored.isTaskListVisible());
	}

	@Test
	void embeddedStateIsSavedAndRestoredByTheSharedWorkspaceView() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("calendar-settings", undo), undo);
		CalendarViewSettings settings = new CalendarViewSettings();
		AtomicBoolean refreshed = new AtomicBoolean();
		JPanel content = new JPanel();
		content.putClientProperty(ViewSettingsState.PROPERTY, settings);
		content.putClientProperty(ViewSettingsState.REFRESH_PROPERTY, (Runnable) () -> refreshed.set(true));
		DockableProjectToolView view = new DockableProjectToolView(project,
			MenuActionConstants.ACTION_CALENDAR_VIEW, content);

		settings.setRowHeight(190);
		settings.setTaskListVisible(true);
		com.microproject.workspace.WorkspaceSetting workspace = view.createWorkspace(0);
		settings.restoreSettings("");
		refreshed.set(false);
		assertFalse(settings.isTaskListVisible());

		view.restoreWorkspace(workspace, 0);
		assertEquals(190, settings.getRowHeight());
		assertTrue(settings.isTaskListVisible());
		assertTrue(refreshed.get(), "a restored workspace must re-apply the settings to the calendar");
	}

	@Test
	void defaultAndMalformedStatesFallBackToTheDefaults() {
		CalendarViewSettings settings = new CalendarViewSettings();
		settings.setRowHeight(200);
		settings.setTaskListVisible(true);
		settings.restoreSettings("");
		assertEquals(CalendarViewSettings.DEFAULT_ROW_HEIGHT, settings.getRowHeight());
		assertFalse(settings.isTaskListVisible());
		assertTrue(settings.isAutomaticLayout());

		settings.restoreSettings("rowHeight=broken\nbar.CRITICAL=nope\ntext.BAR_TEXT=bad,true");
		assertEquals(CalendarViewSettings.DEFAULT_ROW_HEIGHT, settings.getRowHeight());
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		assertEquals(CalendarViewSettings.TextStyle.DEFAULT,
			settings.getTextStyle(CalendarViewSettings.TextItem.BAR_TEXT));

		settings.restoreSettings("rowHeight=-5\nbar.NORMAL=16777216\ntext.DAY_NUMBERS=99,true");
		assertEquals(CalendarViewSettings.MIN_ROW_HEIGHT, settings.getRowHeight());
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.NORMAL));
		assertEquals(CalendarViewSettings.MAX_TEXT_SIZE,
			settings.getTextStyle(CalendarViewSettings.TextItem.DAY_NUMBERS).size());
	}

	@Test
	void settingsKeepOnlyChangedValues() {
		CalendarViewSettings settings = new CalendarViewSettings();
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setBarColor(CalendarViewSettings.BarType.CRITICAL, 0xC0392B);
		assertEquals(0xC0392B, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setBarColor(CalendarViewSettings.BarType.CRITICAL, null);
		assertEquals(null, settings.getBarColor(CalendarViewSettings.BarType.CRITICAL));
		settings.setTextStyle(CalendarViewSettings.TextItem.BAR_TEXT, new CalendarViewSettings.TextStyle(14, true));
		assertEquals(14, settings.getTextStyle(CalendarViewSettings.TextItem.BAR_TEXT).size());
		assertEquals(CalendarViewSettings.TextStyle.DEFAULT, settings.getTextStyle(CalendarViewSettings.TextItem.DAY_NUMBERS));
		settings.setRowHeight(10);
		assertEquals(CalendarViewSettings.MIN_ROW_HEIGHT, settings.getRowHeight());
		assertTrue(settings.isAutomaticLayout());
	}
}
