package com.projectlibre1.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.text.AttributedString;
import java.text.SimpleDateFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.ChangeAwareTextField;

class EditorSelectionBehaviorTest {
	@Test
	void programmaticClearCanBeCommittedAfterEditorInitialization() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ChangeAwareTextField field = new ChangeAwareTextField();
			field.setText("Existing task");
			field.resetChange();

			// Mirrors the spreadsheet clear path: clear during setup, then reset
			// the initialization dirtiness before marking the explicit edit.
			field.setText("");
			field.resetChange();
			field.markChanged();

			assertTrue(field.hasChanged());
		});
	}

	@Test
	void keyboardFocusSpinnerSelectAllKeepsFullSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			KeyboardFocusSpinner spinner = new KeyboardFocusSpinner(new SpinnerNumberModel(12.0, 0.0, 100.0, 1.0));
			spinner.setEditor(new javax.swing.JSpinner.NumberEditor(spinner, "0"));
			spinner.getTextField().setText("12345");

			spinner.selectAll(true);

			assertEquals("12345", spinner.getTextField().getSelectedText());
		});
	}

	@Test
	void dateFieldMouseSelectionRestoresFullSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			DateEditor.ExtDateField field = new DateEditor.ExtDateField(new SimpleDateFormat("yyyy/MM/dd"));
			field.getTextField().setText("2026/06/06");
			field.selectAll(false);

			assertEquals("2026/06/06", field.getTextField().getSelectedText());
		});
	}

	@Test
	void spreadsheetEditorAcceptsImeCompositionAsEditStart() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTextField field = new JTextField();
			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new DefaultCellEditor(field));
			InputMethodEvent event = new InputMethodEvent(field, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString("テ").getIterator(), 0, null, null);

			assertTrue(adapter.isCellEditable(event));
		});
	}

	@Test
	void spreadsheetEditorAcceptsConvertKeyForReconversion() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTextField field = new JTextField();
			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new DefaultCellEditor(field));
			KeyEvent event = new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_CONVERT, KeyEvent.CHAR_UNDEFINED);

			assertTrue(adapter.isCellEditable(event));
		});
	}

	@Test
	void spreadsheetEditorRoutesConvertKeyToImeWithoutChangingTheSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTextField field = new JTextField("確定済みの文字");
			SpreadSheetCellEditorAdapter adapter = new SpreadSheetCellEditorAdapter(new DefaultCellEditor(field));
			adapter.prepareEditorComponent(field);
			field.select(2, 5);

			Object actionKey = field.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_CONVERT, 0));
			assertEquals("spreadsheet.imeReconvert", actionKey);
			Action action = field.getActionMap().get(actionKey);
			assertNotNull(action);
			action.actionPerformed(new java.awt.event.ActionEvent(field, java.awt.event.ActionEvent.ACTION_PERFORMED, "convert"));

			assertEquals("済みの", field.getSelectedText());
		});
	}

	@Test
	void spreadsheetNameCellEditorTracksImeCompositionOnInnerTextField() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheetNameCellEditor adapter = new SpreadSheetNameCellEditor(new SimpleEditor(String.class));
			JTextField textField = new JTextField();

			adapter.prepareEditorComponent(textField);
			assertTrue(hasCompositionListener(textField));
			assertEquals(Boolean.FALSE, textField.getClientProperty("projectlibre.input.composing"));

			fireInputMethodState(textField, "テ", 0);
			assertEquals(Boolean.TRUE, textField.getClientProperty("projectlibre.input.composing"));
			assertFalse(adapter.stopCellEditing());

			fireInputMethodState(textField, "テ", 1);
			assertEquals(Boolean.FALSE, textField.getClientProperty("projectlibre.input.composing"));
			assertTrue(adapter.stopCellEditing());
		});
	}

	@Test
	void spreadsheetNameCellEditorBlocksCommitUntilMultiCharImeCompositionFinishes() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheetNameCellEditor adapter = new SpreadSheetNameCellEditor(new SimpleEditor(String.class));
			JTextField textField = new JTextField();

			adapter.prepareEditorComponent(textField);
			fireInputMethodState(textField, "テスト", 1);
			assertEquals(Boolean.TRUE, textField.getClientProperty("projectlibre.input.composing"));
			assertFalse(adapter.stopCellEditing());

			fireInputMethodState(textField, "テスト", 3);
			assertEquals(Boolean.FALSE, textField.getClientProperty("projectlibre.input.composing"));
			assertTrue(adapter.stopCellEditing());
			assertEquals(Boolean.FALSE, textField.getClientProperty("projectlibre.input.composing"));
		});
	}

	private static void fireInputMethodState(JTextField textField, String text, int committedCharacters) {
		InputMethodEvent event = new InputMethodEvent(textField, InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new AttributedString(text).getIterator(), committedCharacters, null, null);
		for (InputMethodListener listener : textField.getInputMethodListeners()) {
			if (listener.getClass().getName().contains("SpreadSheetCellEditorAdapter")) {
				listener.inputMethodTextChanged(event);
			}
		}
	}

	private static boolean hasCompositionListener(JTextField textField) {
		for (InputMethodListener listener : textField.getInputMethodListeners()) {
			if (listener.getClass().getName().contains("SpreadSheetCellEditorAdapter")) {
				return true;
			}
		}
		return false;
	}
}
