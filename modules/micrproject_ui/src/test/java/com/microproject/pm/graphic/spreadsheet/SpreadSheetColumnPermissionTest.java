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
