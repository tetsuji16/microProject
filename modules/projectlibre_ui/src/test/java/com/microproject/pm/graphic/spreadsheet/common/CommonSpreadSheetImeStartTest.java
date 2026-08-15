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
		AtomicReference<RecordingSpreadSheet> sheetRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> sheetRef.set(new RecordingSpreadSheet()));
		var sheet = sheetRef.get();
		SwingUtilities.invokeAndWait(() -> {
			sheet.primeEditor("既存");
			sheet.processKeyEvent(new KeyEvent(sheet, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'テ'));
		});
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {
			sheet.processKeyEvent(new KeyEvent(sheet, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'ス'));
		});
		SwingUtilities.invokeAndWait(() -> {});

		assertEquals("既存テス", sheet.editorText());
	}

	@Test
	void inputMethodStartDispatchesFullImeTextToEditor() throws Exception {
		AtomicReference<RecordingSpreadSheet> sheetRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			RecordingSpreadSheet sheet = new RecordingSpreadSheet();
			sheet.primeEditorOnNextEdit();
			sheetRef.set(sheet);
		});
		RecordingSpreadSheet sheet = sheetRef.get();

		SwingUtilities.invokeAndWait(() -> {
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

	private static final class RecordingSpreadSheet extends CommonSpreadSheet {
		private static final long serialVersionUID = 1L;
		private java.util.EventObject recordedEditEvent;
		private boolean editing;
		private JTextField nextEditor;
		private String dispatchedImeText;

		void primeEditor(String text) {
			editorComp = new JTextField(text);
			editing = true;
		}

		void primeEditorOnNextEdit() {
			JTextField field = new JTextField();
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

		String editorText() {
			return ((JTextField) editorComp).getText();
		}

		String lastDispatchedImeText() {
			return dispatchedImeText;
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
			editing = editorComp != null;
			return editorComp != null;
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
