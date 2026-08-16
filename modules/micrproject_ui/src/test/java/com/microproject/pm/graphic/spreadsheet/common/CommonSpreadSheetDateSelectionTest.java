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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;

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
