package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GraphInteractorLifecycleTest {

	@Test
	void replacesAndCleansUpTheGanttInteractor() {
		onEdt(() -> {
			Gantt gantt = newGantt();
			try {
				assertCurrentInteractorIsTheOnlyOwnedListener(gantt);

				for (int i = 0; i < 3; i++) {
					GanttInteractor previous = gantt.getInteractor();
					gantt.updateUI();
					assertFalse(hasListener(gantt.getMouseListeners(), previous));
					assertFalse(hasListener(gantt.getMouseMotionListeners(), previous));
					assertCurrentInteractorIsTheOnlyOwnedListener(gantt);
				}

				GanttInteractor current = gantt.getInteractor();
				gantt.cleanUp();
				gantt.cleanUp();
				assertFalse(hasListener(gantt.getMouseListeners(), current));
				assertFalse(hasListener(gantt.getMouseMotionListeners(), current));
				assertEquals(0, countOwnedInteractors(gantt.getMouseListeners()));
				assertEquals(0, countOwnedInteractors(gantt.getMouseMotionListeners()));
			} finally {
				gantt.cleanUp();
			}
		});
	}

	private static void assertCurrentInteractorIsTheOnlyOwnedListener(Gantt gantt) {
		GanttInteractor current = gantt.getInteractor();
		assertEquals(1, countOwnedInteractors(gantt.getMouseListeners()));
		assertEquals(1, countOwnedInteractors(gantt.getMouseMotionListeners()));
		assertTrue(hasListener(gantt.getMouseListeners(), current));
		assertTrue(hasListener(gantt.getMouseMotionListeners(), current));
	}

	private static int countOwnedInteractors(Object[] listeners) {
		return (int) Arrays.stream(listeners).filter(GanttInteractor.class::isInstance).count();
	}

	private static boolean hasListener(Object[] listeners, Object target) {
		return Arrays.stream(listeners).anyMatch(listener -> listener == target);
	}

	private static Gantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("interactor-lifecycle-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		long projectStart = Instant.parse("2025-01-06T08:00:00Z").toEpochMilli();
		project.setStart(projectStart);
		project.setEnd(projectStart + 30L * 24 * 60 * 60 * 1000);
		return new Gantt(project, "Gantt");
	}

	private static void onEdt(Runnable action) {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(action);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while running the Swing test on the EDT", interrupted);
		} catch (InvocationTargetException failure) {
			Throwable cause = failure.getCause();
			if (cause instanceof Error error)
				throw error;
			if (cause instanceof RuntimeException runtimeFailure)
				throw runtimeFailure;
			throw new AssertionError("Swing test failed on the EDT", cause);
		}
	}
}
