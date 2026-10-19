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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;

class SpreadSheetCornerLifecycleTest {
	@Test
	void cleanupRemovesCornerFromBothSelectionModels() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			TestSpreadSheet sheet = new TestSpreadSheet();
			CountingCorner corner = new CountingCorner(sheet);
			sheet.installCorner(corner);

			var rowSelection = sheet.getRowHeader().getSelectionModel();
			var columnSelection = sheet.getColumnModel().getSelectionModel();
			rowSelection.setSelectionInterval(0, 0);
			columnSelection.setSelectionInterval(0, 0);
			assertEquals(2, corner.getChangeCount());

			sheet.cleanUp();
			rowSelection.clearSelection();
			columnSelection.clearSelection();

			assertEquals(2, corner.getChangeCount());
			assertTrue(java.util.Arrays.stream(((DefaultListSelectionModel) rowSelection).getListSelectionListeners())
				.noneMatch(listener -> listener == corner));
			assertTrue(java.util.Arrays.stream(((DefaultListSelectionModel) columnSelection).getListSelectionListeners())
				.noneMatch(listener -> listener == corner));
		});
	}

	private static final class TestSpreadSheet extends SpreadSheet {
		private void installCorner(SpreadSheetCorner corner) {
			this.corner = corner;
		}
	}

	private static final class CountingCorner extends SpreadSheetCorner {
		private int changeCount;

		private CountingCorner(CommonSpreadSheet sheet) {
			super(sheet);
		}

		@Override
		public void valueChanged(ListSelectionEvent event) {
			changeCount++;
			super.valueChanged(event);
		}

		private int getChangeCount() {
			return changeCount;
		}
	}
}
