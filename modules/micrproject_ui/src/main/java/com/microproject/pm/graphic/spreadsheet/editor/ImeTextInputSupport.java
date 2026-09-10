/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.im.InputContext;
import java.text.AttributedCharacterIterator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

/** Shared boundary between Swing commands and text-input methods. */
public final class ImeTextInputSupport {
	public static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";
	private ImeTextInputSupport() {
	}

	public static boolean isComposing(InputMethodEvent event) {
		AttributedCharacterIterator text = event == null ? null : event.getText();
		return text != null && event.getCommittedCharacterCount() < text.getEndIndex() - text.getBeginIndex();
	}

	public static void updateCompositionState(JComponent component, InputMethodEvent event) {
		if (component != null) {
			component.putClientProperty(COMPOSITION_PROPERTY, Boolean.valueOf(isComposing(event)));
		}
	}

	public static boolean isCompositionActive(JComponent component) {
		return component != null && Boolean.TRUE.equals(component.getClientProperty(COMPOSITION_PROPERTY));
	}

	/** Installs only an editor-local Convert binding; no document-wide key is consumed. */
	public static void installReconversionAction(JComponent editor) {
		if (editor == null) return;
		InputMap inputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = editor.getActionMap();
		String actionKey = "spreadsheet.imeReconvert";
		inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CONVERT, 0), actionKey);
		actionMap.put(actionKey, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) { reconvert(editor); }
		});
	}

	public static boolean reconvert(JComponent editor) {
		if (editor == null) return false;
		try {
			InputContext inputContext = editor.getInputContext();
			if (inputContext != null) {
				inputContext.reconvert();
				return true;
			}
		} catch (RuntimeException ignored) {
			// Input methods are optional; keep the selection and editor intact.
		}
		return false;
	}
}
