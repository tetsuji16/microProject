/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JPanel;

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
