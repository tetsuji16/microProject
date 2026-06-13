package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.spreadsheet.editor.DateEditor;

class CommonSpreadSheetDateSelectionTest {
	private static final class TestableCommonSpreadSheet extends CommonSpreadSheet {
		void setEditorComponent(java.awt.Component component) {
			this.editorComp = component;
		}
	}

	@Test
	void dateEditorSelectionCanBeCollapsedAfterTyping() throws Exception {
		final TestableCommonSpreadSheet[] sheetRef = new TestableCommonSpreadSheet[1];
		final DateEditor.ExtDateField[] fieldRef = new DateEditor.ExtDateField[1];
		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = new TestableCommonSpreadSheet();
			DateEditor.ExtDateField dateField = new DateEditor.ExtDateField(new SimpleDateFormat("yyyy/MM/dd"));
			dateField.getTextField().setText("3");
			dateField.getTextField().selectAll();
			fieldRef[0] = dateField;
			try {
				sheetRef[0].setEditorComponent(dateField);
				Method method = CommonSpreadSheet.class.getDeclaredMethod("stabilizeDateEditorSelection", JTextComponent.class);
				method.setAccessible(true);
				method.invoke(sheetRef[0], dateField.getTextField());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		SwingUtilities.invokeAndWait(() -> {
		});
		assertEquals(1, fieldRef[0].getTextField().getCaretPosition());
		assertNull(fieldRef[0].getTextField().getSelectedText());
	}
}
