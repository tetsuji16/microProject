/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ChartInfoWorkspaceTest {
	@Test
	void restoresVerticalScrollWithoutDependingOnTheRemovedPopupItem() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("chart-workspace", undo), undo);
		project.initialize(false, false);

		ChartInfo chartInfo = new ChartInfo();
		chartInfo.setMode(ChartMode.RESOURCE_GRAPH);
		chartInfo.setProject(project);
		chartInfo.setCoord(new CoordinatesConverter(project));
		chartInfo.getModel().computeHistogram(project, List.of(), List.of(), chartInfo.getTraces());
		TimeChartPanel chartPanel = new TimeChartPanel(chartInfo);
		chartInfo.setChartPanel(chartPanel);

		ChartInfo.Workspace workspace = new ChartInfo.Workspace();
		workspace.setVerticalScroll(true);
		assertDoesNotThrow(() -> chartInfo.restoreWorkspace(workspace, 0));
		assertTrue(chartPanel.isVerticalScrolling());
		assertTrue(((ChartInfo.Workspace) chartInfo.createWorkspace(0)).isVerticalScroll());
	}
}
