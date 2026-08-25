/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;

/**
 * Regression tests for issue #179: the Gantt chart highlights the complete
 * calendar row of every task selected in the task table.
 */
class GanttRendererSelectedRowTest {
	private static final int ROW_HEIGHT = 24;

	@Test
	void highlightedRowsSpanTheFullChartWidth() {
		TestProjectionGantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(Set.of(gantt.keyAt(1), gantt.keyAt(3)));
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
		TestProjectionGantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(Set.of(gantt.keyAt(1)));
			gantt.setHighlightedRowKeys(Collections.emptySet());
			BufferedImage image = render(gantt);

			Color highlight = FlatUiSupport.spreadsheetRangeSelectionBackground();
			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT + ROW_HEIGHT / 2), "no row should be highlighted after clearing");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void highlightedRowsStateStoresAndNormalizesItsInput() {
		TestProjectionGantt gantt = newGantt();
		try {
			gantt.setHighlightedRowKeys(null);
			assertTrue(gantt.getHighlightedRowKeys().isEmpty(), "null should clear highlighted rows");

			ProjectionRowKey key5 = gantt.keyAt(5);
			ProjectionRowKey key7 = gantt.keyAt(7);
			gantt.setHighlightedRowKeys(Set.of(key5, key7));
			Set<ProjectionRowKey> keys = gantt.getHighlightedRowKeys();
			assertEquals(2, keys.size());
			assertTrue(keys.contains(key5));
			assertTrue(keys.contains(key7));
		} finally {
			gantt.cleanUp();
		}
	}

	private static TestProjectionGantt newGantt() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("selected-row-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		TestProjectionGantt gantt = new TestProjectionGantt(project);
		gantt.setRowHeight(ROW_HEIGHT);
		return gantt;
	}

	@Test
	void identitySelectionFollowsProjectionReorder() {
		TestProjectionGantt gantt = newGantt();
		try {
			ProjectionRowKey selected = gantt.keyAt(1);
			gantt.setHighlightedRowKeys(Set.of(selected));
			gantt.move(selected, 3);

			BufferedImage image = render(gantt);
			Color highlight = FlatUiSupport.spreadsheetRangeSelectionBackground();
			assertNotEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT + ROW_HEIGHT / 2));
			assertEquals(highlight.getRGB(), image.getRGB(5, ROW_HEIGHT * 3 + ROW_HEIGHT / 2));
		} finally {
			gantt.cleanUp();
		}
	}

	private static final class TestProjectionGantt extends Gantt {
		private static final long serialVersionUID = 1L;
		private final Map<ProjectionRowKey, Integer> rows = new HashMap<>();

		private TestProjectionGantt(Project project) {
			super(project, "Gantt");
		}

		private ProjectionRowKey keyAt(int row) {
			ProjectionRowKey key = new ProjectionRowKey(ProjectionRowKey.Kind.TASK, null, 0L, row + 1L);
			rows.put(key, Integer.valueOf(row));
			return key;
		}

		private void move(ProjectionRowKey key, int row) {
			rows.put(key, Integer.valueOf(row));
		}

		@Override
		public int getProjectionRow(ProjectionRowKey key) {
			return rows.getOrDefault(key, Integer.valueOf(-1)).intValue();
		}
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
}
