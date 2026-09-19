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
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SpreadSheetColumnPermissionTest {
	@Test
	void backspaceColumnRemovalHonorsFixedColumnSpreadsheets() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			FixedColumnSpreadSheet sheet = new FixedColumnSpreadSheet();
			sheet.setCanModifyColumns(false);

			assertFalse(sheet.removeSelectedColumn(0));
			assertFalse(sheet.fieldArrayAccessed);
			assertTrue(sheet.handleHeaderBackspace());
			assertFalse(sheet.fieldArrayAccessed);
		});
	}

	private static final class FixedColumnSpreadSheet extends SpreadSheet {
		private boolean fieldArrayAccessed;

		@Override
		public ArrayList getFieldArray() {
			fieldArrayAccessed = true;
			throw new AssertionError("fixed-column removal must stop before reading fields");
		}

		@Override
		public int getSelectedColumn() {
			return 0;
		}

		@Override
		public boolean isColumnFullySelected(int column) {
			return true;
		}

		private boolean handleHeaderBackspace() {
			return handleClearCellKey(new KeyEvent(this, KeyEvent.KEY_PRESSED,
					System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE, '\b'));
		}
	}
}
