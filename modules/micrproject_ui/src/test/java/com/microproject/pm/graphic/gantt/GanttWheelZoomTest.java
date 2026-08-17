package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Regression test for #203: Ctrl+mouse-wheel must zoom the Gantt timescale
 * (matching MS Project) instead of being swallowed by the graph interactor.
 */
class GanttWheelZoomTest {

	@Test
	void ctrlWheelDownZoomsOutTheTimescale() {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out for the test to be meaningful");

			JScrollPane chartPane = new JScrollPane(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			int before = coord.getTimescaleManager().getCurrentScaleIndex();
			MouseWheelEvent wheel = new MouseWheelEvent(gantt, MouseEvent.MOUSE_WHEEL,
					System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, 10, 10, 0, false,
					MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1);
			gantt.dispatchEvent(wheel);

			int after = coord.getTimescaleManager().getCurrentScaleIndex();
			assertTrue(after > before, "Ctrl+wheel down should zoom out (increase scale index), was " + before + " -> " + after);
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void plainWheelDoesNotZoomTheTimescale() {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out for the test to be meaningful");

			JScrollPane chartPane = new JScrollPane(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			int before = coord.getTimescaleManager().getCurrentScaleIndex();
			MouseWheelEvent wheel = new MouseWheelEvent(gantt, MouseEvent.MOUSE_WHEEL,
					System.currentTimeMillis(), 0, 10, 10, 0, false,
					MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1);
			gantt.dispatchEvent(wheel);

			int after = coord.getTimescaleManager().getCurrentScaleIndex();
			assertTrue(after == before, "plain wheel must scroll, not zoom (scale index " + before + " -> " + after + ")");
		} finally {
			gantt.cleanUp();
		}
	}

	private static Gantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("wheel-zoom-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		return new Gantt(project, "Gantt");
	}

}
