/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Verifies the "empty file -> CCPM screen" flow does not throw.  An empty
 * project (no tasks, no resources, no applied CCPM plan) must reach the CCPM
 * status surfaces without a NullPointerException and must show the
 * "no applied plan" placeholder rather than crashing the dialog.
 */
class CriticalChainEmptyProjectViewTest {

	private static Project emptyProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-empty", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Empty CCPM Project");
		// Deliberately no tasks, no resources, no baseline, no CCPM settings.
		return project;
	}

	@Test
	void emptyProjectAnalysisReturnsNoAppliedPlan() {
		Project project = emptyProject();
		CriticalChainService service = new CriticalChainService();
		// analysis() must return null for an unconfigured, empty project so the
		// dialog can show the "no applied plan" placeholder instead of crashing.
		assertNull(service.analysis(project), "analysis() must be null for an empty project");
	}

	@Test
	void bufferStatusDialogShowIsSafeOnEmptyProject() throws Exception {
		Project project = emptyProject();
		// In a headless environment show() must return without building the
		// JDialog (consistent with DocumentFrame.doCriticalChainDialog); on a
		// real desktop it opens the placeholder surface.  Either way it must
		// not throw for an empty project.  owner is null because a JFrame
		// cannot be constructed under headless().
		SwingUtilities.invokeAndWait(() -> CriticalChainStatusDialogBox.show(null, project,
				CriticalChainStatusDialogBox.Surface.BUFFER_STATUS));
	}

	@Test
	void networkDialogShowIsSafeOnEmptyProject() throws Exception {
		Project project = emptyProject();
		SwingUtilities.invokeAndWait(() -> CriticalChainStatusDialogBox.show(null, project,
				CriticalChainStatusDialogBox.Surface.NETWORK));
	}

	@Test
	void graphPanelRendersNullAnalysisWithoutException() {
		Project project = emptyProject();
		CriticalChainGraphPanel panel = new CriticalChainGraphPanel(project);
		assertDoesNotThrow(() -> panel.setAnalysis(null));
		BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
		panel.setSize(200, 120);
		Graphics g = image.getGraphics();
		try {
			panel.paint(g);
		} finally {
			g.dispose();
		}
	}

	@Test
	void bufferChartPanelRendersNullAnalysisWithoutException() {
		Project project = emptyProject();
		CriticalChainBufferChartPanel panel = new CriticalChainBufferChartPanel(project);
		assertDoesNotThrow(() -> panel.setAnalysis(null, true));
		BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
		panel.setSize(200, 120);
		Graphics g = image.getGraphics();
		try {
			panel.paint(g);
		} finally {
			g.dispose();
		}
	}
}
