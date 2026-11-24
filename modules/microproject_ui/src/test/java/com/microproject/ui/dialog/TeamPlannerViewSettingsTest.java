/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.ui.dialog.TeamPlannerViewSettings.Category;
import com.microproject.ui.dialog.TeamPlannerViewSettings.Timescale;
import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.view.views.DockableProjectToolView;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/** The Team Planner Format settings and their workspace round trip. */
class TeamPlannerViewSettingsTest {
	@Test
	void outlineLevelAndTextLinesAreClamped() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setOutlineLevel(50);
		assertEquals(TeamPlannerViewSettings.MAX_OUTLINE_LEVEL, settings.getOutlineLevel());
		settings.setOutlineLevel(-1);
		assertEquals(0, settings.getOutlineLevel());
		settings.setTextLines(0);
		assertEquals(TeamPlannerViewSettings.MIN_TEXT_LINES, settings.getTextLines());
		settings.setTextLines(40);
		assertEquals(TeamPlannerViewSettings.MAX_TEXT_LINES, settings.getTextLines());
	}

	@Test
	void resettingACategoryClearsOnlyThatCategory() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setFill(Category.AUTO, 0x112233);
		settings.setBorder(Category.AUTO, 0x445566);
		settings.setFill(Category.LATE, 0x778899);
		settings.resetCategory(Category.AUTO);
		assertNull(settings.getFill(Category.AUTO));
		assertNull(settings.getBorder(Category.AUTO));
		assertEquals(0x778899, settings.getFill(Category.LATE));
	}

	@Test
	void everyOptionSurvivesASaveAndRestore() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setOutlineLevel(3);
		settings.setGridlines(!settings.isGridlines());
		settings.setTextLines(2);
		settings.setExpandRows(!settings.isExpandRows());
		settings.setShowUnassigned(!settings.isShowUnassigned());
		settings.setShowUnscheduled(!settings.isShowUnscheduled());
		settings.setPreventOverallocation(!settings.isPreventOverallocation());
		settings.setFill(Category.MANUAL, 0xAA0011);
		settings.setBorder(Category.EXTERNAL, 0x00BB22);
		settings.setTaskFill(5L, 0x123456);
		settings.setTaskBorder(6L, 0x654321);

		TeamPlannerViewSettings restored = new TeamPlannerViewSettings();
		restored.restoreSettings(settings.saveSettings());

		assertEquals(3, restored.getOutlineLevel());
		assertEquals(settings.isGridlines(), restored.isGridlines());
		assertEquals(2, restored.getTextLines());
		assertEquals(settings.isExpandRows(), restored.isExpandRows());
		assertEquals(settings.isShowUnassigned(), restored.isShowUnassigned());
		assertEquals(settings.isShowUnscheduled(), restored.isShowUnscheduled());
		assertEquals(settings.isPreventOverallocation(), restored.isPreventOverallocation());
		assertEquals(0xAA0011, restored.getFill(Category.MANUAL));
		assertEquals(0x00BB22, restored.getBorder(Category.EXTERNAL));
		assertEquals(0x123456, restored.getTaskFill(5L));
		assertEquals(0x654321, restored.getTaskBorder(6L));
	}

	/** The gallery and the options dialog list one definition: the units' own labels, finest first. */
	@Test
	void bothTimescaleChoosersListTheUnitsInScaleOrder() {
		String[] labels = Timescale.labels();
		assertEquals(Timescale.values().length, labels.length, "every unit must have one chooser row");
		for (int index = 0; index < labels.length; index++) {
			assertEquals(Timescale.values()[index].label(), labels[index], "row " + index + " must name its unit");
			assertEquals(Timescale.values()[index], Timescale.at(index), "row " + index + " must map back to its unit");
		}
		assertNull(Timescale.at(-1), "a row before the list names no unit");
		assertNull(Timescale.at(labels.length), "a row after the list names no unit");
	}

	/** The scale chosen with the View tab's Timescale command survives saving and reopening the project. */
	@Test
	void theChosenTimescaleSurvivesASaveAndRestore() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		assertEquals(TeamPlannerViewSettings.DEFAULT_TIMESCALE, settings.getTimescale());
		settings.setTimescale(Timescale.MONTHS);

		TeamPlannerViewSettings restored = new TeamPlannerViewSettings();
		restored.restoreSettings(settings.saveSettings());

		assertEquals(Timescale.MONTHS, restored.getTimescale());
		assertTrue(settings.saveSettings().contains("timescale=MONTHS"));
	}

	@Test
	void anUnknownOrMissingTimescaleFallsBackToTheDefault() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setTimescale(Timescale.YEARS);
		settings.restoreSettings("timescale=NOT_A_UNIT");
		assertEquals(TeamPlannerViewSettings.DEFAULT_TIMESCALE, settings.getTimescale());
		settings.setTimescale(Timescale.YEARS);
		settings.restoreSettings("");
		assertEquals(TeamPlannerViewSettings.DEFAULT_TIMESCALE, settings.getTimescale());
	}

	/** The Team Planner is an embedded tool view, so its workspace round trip carries the chosen scale. */
	@Test
	void theChosenTimescaleIsRestoredByTheSharedWorkspaceView() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("team-planner-settings", undo), undo);
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		AtomicBoolean refreshed = new AtomicBoolean();
		JPanel content = new JPanel();
		content.putClientProperty(ViewSettingsState.PROPERTY, settings);
		content.putClientProperty(ViewSettingsState.REFRESH_PROPERTY, (Runnable) () -> refreshed.set(true));
		DockableProjectToolView view = new DockableProjectToolView(project,
			MenuActionConstants.ACTION_DELEGATE_TASKS, content);

		settings.setTimescale(Timescale.QUARTERS);
		com.microproject.workspace.WorkspaceSetting workspace = view.createWorkspace(0);
		settings.restoreSettings("");
		refreshed.set(false);
		assertEquals(TeamPlannerViewSettings.DEFAULT_TIMESCALE, settings.getTimescale());

		view.restoreWorkspace(workspace, 0);
		assertEquals(Timescale.QUARTERS, settings.getTimescale());
		assertTrue(refreshed.get(), "a restored workspace must re-apply the scale to the planner");
	}
}
