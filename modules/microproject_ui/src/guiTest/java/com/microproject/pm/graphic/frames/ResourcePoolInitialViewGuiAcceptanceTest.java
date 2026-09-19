/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.GraphicsEnvironment;
import java.awt.Window;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI regression coverage for issue #461. */
class ResourcePoolInitialViewGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null)
				manager.cleanUp();
			for (Window candidate : Window.getWindows())
				if (candidate == window)
					candidate.dispose();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void masterResourcePoolOpensOnResourceSheetInsteadOfGantt() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for GUI acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("issue-461-resource-pool", undo);
		pool.setLocal(true);
		pool.setMaster(true);
		Project project = Project.createProject(pool, undo);
		project.setResourcePoolProject(true);
		project.initialize(false, false);

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("issue-461-resource-pool", null, null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setVisible(true);
		});

		DocumentFrame frame = manager.getFrameForProject(project);
		ResourceView expectedView[] = new ResourceView[1];
		SwingUtilities.invokeAndWait(() -> expectedView[0] = frame.getResourceView());
		GuiAcceptanceSupport.await(() -> frame.getActiveTopView() == expectedView[0],
				"a master resource pool did not activate the resource sheet");
		assertSame(expectedView[0], frame.getActiveTopView());
		org.junit.jupiter.api.Assertions.assertInstanceOf(ResourceView.class, frame.getActiveTopView());
	}
}
