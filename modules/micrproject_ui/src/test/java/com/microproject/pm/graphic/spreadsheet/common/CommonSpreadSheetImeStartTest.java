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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.lang.reflect.Field;
import java.text.AttributedString;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

class CommonSpreadSheetImeStartTest {
	@Test
	void inputMethodStartDoesNotClearExistingCellText() throws Exception {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				var sheet = new RecordingSpreadSheet();

				var event = new InputMethodEvent(sheet, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString("テ").getIterator(), 0, null, null);
				sheet.processInputMethodEvent(event);

				assertNotNull(sheet.recordedEditEvent);
				assertFalse(readBooleanField(sheet.recordedEditEvent, "clearTextOnStart"));
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		if (failure.get() != null) {
			throw new RuntimeException(failure.get());
		}
	}

	@Test
	void receivedTextIsAppendedToTheActiveEditor() throws Exception {
		RecordingSpreadSheet sheet = newRecordingSpreadSheet();
		onEdt(() -> {
			sheet.primeEditor("既存");
			sheet.processKeyEvent(typedKey(sheet, 'テ'));
		});
		flushEdt();
		typeIntoSheet(sheet, 'ス');

		assertEquals("既存テス", sheet.editorText());
	}

	@Test
	void receivedTextIsNotLostWhenTheNextCharacterArrivesBeforeTheEditorIsReady() throws Exception {
		RecordingSpreadSheet sheet = newRecordingSpreadSheet();

		onEdt(() -> {
			sheet.deferEditorAttachment();
			sheet.processKeyEvent(typedKey(sheet, 'テ'));
		});
		SwingUtilities.invokeAndWait(() -> {});
		onEdt(sheet::attachDeferredEditor);
		typeIntoSheet(sheet, 'ス');

		assertEquals("テス", sheet.editorText());
	}

	@Test
	void everyJapaneseCharacterIsRetainedWhileTheEditorIsBeingAttached() throws Exception {
		RecordingSpreadSheet sheet = newRecordingSpreadSheet();

		// This is the historical failure window: the IME produces more input before
		// the cell editor has attached. All characters must remain buffered, not only
		// the first one.
		onEdt(() -> {
			sheet.deferEditorAttachment();
			sheet.processKeyEvent(typedKey(sheet, '日'));
		});
		flushEdt();
		typeIntoSheet(sheet, '本');
		typeIntoSheet(sheet, '語');
		onEdt(sheet::attachDeferredEditor);
		typeIntoSheet(sheet, '！');

		assertEquals("日本語！", sheet.editorText());
	}

	@Test
	void ordinaryTextInputAppendsEveryCharacterToTheActiveEditor() throws Exception {
		RecordingSpreadSheet sheet = newRecordingSpreadSheet();
		onEdt(sheet::primeEditorOnNextEdit);

		// The table receives the first key to start editing. Once the editor owns
		// focus, the remaining text goes to that editor through normal Swing input.
		typeIntoSheet(sheet, 'T');
		for (char character : "ask 42".toCharArray()) {
			onEdt(() -> sheet.typeInActiveEditor(character));
		}

		assertEquals("Task 42", sheet.editorText());
	}

	@Test
	void inputMethodStartDispatchesFullImeTextToEditor() throws Exception {
		RecordingSpreadSheet sheet = newRecordingSpreadSheet();
		onEdt(sheet::primeEditorOnNextEdit);

		onEdt(() -> {
			InputMethodEvent event = new InputMethodEvent(sheet, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
				new AttributedString("テスト").getIterator(), 0, null, null);
			sheet.processInputMethodEvent(event);
		});
		SwingUtilities.invokeAndWait(() -> {});

		assertEquals("テスト", sheet.lastDispatchedImeText());
	}

	private static boolean readBooleanField(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getBoolean(target);
	}

	private static KeyEvent typedKey(CommonSpreadSheet source, char character) {
		return new KeyEvent(source, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0,
			KeyEvent.VK_UNDEFINED, character);
	}

	private static RecordingSpreadSheet newRecordingSpreadSheet() throws Exception {
		AtomicReference<RecordingSpreadSheet> sheet = new AtomicReference<>();
		onEdt(() -> sheet.set(new RecordingSpreadSheet()));
		return sheet.get();
	}

	private static void typeIntoSheet(RecordingSpreadSheet sheet, char character) throws Exception {
		onEdt(() -> sheet.processKeyEvent(typedKey(sheet, character)));
		flushEdt();
	}

	private static void onEdt(Runnable action) throws Exception {
		SwingUtilities.invokeAndWait(action);
	}

	private static void flushEdt() throws Exception {
		SwingUtilities.invokeAndWait(() -> { });
	}

	private static final class RecordingSpreadSheet extends CommonSpreadSheet {
		private static final long serialVersionUID = 1L;
		private java.util.EventObject recordedEditEvent;
		private boolean editing;
		private JTextField nextEditor;
		private JTextField deferredEditor;
		private String dispatchedImeText;

		void primeEditor(String text) {
			editorComp = new DispatchTextField(text);
			editing = true;
		}

		void primeEditorOnNextEdit() {
			JTextField field = new DispatchTextField();
			field.addInputMethodListener(new InputMethodListener() {
				@Override
				public void inputMethodTextChanged(InputMethodEvent event) {
					dispatchedImeText = toText(event.getText());
				}

				@Override
				public void caretPositionChanged(InputMethodEvent event) {
				}
			});
			nextEditor = field;
		}

		void deferEditorAttachment() {
			deferredEditor = new JTextField();
		}

		void attachDeferredEditor() {
			editorComp = deferredEditor;
			deferredEditor = null;
		}

		String editorText() {
			return ((JTextField) editorComp).getText();
		}

		String lastDispatchedImeText() {
			return dispatchedImeText;
		}

		void typeInActiveEditor(char character) {
			((DispatchTextField) editorComp).dispatchTypedKey(character);
		}

		@Override
		public boolean isEditing() {
			return editing;
		}

		@Override
		public int getCurrentRow() {
			return 0;
		}

		@Override
		public int getSelectedRow() {
			return 0;
		}

		@Override
		public int getSelectedColumn() {
			return 0;
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public boolean editCellAt(int row, int column, java.util.EventObject e) {
			recordedEditEvent = e;
			if (editorComp == null && nextEditor != null) {
				editorComp = nextEditor;
				nextEditor = null;
			}
			editing = editorComp != null || deferredEditor != null;
			return editing;
		}
	}

	private static final class DispatchTextField extends JTextField {
		private static final long serialVersionUID = 1L;

		DispatchTextField() {
		}

		DispatchTextField(String text) {
			super(text);
		}

		void dispatchTypedKey(char character) {
			processKeyEvent(new KeyEvent(this, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0,
				KeyEvent.VK_UNDEFINED, character));
		}
	}

	private static String toText(AttributedCharacterIterator iterator) {
		if (iterator == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char c = iterator.first(); c != AttributedCharacterIterator.DONE; c = iterator.next()) {
			builder.append(c);
		}
		return builder.toString();
	}
}
