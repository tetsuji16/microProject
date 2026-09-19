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
import java.time.Instant;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
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

	@Test
	void bufferConsumptionChartPaintsAndKeepsDistinctObservations() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("buffer-chart-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		CriticalChainService.Buffer buffer = new CriticalChainService.Buffer(100L, 50L, 50L, 0.5D,
			CriticalChainService.BufferStatus.AMBER);
		CriticalChainService.Analysis analysis = new CriticalChainService.Analysis(null, List.of(), 100L, java.util.Map.of(),
			buffer, java.util.Map.of(), java.util.Map.of(), List.of());
		CriticalChainBufferChartPanel panel = new CriticalChainBufferChartPanel(project);
		panel.setSize(620, 420);
		panel.setAnalysis(analysis, true);
		panel.setAnalysis(analysis, true);
		assertEquals(1, CriticalChainBufferChartPanel.observationCount(panel));
		assertEquals(1, project.findTransientDocumentState(CriticalChainBufferHistory.class).points().size());
		assertEquals(50D, CriticalChainBufferChartPanel.currentPoint(project, buffer).consumptionPercent());
		assertEquals(CriticalChainBufferChartPanel.Zone.GREEN, CriticalChainBufferChartPanel.zoneFor(
			CriticalChainBufferChartPanel.currentPoint(project, new CriticalChainService.Buffer(100L, 5L, 95L, 0.05D,
				CriticalChainService.BufferStatus.GREEN))));
		assertEquals(CriticalChainBufferChartPanel.Zone.AMBER, CriticalChainBufferChartPanel.zoneFor(
			CriticalChainBufferChartPanel.currentPoint(project, new CriticalChainService.Buffer(100L, 20L, 80L, 0.2D,
				CriticalChainService.BufferStatus.AMBER))));
		assertEquals(CriticalChainBufferChartPanel.Zone.RED, CriticalChainBufferChartPanel.zoneFor(
			CriticalChainBufferChartPanel.currentPoint(project, new CriticalChainService.Buffer(100L, 60L, 40L, 0.6D,
				CriticalChainService.BufferStatus.RED))));
		BufferedImage image = new BufferedImage(620, 420, BufferedImage.TYPE_INT_ARGB);
		assertDoesNotThrow(() -> {
			Graphics2D graphics = image.createGraphics();
			try { panel.paint(graphics); } finally { graphics.dispose(); }
		});
	}

	@Test
	void bufferChartRendersOnlyAdjacentChronologicalObservations() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("buffer-chart-render-order", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		history.add(point("2026-08-20T09:00:00Z", 75D, 30D));
		history.add(point("2026-07-01T09:00:00Z", 0D, 0D));
		history.add(point("2026-08-01T09:00:00Z", 50D, 55D));
		history.add(point("2026-07-15T09:00:00Z", 25D, 20D));
		// Opening the status surface may sample the current 75/30 value again.
		history.add(point("2026-09-06T09:00:00Z", 75D, 30D));

		List<CriticalChainBufferHistory.Point> rendered = CriticalChainBufferChartPanel.renderPoints(history.points());
		assertEquals(List.of(0D, 25D, 50D, 75D), rendered.stream()
			.map(CriticalChainBufferHistory.Point::progressPercent).toList());
		assertEquals(List.of(0D, 20D, 55D, 30D), rendered.stream()
			.map(CriticalChainBufferHistory.Point::consumptionPercent).toList());
	}

	private static CriticalChainBufferHistory.Point point(String observedAt, double progress, double consumption) {
		return new CriticalChainBufferHistory.Point(Instant.parse(observedAt), "test", "test", progress,
			consumption, "GREEN", "baseline");
	}

	@Test
	void clearingCcpmAlsoDiscardsTheTransientBufferHistory() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("buffer-history-clear", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.restoreBaseline(project, new CriticalChainService.Baseline(project.getEnd(), 100L, 0.5D, List.of(), java.util.Map.of(), java.util.Map.of()));
		CriticalChainService.Buffer buffer = new CriticalChainService.Buffer(100L, 20L, 80L, 0.2D, CriticalChainService.BufferStatus.GREEN);
		CriticalChainBufferChartPanel chart = new CriticalChainBufferChartPanel(project);
		chart.setAnalysis(new CriticalChainService.Analysis(null, List.of(), 100L, java.util.Map.of(), buffer, java.util.Map.of(), java.util.Map.of(), List.of()), true);
		assertEquals(1, CriticalChainBufferChartPanel.observationCount(chart));

		service.clear(project);

		assertEquals(0, CriticalChainBufferChartPanel.observationCount(new CriticalChainBufferChartPanel(project)));
	}

	@Test
	void criticalChainGraphExpandsScrollableCanvasForLongChains() {
		List<Long> ids = java.util.stream.LongStream.rangeClosed(1L, 8L).boxed().toList();
		List<CriticalChainService.ChainEdge> edges = new java.util.ArrayList<>();
		for (long id = 1L; id < 8L; id++) {
			edges.add(new CriticalChainService.ChainEdge(id, id + 1L,
				CriticalChainService.ChainEdge.Kind.DEPENDENCY));
		}
		CriticalChainService.Analysis analysis = new CriticalChainService.Analysis(null, ids, 100L,
			java.util.Map.of(), new CriticalChainService.Buffer(100L, 0L, 100L, 0D,
				CriticalChainService.BufferStatus.GREEN), java.util.Map.of(), java.util.Map.of(), edges);
		CriticalChainGraphPanel panel = new CriticalChainGraphPanel(null);
		panel.setAnalysis(analysis);

		assertEquals(9 * (170 + 70) + 40, panel.getPreferredSize().width);
		assertEquals(420, panel.getPreferredSize().height);
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
