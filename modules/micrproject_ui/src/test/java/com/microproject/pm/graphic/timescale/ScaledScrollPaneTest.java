/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.JViewport;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ScaledScrollPaneTest {
	@Test
	void timeAxisShowsPreciseHorizontalScrollButtonsOnly() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		CoordinatesConverter coordinates = new CoordinatesConverter(project);
		ScaledScrollPane scrollPane = new ScaledScrollPane(new TestScaledComponent(coordinates), coordinates, null, 10);

		assertEquals(Boolean.TRUE, scrollPane.getHorizontalScrollBar().getClientProperty("JScrollBar.showButtons"));
		assertNull(scrollPane.getVerticalScrollBar().getClientProperty("JScrollBar.showButtons"));
	}

	@Test
	void originChangeKeepsTheVisibleLeftDateAnchored() {
		assertOriginChangeKeepsLeftDateAnchored(200);
	}

	private static void assertOriginChangeKeepsLeftDateAnchored(int initialX) {
		var undoController = new DataFactoryUndoController();
		var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-test", undoController);
		var project = Project.createProject(resourcePool, undoController);
		var coord = new CoordinatesConverter(project);
		var pane = new ScaledScrollPane(new TestScaledComponent(coord), coord, null, 20);
		pane.setSize(400, 300);
		pane.doLayout();
		JViewport viewport = pane.getViewport();
		viewport.setViewSize(new Dimension(4_000, 300));
		viewport.setViewPosition(new Point(initialX, 0));
		double leftDateBefore = coord.toTime(initialX);

			// This is the same origin change produced when an edit moves the
			// earliest scheduled task, and also exercises the leading-edge path.
		coord.extendViewBefore(10);

		double leftDateAfter = coord.toTime(viewport.getViewPosition().x);
		double onePixel = Math.abs(coord.toTime(initialX + 1) - coord.toTime(initialX));
		assertTrue(Math.abs(leftDateAfter - leftDateBefore) <= onePixel,
			"an origin change must preserve the visible left date (before="
					+ leftDateBefore + ", after=" + leftDateAfter + ", x="
					+ viewport.getViewPosition().x + ")");
	}

	private static final class TestScaledComponent extends JPanel implements ScaledComponent {
		private CoordinatesConverter coordinates;

		private TestScaledComponent(CoordinatesConverter coordinates) {
			this.coordinates = coordinates;
		}

		@Override
		public void setCoord(CoordinatesConverter coord) {
			coordinates = coord;
		}

		@Override
		public CoordinatesConverter getCoord() {
			return coordinates;
		}
	}
}
