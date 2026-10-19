package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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
	void keypadZoomShortcutsAreNotRegisteredOnTheGanttComponent() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			var inputMap = gantt.getInputMap(Gantt.WHEN_IN_FOCUSED_WINDOW);
			var actionMap = gantt.getActionMap();
			Object zoomIn = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, InputEvent.CTRL_DOWN_MASK));
			Object zoomOut = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK));
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, InputEvent.CTRL_DOWN_MASK)),
				"Ctrl+/ must be registered on the document root pane only");
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK)),
				"Ctrl+* must be registered on the document root pane only");
			assertFalse(hasLocalAction(actionMap, "GanttKeypadZoomIn"));
			assertFalse(hasLocalAction(actionMap, "GanttKeypadZoomOut"));
		} finally {
			gantt.cleanUp();
		}
		});
	}

	private static boolean hasLocalAction(javax.swing.ActionMap actionMap, Object key) {
		Object[] keys = actionMap.keys();
		if (keys == null)
			return false;
		for (Object localKey : keys) {
			if (key.equals(localKey))
				return true;
		}
		return false;
	}

	@Test
	void undoRedoShortcutsAreLeftToTheDocumentRootPane() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			var inputMap = gantt.getInputMap(Gantt.WHEN_IN_FOCUSED_WINDOW);
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)));
			assertFalse(hasLocalBinding(inputMap, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)));
		} finally {
			gantt.cleanUp();
		}
		});
	}

	@Test
	void ctrlWheelDownZoomsOutTheTimescale() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out for the test to be meaningful");

			JScrollPane chartPane = chartPaneForTest(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			int before = coord.getTimescaleManager().getCurrentScaleIndex();
			dispatchCtrlWheel(chartPane, gantt, 10, 1);

			int after = coord.getTimescaleManager().getCurrentScaleIndex();
			assertTrue(after > before, "Ctrl+wheel down should zoom out (increase scale index), was " + before + " -> " + after);
		} finally {
			gantt.cleanUp();
		}
		});
	}

	@Test
	void ctrlWheelKeepsTheCursorDateAnchored() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);

			JScrollPane chartPane = chartPaneForTest(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			// Zoom out once so a zoom-in (which can anchor without left-edge clamping) is available.
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out");
			dispatchCtrlWheel(chartPane, gantt, 150, 1);
			assertTrue(coord.canZoomIn(), "after zooming out a zoom-in must be available");

			int cursorX = 150;
			double anchorDate = coord.toTime(cursorX);
			dispatchCtrlWheel(chartPane, gantt, cursorX, -1);

			int left = chartPane.getViewport().getViewPosition().x;
			int expectedLeft = (int) Math.round(coord.toX(anchorDate)) - cursorX;
			assertTrue(expectedLeft >= 0, "test setup must avoid left-edge clamping, was " + expectedLeft);
			// The viewport position is an integer while the date-to-pixel conversion is
			// floating point; a one-pixel difference is expected across JDK/font metrics.
			assertTrue(Math.abs(expectedLeft - left) <= 1,
					"the date under the cursor must stay at the cursor screen x (expected "
							+ expectedLeft + ", actual " + left + ")");
		} finally {
			gantt.cleanUp();
		}
		});
	}

	@Test
	void plainWheelDoesNotZoomTheTimescale() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			CoordinatesConverter coord = new CoordinatesConverter(gantt.getProject());
			gantt.setCoord(coord);
			assertTrue(coord.canZoomOut(), "default scale must allow zooming out for the test to be meaningful");

			JScrollPane chartPane = chartPaneForTest(gantt);
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);

			int before = coord.getTimescaleManager().getCurrentScaleIndex();
			MouseWheelEvent wheel = dispatchWheel(chartPane, gantt, 10, 0, 1);

			int after = coord.getTimescaleManager().getCurrentScaleIndex();
			assertTrue(after == before, "plain wheel must scroll, not zoom (scale index " + before + " -> " + after + ")");
		} finally {
			gantt.cleanUp();
		}
		});
	}

	@Test
	void plainWheelOnSynchronizedGanttAdvancesOnlyOneOwnerStep() {
		onEdt(() -> {
		Gantt gantt = newGantt();
		try {
			JScrollPane chartPane = chartPaneForTest(gantt);
			chartPane.setSize(300, 180);
			chartPane.doLayout();
			gantt.setSize(4000, 1200);
			chartPane.getViewport().setViewSize(new Dimension(4000, 1200));
			JScrollPane otherPane = new JScrollPane(new JPanel());
			Synchronizer synchronizer = new Synchronizer();
			int paneWheelListenersBefore = chartPane.getMouseWheelListeners().length;
			int ganttWheelListenersBefore = gantt.getMouseWheelListeners().length;
			synchronizer.addSynchro(chartPane, otherPane, ScrollPaneSynchronizer.HORIZONTAL);
			assertEquals(paneWheelListenersBefore + 1, chartPane.getMouseWheelListeners().length,
				"a synchronized scroll pane must have exactly one wheel controller");
			assertEquals(ganttWheelListenersBefore, gantt.getMouseWheelListeners().length,
				"the Gantt view must not own a duplicate wheel controller");

			JScrollBar vertical = chartPane.getVerticalScrollBar();
			int before = vertical.getValue();
			int oneOwnerStep = gantt.getRowHeight() * 5;
			MouseWheelEvent wheel = dispatchWheel(chartPane, gantt, 10, 0, 1);

			assertEquals(before + oneOwnerStep, vertical.getValue(),
				"plain Gantt wheel must be consumed by one vertical-scroll owner");
			assertTrue(wheel.isConsumed(), "the owning Gantt wheel route must consume the event");
		} finally {
			gantt.cleanUp();
		}
		});
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

	private static Gantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("wheel-zoom-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		// Keep the chart's time origin independent of wall-clock and host timezone.
		long projectStart = Instant.parse("2025-01-06T08:00:00Z").toEpochMilli();
		project.setStart(projectStart);
		project.setEnd(projectStart + 30L * 24 * 60 * 60 * 1000);
		return new Gantt(project, "Gantt");
	}

	private static JScrollPane chartPaneForTest(Gantt gantt) {
		JScrollPane chartPane = new JScrollPane(gantt);
		// Realize a non-zero viewport so zoom anchoring is tested against the same
		// geometry as the production Gantt view. Without this, a zero-sized
		// viewport makes the result depend on JDK layout timing.
		chartPane.setSize(300, 400);
		chartPane.doLayout();
		gantt.synchronizeViewportSize();
		gantt.setSize(4000, 300);
		chartPane.getViewport().setViewSize(new Dimension(4000, 300));
		return chartPane;
	}

	private static void dispatchCtrlWheel(JScrollPane chartPane, Gantt gantt, int x, int rotation) {
		dispatchWheel(chartPane, gantt, x, InputEvent.CTRL_DOWN_MASK, rotation);
	}

	private static MouseWheelEvent dispatchWheel(JScrollPane chartPane, Gantt gantt, int x, int modifiers, int rotation) {
		java.awt.Point panePoint = SwingUtilities.convertPoint(gantt, x, 10, chartPane);
		MouseWheelEvent wheel = new MouseWheelEvent(chartPane, MouseEvent.MOUSE_WHEEL,
				System.currentTimeMillis(), modifiers, panePoint.x, panePoint.y, 0, false,
				MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation);
		chartPane.dispatchEvent(wheel);
		return wheel;
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
