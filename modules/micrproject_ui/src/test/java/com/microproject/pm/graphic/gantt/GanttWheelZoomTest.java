package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

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
	void undoRedoShortcutsAreLeftToTheDocumentRootPane() {
		Gantt gantt = newGantt();
		try {
			var inputMap = gantt.getInputMap(Gantt.WHEN_IN_FOCUSED_WINDOW);
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)));
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)));
		} finally {
			gantt.cleanUp();
		}
	}

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
			dispatchCtrlWheel(gantt, 10, 1);

			int after = coord.getTimescaleManager().getCurrentScaleIndex();
			assertTrue(after > before, "Ctrl+wheel down should zoom out (increase scale index), was " + before + " -> " + after);
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void ctrlWheelKeepsTheCursorDateAnchored() {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);

			JScrollPane chartPane = new JScrollPane(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			// Zoom out once so a zoom-in (which can anchor without left-edge clamping) is available.
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out");
			dispatchCtrlWheel(gantt, 150, 1);
			assertTrue(coord.canZoomIn(), "after zooming out a zoom-in must be available");

			int cursorX = 150;
			double anchorDate = coord.toTime(cursorX);
			dispatchCtrlWheel(gantt, cursorX, -1);

			int left = chartPane.getViewport().getViewPosition().x;
			int expectedLeft = (int) Math.round(coord.toX(anchorDate)) - cursorX;
			assertTrue(expectedLeft >= 0, "test setup must avoid left-edge clamping, was " + expectedLeft);
			assertEquals(expectedLeft, left, "the date under the cursor must stay at the cursor screen x");
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
	}	private static Gantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("wheel-zoom-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		return new Gantt(project, "Gantt");
	}

	private static void dispatchCtrlWheel(Gantt gantt, int x, int rotation) {
		MouseWheelEvent wheel = new MouseWheelEvent(gantt, MouseEvent.MOUSE_WHEEL,
				System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, x, 10, 0, false,
				MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation);
		gantt.dispatchEvent(wheel);
	}

	private static boolean hasLocalBinding(javax.swing.InputMap inputMap, KeyStroke keyStroke) {
		KeyStroke[] keys = inputMap.keys();
		if (keys == null)
			return false;
		for (KeyStroke key : keys) {
			if (keyStroke.equals(key))
				return true;
		}
		return false;
	}
}
