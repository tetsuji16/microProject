/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GanttInteractorDragTest {
	@Test
	void subHourBarDragDoesNotBecomeAConstraintChangingEdit() {
		ScheduleInterval interval = new ScheduleInterval(10L * 60L * 60L * 1000L,
				18L * 60L * 60L * 1000L);

		assertFalse(GanttInteractor.changesIntervalAtHourPrecision(interval,
				10L * 60L * 60L * 1000L + 59L * 60L * 1000L,
				18L * 60L * 60L * 1000L + 59L * 60L * 1000L));
		assertTrue(GanttInteractor.changesIntervalAtHourPrecision(interval,
				11L * 60L * 60L * 1000L,
				19L * 60L * 60L * 1000L));
	}

	@Test
	void verticalDependencyDragIsMeaningfulEvenWhenItsHorizontalPositionDoesNotChange() {
		assertTrue(GanttInteractor.hasMeaningfulDrag(true, 120.0d, 120.0d));
		assertFalse(GanttInteractor.hasMeaningfulDrag(false, 120.0d, 120.0d));
	}

	@Test
	void splitPointActionIsMeaningfulWithoutPointerMovement() {
		assertTrue(GanttInteractor.hasMeaningfulAction(false, true, 120.0d, 120.0d));
		assertFalse(GanttInteractor.hasMeaningfulAction(false, false, 120.0d, 120.0d));
	}

	@Test
	void progressLineHitTargetRetainsAZeroLengthPointHalo() {
		assertTrue(GanttUI.isWithinProgressLineHitTarget(100.0d, 50.0d, 100.0d, 50.0d, 4.0d));
		assertTrue(GanttUI.isWithinProgressLineHitTarget(104.0d, 54.0d, 100.0d, 50.0d, 4.0d));
		assertFalse(GanttUI.isWithinProgressLineHitTarget(105.0d, 50.0d, 100.0d, 50.0d, 4.0d));
	}

	@Test
	void dependencyPropertiesOpenOnlyOnSecondGanttClick() {
		// Microsoft documents opening task-link properties by double-clicking the Gantt link line:
		// https://support.microsoft.com/en-us/project/change-a-task-link
		onEdt(() -> {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			Project project = Project.createProject(
				ResourcePool.createRourcePool("dependency-click-test", undoController), undoController);
			TestGantt gantt = new TestGantt(project);
			try {
				GraphicDependency dependency = new GraphicDependency(null, null, null);
				AtomicInteger dialogs = new AtomicInteger();
				TestGanttUI ui = gantt.installTestUI(dependency, dialogs);
				GanttInteractor interactor = ui.testInteractor;
				interactor.mousePressed(mouse(gantt, MouseEvent.MOUSE_PRESSED, 1));
				interactor.mouseReleased(mouse(gantt, MouseEvent.MOUSE_RELEASED, 1));
				assertEquals(0, dialogs.get());
				assertSame(dependency, interactor.getSelectedObject());

				interactor.mousePressed(mouse(gantt, MouseEvent.MOUSE_PRESSED, 2));
				interactor.mouseReleased(mouse(gantt, MouseEvent.MOUSE_RELEASED, 2));
				assertEquals(1, dialogs.get());
				assertSame(dependency, interactor.getSelectedObject());
			} finally {
				gantt.cleanUp();
			}
		});
	}

	private static MouseEvent mouse(Gantt gantt, int id, int clickCount) {
		return new MouseEvent(gantt, id, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
				20, 20, clickCount, false, MouseEvent.BUTTON1);
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
			throw new AssertionError(interrupted);
		} catch (InvocationTargetException failure) {
			throw new AssertionError(failure.getCause());
		}
	}

	private static final class TestGanttUI extends GanttUI {
		private final GanttInteractor testInteractor;
		private final GraphicDependency dependency;

		private TestGanttUI(Gantt gantt, GraphicDependency dependency, AtomicInteger dialogs) {
			super(gantt);
			this.dependency = dependency;
			testInteractor = new GanttInteractor(this) {
				@Override
				public boolean isReadOnly() {
					return false;
				}

				@Override
				public void showDependencyPropertiesDialog(GraphicDependency selectedDependency) {
					assertSame(dependency, selectedDependency);
					dialogs.incrementAndGet();
				}
			};
			interactor = testInteractor;
		}

		@Override
		public GraphZone getObjectAt(double x, double y) {
			return new GraphZone(dependency);
		}
	}

	private static final class TestGantt extends Gantt {
		private TestGantt(Project project) {
			super(project, "Gantt");
		}

		private TestGanttUI installTestUI(GraphicDependency dependency, AtomicInteger dialogs) {
			TestGanttUI ui = new TestGanttUI(this, dependency, dialogs);
			setUI(ui);
			return ui;
		}
	}
}
