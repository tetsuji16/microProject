/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GraphInteractorLifecycleTest {
	@Test
	void updatingTheUiReplacesRatherThanAccumulatesTheInteractor() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Gantt gantt = newGantt();
			assertInteractorCounts(gantt, 1, 1);

			for (int i = 0; i < 10; i++) gantt.updateUI();

			assertInteractorCounts(gantt, 1, 1);
			gantt.cleanUp();
			assertInteractorCounts(gantt, 0, 0);
		});
	}

	private static void assertInteractorCounts(Graph graph, long mouse, long motion) {
		assertEquals(mouse, Arrays.stream(graph.getMouseListeners())
				.filter(GraphInteractor.class::isInstance).count());
		assertEquals(motion, Arrays.stream(graph.getMouseMotionListeners())
				.filter(GraphInteractor.class::isInstance).count());
		assertEquals(0, Arrays.stream(graph.getMouseWheelListeners())
				.filter(GraphInteractor.class::isInstance).count());
	}

	private static Gantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("graph-interactor-lifecycle", undoController);
		return new Gantt(Project.createProject(resourcePool, undoController), "Gantt");
	}
}
