/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.model.resource.ResourceImpl;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Geometry and drag coverage for the Team Planner canvas. The resource names are painted by the scroll pane's row
 * header, so the canvas must place its bars from its own left edge and keep the drag day arithmetic unchanged.
 */
class TeamPlannerCanvasTest {

	@Test
	void theTimelineStartsAtTheCanvasLeftEdge() throws Exception {
		Fixture fixture = fixture();
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = canvas(fixture);
		Point bar = findBar(canvas, "Planned work");
		assertNotNull(bar, "the assignment bar must be painted on the canvas");
		int expectedX = (int) Math.round((fixture.task.getStart() - canvas.getTimelineStart())
			/ (double) (24L * 60L * 60L * 1000L) * canvas.getPixelsPerDay());
		assertTrue(Math.abs(bar.x - expectedX) <= 5,
			"the bar must begin at its date-based canvas position without the removed resource-column offset");
	}

	@Test
	void draggingABarMovesItByTheDraggedNumberOfDays() throws Exception {
		Fixture fixture = fixture();
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = canvas(fixture);
		Point bar = findBar(canvas, "Planned work");
		assertNotNull(bar, "the assignment bar must be painted on the canvas");
		long before = fixture.task.getStart();
		int pixels = 2 * canvas.getPixelsPerDay();
		SwingUtilities.invokeAndWait(() -> {
			canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, bar));
			canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_DRAGGED, new Point(bar.x + pixels, bar.y)));
			canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_RELEASED, new Point(bar.x + pixels, bar.y)));
		});
		long expected = fixture.task.getEffectiveWorkCalendar()
			.add(before, 2L * CalendarOption.getInstance().getMillisPerDay(), false);
		assertEquals(expected, fixture.task.getStart(),
			"a two-day drag must move the bar by two working days, as it did before the resource column was split out");
	}

	/** The View tab's Timescale unit fixes the day width and the two date-header bands. */
	@Test
	void choosingATimeUnitChangesTheDayWidthAndTheHeaderBands() throws Exception {
		Fixture fixture = fixture();
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = canvas(fixture);
		assertEquals(TeamPlannerViewSettings.Timescale.WEEKS.getPixelsPerDay(), canvas.getPixelsPerDay(),
			"the planner opens at its default week scale");
		assertArrayEquals(new TeamPlannerViewSettings.Timescale[] {
			TeamPlannerViewSettings.Timescale.MONTHS, TeamPlannerViewSettings.Timescale.WEEKS }, canvas.headerUnits());

		SwingUtilities.invokeAndWait(() -> canvas.setTimescale(TeamPlannerViewSettings.Timescale.HOURS));
		assertEquals(TeamPlannerViewSettings.Timescale.HOURS.getPixelsPerDay(), canvas.getPixelsPerDay(),
			"a finer unit must widen each day, as the toolbar slider used to do");
		assertArrayEquals(new TeamPlannerViewSettings.Timescale[] {
			TeamPlannerViewSettings.Timescale.DAYS, TeamPlannerViewSettings.Timescale.HOURS }, canvas.headerUnits(),
			"the header must show the selected unit below the one coarser than it");
	}

	/** Zoom In and Zoom Out move one unit step and stop at the finest and coarsest units. */
	@Test
	void zoomMovesOneUnitAndStopsAtTheEnds() throws Exception {
		Fixture fixture = fixture();
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = canvas(fixture);
		SwingUtilities.invokeAndWait(() -> {
			canvas.setTimescale(TeamPlannerViewSettings.Timescale.DAYS);
			canvas.zoomIn();
		});
		assertEquals(TeamPlannerViewSettings.Timescale.HOURS, canvas.getTimescale());
		assertFalse(canvas.canZoomIn(), "Zoom In must be disabled at the finest unit");
		SwingUtilities.invokeAndWait(canvas::zoomIn);
		assertEquals(TeamPlannerViewSettings.Timescale.HOURS, canvas.getTimescale(), "Zoom In must stay at the finest unit");

		SwingUtilities.invokeAndWait(() -> {
			canvas.setTimescale(TeamPlannerViewSettings.Timescale.QUARTERS);
			canvas.zoomOut();
		});
		assertEquals(TeamPlannerViewSettings.Timescale.YEARS, canvas.getTimescale());
		assertFalse(canvas.canZoomOut(), "Zoom Out must be disabled at the coarsest unit");
		SwingUtilities.invokeAndWait(canvas::zoomOut);
		assertEquals(TeamPlannerViewSettings.Timescale.YEARS, canvas.getTimescale(), "Zoom Out must stay at the coarsest unit");
	}

	/** The finest supported scale must not cut off dates beyond the old fixed 100,000 px canvas cap. */
	@Test
	void hourlyTimescaleKeepsTheFullLongProjectInTheScrollableExtent() throws Exception {
		Fixture fixture = fixture();
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = canvas(fixture);
		SwingUtilities.invokeAndWait(() -> {
			fixture.task.setDuration(250L * 24L * 60L * 60L * 1000L);
			canvas.reload();
			canvas.setTimescale(TeamPlannerViewSettings.Timescale.HOURS);
		});

		assertTrue(canvas.getPreferredSize().width > 120_000,
			"the horizontal scroll extent must include a task 250 days after the timeline start at 480 px/day; actual width="
				+ canvas.getPreferredSize().width);
	}

	@Test
	void partialPeriodLabelsStartAtTheVisibleHeaderEdge() {
		assertEquals(3, TeamPlannerDialogBox.TeamPlannerCanvas.visibleLabelStartX(-40));
		assertEquals(18, TeamPlannerDialogBox.TeamPlannerCanvas.visibleLabelStartX(15));
	}

	private static TeamPlannerDialogBox.TeamPlannerCanvas canvas(Fixture fixture) throws Exception {
		TeamPlannerDialogBox.TeamPlannerCanvas[] canvas = new TeamPlannerDialogBox.TeamPlannerCanvas[1];
		SwingUtilities.invokeAndWait(() -> {
			canvas[0] = new TeamPlannerDialogBox.TeamPlannerCanvas(
				fixture.project, new TeamPlannerViewSettings(), () -> { });
			canvas[0].setSize(canvas[0].getPreferredSize());
			paint(canvas[0]);
		});
		return canvas[0];
	}

	private static void paint(TeamPlannerDialogBox.TeamPlannerCanvas canvas) {
		BufferedImage image = new BufferedImage(Math.max(1, canvas.getWidth()), Math.max(1, canvas.getHeight()),
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			canvas.printAll(graphics);
		} finally {
			graphics.dispose();
		}
	}

	private static Point findBar(TeamPlannerDialogBox.TeamPlannerCanvas canvas, String expected) throws Exception {
		Point[] result = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < canvas.getHeight() && result[0] == null; y += 5) {
				for (int x = 0; x < canvas.getWidth(); x += 5) {
					String tooltip = canvas.getToolTipText(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
						System.currentTimeMillis(), 0, x, y, 0, false));
					if (tooltip != null && tooltip.contains(expected)) {
						result[0] = new Point(x, y);
						return;
					}
				}
			}
		});
		return result[0];
	}

	private static MouseEvent mouse(TeamPlannerDialogBox.TeamPlannerCanvas canvas, int id, Point point) {
		return new MouseEvent(canvas, id, System.currentTimeMillis(), 0, point.x, point.y, 1, false,
			MouseEvent.BUTTON1);
	}

	private static Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("team-planner-canvas", undo), undo);
		project.initialize(false, false);
		project.setName("team-planner-canvas");
		project.setFileName("C:/test-fixtures/team-planner-canvas.mpo");
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setName("Engineer");
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Planned work");
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, TeamPlannerCanvasTest.class);
		return new Fixture(project, task);
	}

	private record Fixture(Project project, NormalTask task) { }
}
