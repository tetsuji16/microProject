/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DockableProjectToolViewTest {
	@Test
	void wrapsToolContentWithoutCreatingASecondWindow() {
		JPanel content = new JPanel(new BorderLayout());
		JLabel label = new JLabel("tool");
		content.add(label, BorderLayout.CENTER);
		var view = new DockableProjectToolView(null, "Tool", content);

		assertEquals("Tool", view.getViewName());
		assertSame(content, view.getComponent(0));
		assertSame(label, ((JPanel) view.getComponent(0)).getComponent(0));
		view.cleanUp();
		assertEquals(0, view.getComponentCount());
	}

	@Test
	void criticalChainPanelPaintsTheAnalysisWithBufferNodes() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("graph-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Engineer");
		NormalTask first = task(project, "First");
		NormalTask second = task(project, "Second");
		AssignmentService.getInstance().newAssignment(first, resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, resource, 1D, 0L, this);
		CriticalChainService.Analysis analysis = new CriticalChainService().preview(project, List.of(resource));

		CriticalChainGraphPanel panel = new CriticalChainGraphPanel(project);
		panel.setSize(1200, 700);
		panel.setAnalysis(analysis);
		BufferedImage image = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
		assertDoesNotThrow(() -> {
			Graphics2D graphics = image.createGraphics();
			try { panel.paint(graphics); } finally { graphics.dispose(); }
		});
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(java.util.concurrent.TimeUnit.DAYS.toMillis(1));
		return task;
	}
}
