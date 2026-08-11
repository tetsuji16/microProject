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
}
