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
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.task.Project;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.undo.DataFactoryUndoController;

class ScaledScrollPaneTest {
	@Test
	void zoomAndUnrelatedScheduleNotificationsKeepTheDisplayRange() {
		var undoController = new DataFactoryUndoController();
		var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-test", undoController);
		var project = Project.createProject(resourcePool, undoController);
		var coord = new CoordinatesConverter(project);
		coord.extendViewBefore(30);
		coord.extendViewAfter(30);
		long origin = coord.getOrigin();
		long end = coord.getEnd();

		coord.zoomIn();
		coord.zoomOut();
		coord.zoomReset();
		coord.scheduleChanged(new ScheduleEvent(project, ScheduleEvent.SCHEDULE));
		coord.scheduleChanged(new ScheduleEvent(project, null));

		assertEquals(origin, coord.getOrigin());
		assertEquals(end, coord.getEnd());
	}

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
	void creatingTheScrollPaneDoesNotTreatItsInitialScrollbarLayoutAsEdgeNavigation() {
		var undoController = new DataFactoryUndoController();
		var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-test", undoController);
		var project = Project.createProject(resourcePool, undoController);
		var coordinates = new CoordinatesConverter(project);
		long origin = coordinates.getOrigin();
		long end = coordinates.getEnd();

		new ScaledScrollPane(new TestScaledComponent(coordinates), coordinates, null, 10);

		assertEquals(origin, coordinates.getOrigin());
		assertEquals(end, coordinates.getEnd());
	}

	@Test
	void originChangeKeepsTheVisibleLeftDateAnchored() throws Exception {
		SwingUtilities.invokeAndWait(() -> assertOriginChangeKeepsLeftDateAnchored(200));
	}

	@Test
	void originChangeAfterAScaleChangeRestoresTheLeftDateWithTheCurrentScale() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			var undoController = new DataFactoryUndoController();
			var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-scale-test", undoController);
			var project = Project.createProject(resourcePool, undoController);
			var coord = new CoordinatesConverter(project);
			var pane = new ScaledScrollPane(new TestScaledComponent(coord), coord, null, 20);
			pane.setSize(400, 300);
			pane.doLayout();
			JViewport viewport = pane.getViewport();
			viewport.setViewSize(new Dimension(8_000, 300));
			coord.zoomOut();
			viewport.setViewPosition(new Point(300, 0));
			double leftDateBefore = coord.toTime(300);

			coord.extendViewBefore(20);

			double leftDateAfter = coord.toTime(viewport.getViewPosition().x);
			double onePixel = Math.abs(coord.toTime(301) - coord.toTime(300));
			assertTrue(Math.abs(leftDateAfter - leftDateBefore) <= onePixel,
				"the left-edge date must be restored through the current scale (before="
						+ leftDateBefore + ", after=" + leftDateAfter + ")");
		});
	}

	@Test
	void optedInResizePreservesCenterDateAndVerticalPosition() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			var undoController = new DataFactoryUndoController();
			var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-center-test", undoController);
			var project = Project.createProject(resourcePool, undoController);
			var coord = new CoordinatesConverter(project);
			var pane = new ScaledScrollPane(new TestScaledComponent(coord), coord, null, 20);
			JViewport viewport = pane.getViewport();
			viewport.setViewSize(new Dimension(4_000, 300));
			viewport.setExtentSize(new Dimension(500, 200));
			viewport.setViewPosition(new Point(300, 35));
			pane.preserveCenterDateOnWidthResize();
			double centerDateBefore = coord.toTime(viewport.getViewPosition().x + viewport.getExtentSize().width / 2.0d);

			viewport.setExtentSize(new Dimension(236, 200));

			double centerDateAfter = coord.toTime(viewport.getViewPosition().x + viewport.getExtentSize().width / 2.0d);
			double onePixel = Math.abs(coord.toTime(viewport.getViewPosition().x + 1) - coord.toTime(viewport.getViewPosition().x));
			assertTrue(Math.abs(centerDateAfter - centerDateBefore) <= onePixel,
				"an opted-in width resize must preserve the visible center date");
			assertEquals(35, viewport.getViewPosition().y,
				"preserving the horizontal time anchor must retain the vertical viewport position");

			var defaultPane = new ScaledScrollPane(new TestScaledComponent(coord), coord, null, 20);
			JViewport defaultViewport = defaultPane.getViewport();
			defaultViewport.setViewSize(new Dimension(4_000, 300));
			defaultViewport.setExtentSize(new Dimension(500, 200));
			defaultViewport.setViewPosition(new Point(300, 0));
			defaultViewport.setExtentSize(new Dimension(236, 200));
			assertEquals(300, defaultViewport.getViewPosition().x,
				"other time-scaled views must keep the existing default resize behavior");
		});
	}

	/**
	 * Issue #1126: the Gantt chart resizes its view from the same time-scale
	 * notification, after this pane has handled it. The left-edge date must not be
	 * lost to the view width of the previous range.
	 */
	@Test
	void originChangeKeepsTheLeftDateWhenTheViewGrowsAfterThePaneIsNotified() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			var undoController = new DataFactoryUndoController();
			var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-stale-width-test", undoController);
			var project = Project.createProject(resourcePool, undoController);
			var coord = new CoordinatesConverter(project);
			var component = new SelfSizingScaledComponent();
			var pane = new ScaledScrollPane(component, coord, null, 20);
			pane.setSize(400, 300);
			component.matchRange();
			pane.validate();
			JViewport viewport = pane.getViewport();
			double onePixel = Math.abs(coord.toTime(1) - coord.toTime(0));
			assertTrue(viewport.getViewSize().width > viewport.getExtentSize().width, "the chart must be scrollable");

			double leftDateBefore = coord.toTime(viewport.getViewPosition().x);
			for (int edit = 0; edit < 20; edit++) {
				// A later finish date, as produced by editing a duration in the task table.
				coord.ensureRangeIncludes(coord.getEnd() + 86_400_000L, 0);
				pane.validate();
			}
			assertTrue(Math.abs(coord.toTime(viewport.getViewPosition().x) - leftDateBefore) <= onePixel,
				"schedule growth must not pan the chart");

			// Far enough that the position needs more room than the previous view width had.
			coord.extendViewBefore(120);
			pane.validate();
			assertTrue(Math.abs(coord.toTime(viewport.getViewPosition().x) - leftDateBefore) <= onePixel,
				"a large origin change must keep the left-edge date (x=" + viewport.getViewPosition().x
						+ ", view width=" + viewport.getViewSize().width + ")");

			// A later user scroll is not pulled back to the restored date.
			int scrolledX = viewport.getViewPosition().x + 100;
			viewport.setViewPosition(new Point(scrolledX, 0));
			coord.ensureRangeIncludes(coord.getEnd() + 86_400_000L, 0);
			pane.validate();
			assertEquals(scrolledX, viewport.getViewPosition().x);
		});
	}

	/**
	 * Opening a file restores the saved time range into a converter whose views
	 * already exist. They must follow it, and a later origin change must keep the
	 * date the user scrolled to afterwards.
	 */
	@Test
	void workspaceRestoreOfTheTimeRangeUpdatesExistingViews() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			var undoController = new DataFactoryUndoController();
			var resourcePool = ResourcePool.createRourcePool("scaled-scroll-pane-workspace-test", undoController);
			var project = Project.createProject(resourcePool, undoController);
			var coord = new CoordinatesConverter(project);
			var component = new SelfSizingScaledComponent();
			var pane = new ScaledScrollPane(component, coord, null, 20);
			pane.setSize(400, 300);
			component.matchRange();
			pane.validate();
			JViewport viewport = pane.getViewport();
			double onePixel = Math.abs(coord.toTime(1) - coord.toTime(0));

			var saved = (CoordinatesConverter.Workspace) coord.createWorkspace(0);
			saved.setOrigin(coord.getOrigin() - 200L * 86_400_000L);
			saved.setEnd(coord.getEnd() + 200L * 86_400_000L);
			double leftDateBefore = coord.toTime(viewport.getViewPosition().x);
			coord.restoreWorkspace(saved, 0);
			pane.validate();

			assertEquals((int) Math.ceil(coord.getWidth()), viewport.getViewSize().width,
				"the chart must be resized to the restored range");
			assertTrue(Math.abs(coord.toTime(viewport.getViewPosition().x) - leftDateBefore) <= onePixel,
				"restoring the range must not pan the chart");

			int scrolledX = viewport.getViewPosition().x + 150;
			viewport.setViewPosition(new Point(scrolledX, 0));
			double scrolledDate = coord.toTime(scrolledX);
			coord.extendViewBefore(30);
			pane.validate();
			assertTrue(Math.abs(coord.toTime(viewport.getViewPosition().x) - scrolledDate) <= onePixel,
				"a scroll made after the restore must survive the next origin change");
		});
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
		int viewportXBefore = viewport.getViewPosition().x;
		double leftDateBefore = coord.toTime(viewportXBefore);
		long originBefore = coord.getOrigin();

			// This is the same origin change produced when an edit moves the
			// earliest scheduled task, and also exercises the leading-edge path.
		coord.extendViewBefore(10);

		double leftDateAfter = coord.toTime(viewport.getViewPosition().x);
		double onePixel = Math.abs(coord.toTime(viewportXBefore + 1) - coord.toTime(viewportXBefore));
		assertTrue(Math.abs(leftDateAfter - leftDateBefore) <= onePixel,
			"an origin change must preserve the visible left date (before="
					+ leftDateBefore + ", after=" + leftDateAfter + ", x="
					+ viewport.getViewPosition().x + ", viewportXBefore=" + viewportXBefore
					+ ", originBefore=" + originBefore
					+ ", originAfter=" + coord.getOrigin() + ", scaleOffsetBefore="
					+ coord.getTimescaleManager().getScale().toTime(initialX)
					+ ", scaleOffsetAfter=" + coord.getTimescaleManager().getScale()
							.toTime(viewport.getViewPosition().x) + ")");
	}

	/** Sizes its view to the date range from the time-scale notification, like the Gantt chart. */
	private static final class SelfSizingScaledComponent extends JPanel implements ScaledComponent, TimeScaleListener {
		private CoordinatesConverter coordinates;

		@Override
		public void setCoord(CoordinatesConverter coord) {
			coordinates = coord;
			coord.addTimeScaleListener(this);
		}

		@Override
		public CoordinatesConverter getCoord() {
			return coordinates;
		}

		@Override
		public void timeScaleChanged(TimeScaleEvent event) {
			matchRange();
		}

		void matchRange() {
			Dimension size = new Dimension((int) Math.ceil(coordinates.getWidth()), 300);
			setPreferredSize(size);
			if (getParent() instanceof JViewport viewport)
				viewport.setViewSize(size);
			revalidate();
		}
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
