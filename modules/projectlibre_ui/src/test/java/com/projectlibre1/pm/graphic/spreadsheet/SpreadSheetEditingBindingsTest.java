package com.projectlibre1.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SpreadSheetEditingBindingsTest {
	@Test
	void ctrlDFillsDownSelectedCells() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			Object action = sheet.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
			assertEquals(SpreadSheet.FILL_DOWN_ACTION, action);
			assertNotNull(sheet.getActionMap().get(action));
		});
	}

	@Test
	void msProjectTaskMoveShortcutsAreInstalledOnSheetAndRowHeader() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
			KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

			assertEquals(SpreadSheet.MOVE_TASK_UP_ACTION, sheet.getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertEquals(SpreadSheet.MOVE_TASK_DOWN_ACTION, sheet.getInputMap(JComponent.WHEN_FOCUSED).get(down));
			assertEquals(SpreadSheet.MOVE_TASK_UP_ACTION, sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertEquals(SpreadSheet.MOVE_TASK_DOWN_ACTION, sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(down));
			assertNotNull(sheet.getActionMap().get(SpreadSheet.MOVE_TASK_UP_ACTION));
			assertNotNull(sheet.getRowHeader().getActionMap().get(SpreadSheet.MOVE_TASK_DOWN_ACTION));
		});
	}
}
