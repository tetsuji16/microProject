package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class CommonSpreadSheetBackspaceKeyTest {
	@Test
	void backspaceIsRecognizedAsCellClearKey() throws Exception {
		final CommonSpreadSheet[] sheetRef = new CommonSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new CommonSpreadSheet());
		CommonSpreadSheet sheet = sheetRef[0];

		Method method = CommonSpreadSheet.class.getDeclaredMethod("isClearCellKey", KeyEvent.class);
		method.setAccessible(true);

		KeyEvent backspacePressed = new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED);
		KeyEvent backspaceTyped = new KeyEvent(sheet, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, '\b');
		KeyEvent deletePressed = new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED);

		assertTrue((Boolean) method.invoke(sheet, backspacePressed));
		assertTrue((Boolean) method.invoke(sheet, backspaceTyped));
		assertFalse((Boolean) method.invoke(sheet, deletePressed));
	}
}
