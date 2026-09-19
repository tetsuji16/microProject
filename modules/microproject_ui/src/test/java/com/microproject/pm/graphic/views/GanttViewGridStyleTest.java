/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttParamsImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;

class GanttViewGridStyleTest {
	@Test
	void ganttImplementationsShowGridlinesByDefault() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gantt-grid-default-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		Gantt gantt = new Gantt(project, "Gantt");
		try {
			assertTrue(gantt.isGridLinesVisible());
			assertTrue(new GanttParamsImpl().isGridLinesVisible());
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void workspaceGridlineSettingPreservesExplicitChoiceAndSupportsLegacyWorkspaces() {
		GanttView.Workspace workspace = new GanttView.Workspace();
		assertTrue(GanttView.gridLinesVisibleFromWorkspace(workspace),
			"workspaces saved before this setting must use the shared visible default");

		workspace.setGridLinesVisible(Boolean.FALSE);
		assertFalse(GanttView.gridLinesVisibleFromWorkspace(workspace));
		assertEquals(Boolean.FALSE, workspace.getGridLinesVisible());
	}

	@Test
	void spreadsheetGridStyleTogglesTheTaskTableRowHeaderAndGanttTogether() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gantt-grid-sync-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		Gantt gantt = new Gantt(project, "Gantt");
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			Color gridColor = new Color(0x345678);

			TaskGanttSyncSupport.applySpreadsheetGridStyle(sheet, gantt, true, gridColor);
			assertTrue(sheet.getShowHorizontalLines());
			assertTrue(sheet.getShowVerticalLines());
			assertTrue(sheet.getRowHeader().getShowHorizontalLines());
			assertTrue(gantt.isGridLinesVisible());
			assertEquals(gridColor, sheet.getGridColor());
			assertEquals(gridColor, gantt.getGridLineColor());

			TaskGanttSyncSupport.applySpreadsheetGridStyle(sheet, gantt, false, gridColor);
			assertFalse(sheet.getShowHorizontalLines());
			assertFalse(sheet.getShowVerticalLines());
			assertFalse(sheet.getRowHeader().getShowHorizontalLines());
			assertFalse(gantt.isGridLinesVisible());
		});
		gantt.cleanUp();
	}

	@Test
	void ganttLineTypesHaveIndependentVisibilityAndStyleState() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gantt-line-types-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		Gantt gantt = new Gantt(project, "Gantt");
		try {
			assertTrue(gantt.isProjectStartLineVisible());
			assertTrue(gantt.isStatusDateLineVisible());
			assertTrue(gantt.isCurrentDateLineVisible());
			gantt.setProjectStartLineVisible(false);
			gantt.setStatusDateLineVisible(false);
			gantt.setCurrentDateLineVisible(false);
			gantt.setProjectLineStyle(0);
			gantt.setStatusDateLineStyle(5);
			assertFalse(gantt.isProjectStartLineVisible());
			assertFalse(gantt.isStatusDateLineVisible());
			assertFalse(gantt.isCurrentDateLineVisible());
			assertEquals(0, gantt.getProjectLineStyle());
			assertEquals(5, gantt.getStatusDateLineStyle());
			gantt.setTimescaleMajorLineVisible(true);
			gantt.setTimescaleMinorLineVisible(true);
			gantt.setNonWorkingBoundaryVisible(true);
			gantt.setTimescaleMajorLineStyle(5);
			gantt.setTimescaleMinorLineStyle(7);
			gantt.setNonWorkingBoundaryStyle(6);
			gantt.setTimescaleMajorLineColor(Color.RED);
			gantt.setTimescaleMinorLineColor(Color.BLUE);
			gantt.setNonWorkingBoundaryColor(Color.GREEN);
			assertTrue(gantt.isTimescaleMajorLineVisible());
			assertTrue(gantt.isTimescaleMinorLineVisible());
			assertTrue(gantt.isNonWorkingBoundaryVisible());
			assertEquals(5, gantt.getTimescaleMajorLineStyle());
			assertEquals(7, gantt.getTimescaleMinorLineStyle());
			assertEquals(6, gantt.getNonWorkingBoundaryStyle());
			assertEquals(Color.RED, gantt.getTimescaleMajorLineColor());
			assertEquals(Color.BLUE, gantt.getTimescaleMinorLineColor());
			assertEquals(Color.GREEN, gantt.getNonWorkingBoundaryColor());
		} finally {
			gantt.cleanUp();
		}
	}
}
