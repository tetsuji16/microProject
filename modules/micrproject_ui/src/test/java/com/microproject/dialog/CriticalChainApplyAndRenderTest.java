/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Reproduces the "new file -> add tasks -> apply CCPM -> open the CCPM graph"
 * flow and confirms a non-empty analysis is produced and the graph/buffer
 * panels render it without throwing.  A real desktop run must show the chain
 * network; an empty criticalTaskIds() here would mean the graph renders blank.
 */
class CriticalChainApplyAndRenderTest {

	private static Project buildProjectWithTasksAndResources() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-apply", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("CCPM Apply Project");

		Task a = project.createScriptedTask();
		a.setName("Task A");
		Task b = project.createScriptedTask();
		b.setName("Task B");
		Task c = project.createScriptedTask();
		c.setName("Task C");
		// Serial dependency A -> B -> C so a critical chain exists.
		DependencyService.getInstance().newDependency(a, b, DependencyType.FS, 0L, project);
		DependencyService.getInstance().newDependency(b, c, DependencyType.FS, 0L, project);

		Resource resource = project.getResourcePool().createScriptedResource();
		resource.setName("CCPM Resource");
		Assignment.getInstance(a, resource, 1.0, 0);
		Assignment.getInstance(b, resource, 1.0, 0);
		Assignment.getInstance(c, resource, 1.0, 0);
		return project;
	}

	@Test
	void applyingCcpmProducesANonEmptyCriticalChain() throws Exception {
		Project project = buildProjectWithTasksAndResources();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		settings.setBufferFraction(0.5);

		CriticalChainService.Analysis applied = service.apply(project, null, settings);
		assertNotNull(applied, "apply() must return an analysis for a project with tasks");
		assertFalse(applied.criticalTaskIds().isEmpty(),
				"apply() must identify a non-empty critical chain so the graph is not blank");
		assertFalse(applied.graphEdges().isEmpty(), "the critical chain graph must contain edges");

		// The replayed analysis() must return the same cached, non-empty result.
		CriticalChainService.Analysis replayed = service.analysis(project);
		assertNotNull(replayed);
		assertFalse(replayed.criticalTaskIds().isEmpty());
	}

	@Test
	void graphPanelRendersAppliedAnalysisWithoutException() throws Exception {
		Project project = buildProjectWithTasksAndResources();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		settings.setBufferFraction(0.5);
		CriticalChainService.Analysis applied = service.apply(project, null, settings);

		CriticalChainGraphPanel panel = new CriticalChainGraphPanel(project);
		assertDoesNotThrow(() -> panel.setAnalysis(applied));
		BufferedImage image = new BufferedImage(400, 240, BufferedImage.TYPE_INT_ARGB);
		panel.setSize(400, 240);
		Graphics g = image.getGraphics();
		try {
			panel.paint(g);
		} finally {
			g.dispose();
		}
	}

	@Test
	void bufferChartPanelRendersAppliedAnalysisWithoutException() throws Exception {
		Project project = buildProjectWithTasksAndResources();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		settings.setBufferFraction(0.5);
		CriticalChainService.Analysis applied = service.apply(project, null, settings);

		CriticalChainBufferChartPanel panel = new CriticalChainBufferChartPanel(project);
		assertDoesNotThrow(() -> panel.setAnalysis(applied, true));
		BufferedImage image = new BufferedImage(400, 240, BufferedImage.TYPE_INT_ARGB);
		panel.setSize(400, 240);
		Graphics g = image.getGraphics();
		try {
			panel.paint(g);
		} finally {
			g.dispose();
		}
	}
}
