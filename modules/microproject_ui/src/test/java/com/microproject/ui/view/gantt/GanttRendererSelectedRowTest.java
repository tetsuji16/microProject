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
package com.microproject.ui.view.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.ui.util.FlatUiSupport;

/**
 * Regression tests for issue #179: the Gantt chart highlights the complete
 * calendar row of every task selected in the task table.
 */
class GanttRendererSelectedRowTest {
	private static final int ROW_HEIGHT = 24;
	private final List<NodeModelCache> caches = new ArrayList<>();
	private final List<ReferenceNodeModelCache> references = new ArrayList<>();

	@AfterEach
	void closeCaches() {
		for (NodeModelCache cache : caches) {
			cache.close();
		}
		caches.clear();
		for (ReferenceNodeModelCache reference : references) {
			reference.close();
		}
		references.clear();
	}

	@Test
	void highlightedRowsSpanTheFullChartWidth() {
		Gantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(Set.of(keyAt(gantt, 1), keyAt(gantt, 3)));
			BufferedImage image = render(gantt);

			Color highlight = FlatUiSupport.spreadsheetRangeSelectionBackground();
			assertEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT * 1 + ROW_HEIGHT / 2), "selected row 1 should be highlighted");
			assertEquals(highlight.getRGB(), image.getRGB(150, ROW_HEIGHT * 1 + ROW_HEIGHT / 2), "highlight should span full chart width");
			assertEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT * 3 + ROW_HEIGHT / 2), "selected row 3 should be highlighted");
			assertEquals(highlight.getRGB(), image.getRGB(150, ROW_HEIGHT * 3 + ROW_HEIGHT / 2), "highlight should span full chart width");

			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT / 2), "row 0 should not be highlighted");
			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT * 2 + ROW_HEIGHT / 2), "row 2 should not be highlighted");
			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT * 4 + ROW_HEIGHT / 2), "row 4 should not be highlighted");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void clearingHighlightedRowsRemovesTheBand() {
		Gantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(Set.of(keyAt(gantt, 1)));
			gantt.setHighlightedRowKeys(Collections.emptySet());
			BufferedImage image = render(gantt);

			Color highlight = FlatUiSupport.spreadsheetRangeSelectionBackground();
			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT + ROW_HEIGHT / 2), "no row should be highlighted after clearing");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void publicRowLookupKeepsLegacyDivisionOutsideTheVisibleRows() {
		Gantt gantt = newGantt();
		try {
			int totalHeight = gantt.getRowHeights().totalHeight(gantt.getCache().getSize());
			assertEquals(-1, gantt.getRow(-ROW_HEIGHT));
			assertEquals(gantt.getCache().getSize(), gantt.getRow(totalHeight));
			assertEquals(gantt.getCache().getSize() + 1, gantt.getRow(totalHeight + ROW_HEIGHT));
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void taskRowHitTestingAndBarPositionUsePerRowHeights() {
		Gantt gantt = newGantt();
		try {
			gantt.getRowHeights().setRowHeight(1, ROW_HEIGHT * 2);
			GanttUI ui = (GanttUI) gantt.getUI();
			int row = 2;
			int rowMiddle = gantt.getRowHeights().yOf(row) + gantt.getRowHeights().heightOf(row) / 2;

			assertSame(gantt.getCache().getElementAt(row), ui.getTaskRowAt(rowMiddle),
				"hit testing below a taller row must resolve the visible task row");
			assertEquals(null, ui.getTaskRowAt(gantt.getRowHeights().totalHeight(gantt.getCache().getSize())),
				"the area below the final row must remain outside task hit testing");
			assertEquals(-1, ui.getRowAt(gantt.getRowHeights().totalHeight(gantt.getCache().getSize())),
				"row-level gestures must preserve the old out-of-range result");
			assertEquals(gantt.getRowHeights().heightOf(1), ui.getBarY(row) - ui.getBarY(1), 0.0,
				"the next Gantt bar must follow the taller spreadsheet row");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void calendarClickAndDragBelowAnOverriddenRowTargetTheVisibleTasks() {
		Gantt gantt = newGantt();
		try {
			gantt.getRowHeights().setRowHeight(1, ROW_HEIGHT * 2);
			GanttUI ui = new GanttUI(gantt) {
				@Override
				public com.microproject.ui.view.graph.GraphZone getObjectAt(double x, double y) {
					return null;
				}
			};
			((TestGantt) gantt).installUI(ui);
			List<Gantt.BarClick> selections = new ArrayList<>();
			gantt.setBarSelectionListener(selections::add);
			int row2Y = gantt.getRowHeights().yOf(2) + ROW_HEIGHT / 2;
			int row3Y = gantt.getRowHeights().yOf(3) + ROW_HEIGHT / 2;

			ui.getInteractor().mousePressed(mouse(gantt, MouseEvent.MOUSE_PRESSED, row2Y));
			assertSame(gantt.getCache().getElementAt(2), selections.get(0).node(),
				"a calendar click below a taller row must select the visible task");
			ui.getInteractor().mouseDragged(mouse(gantt, MouseEvent.MOUSE_DRAGGED, row3Y));
			assertSame(gantt.getCache().getElementAt(3), selections.get(1).node(),
				"a calendar drag below a taller row must extend to the visible task");
			ui.getInteractor().mouseReleased(mouse(gantt, MouseEvent.MOUSE_RELEASED, row3Y));
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void horizontalGridLineIsPaintedForAnEmptyRow() {
		Gantt gantt = newGantt();
		try {
			GanttRenderer renderer = new GanttRenderer(gantt);
			BufferedImage image = new BufferedImage(200, ROW_HEIGHT * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setClip(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
				renderer.paintHorizontalLine(graphics, 0);
			} finally {
				graphics.dispose();
			}

			assertEquals(gantt.getGridLineColor().getRGB(), image.getRGB(100, ROW_HEIGHT - 1),
				"an empty row must still have its horizontal separator");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void horizontalGridLineFollowsTheBottomOfAnOverriddenRow() {
		Gantt gantt = newGantt();
		try {
			gantt.getRowHeights().setRowHeight(1, ROW_HEIGHT * 2);
			GanttRenderer renderer = new GanttRenderer(gantt);
			BufferedImage image = new BufferedImage(200, ROW_HEIGHT * 5, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setClip(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
				renderer.paintHorizontalLine(graphics, 1);
			} finally {
				graphics.dispose();
			}

			assertEquals(gantt.getGridLineColor().getRGB(), image.getRGB(100, ROW_HEIGHT * 3 - 1),
				"the row separator must follow the overridden row height");
			assertNotEquals(gantt.getGridLineColor().getRGB(), image.getRGB(100, ROW_HEIGHT * 2 - 1),
				"the separator must not stay at the fixed-height position");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void highlightedRowsStateStoresAndNormalizesItsInput() {
		Gantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(null);
			assertTrue(gantt.getHighlightedRows().isEmpty(), "null should clear highlighted rows");

			gantt.setHighlightedRowKeys(Set.of(keyAt(gantt, 5), keyAt(gantt, 7)));
			Set<Integer> rows = gantt.getHighlightedRows();
			assertEquals(2, rows.size());
			assertTrue(rows.contains(5));
			assertTrue(rows.contains(7));
		} finally {
			gantt.cleanUp();
		}
	}

	private TestGantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("selected-row-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		for (int i = 0; i < 8; i++) {
			project.createScriptedTask().setName("Selection row " + i);
		}
		project.recalculate();
		TestGantt gantt = new TestGantt(project);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		references.add(reference);
		var cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "selected-row-test", null);
		caches.add(cache);
		cache.update();
		gantt.setCache(cache);
		gantt.setRowHeight(ROW_HEIGHT);
		return gantt;
	}

	private static final class TestGantt extends Gantt {
		private TestGantt(Project project) {
			super(project, "Gantt");
		}

		private void installUI(GanttUI ui) {
			setUI(ui);
		}
	}

	private static com.microproject.ui.view.model.cache.ProjectionRowKey keyAt(Gantt gantt, int row) {
		return gantt.getCache().getVisibleNodes().getProjectionIndex().keyAt(row);
	}

	private static BufferedImage render(Gantt gantt) {
		BufferedImage image = new BufferedImage(200, ROW_HEIGHT * 5, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setClip(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
			new GanttRenderer(gantt).paintSelectedRows(graphics, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static MouseEvent mouse(Gantt gantt, int id, int y) {
		return new MouseEvent(gantt, id, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
				20, y, 1, false, MouseEvent.BUTTON1);
	}
}
