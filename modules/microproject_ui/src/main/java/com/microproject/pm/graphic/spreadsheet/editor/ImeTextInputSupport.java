/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.im.InputContext;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.text.AttributedCharacterIterator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

/** Shared boundary between Swing commands and text-input methods. */
public final class ImeTextInputSupport {
	public static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";
	public enum ReconversionResult { REQUESTED, UNSUPPORTED }
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
		installReconversionAction(editor, editor == null ? null : editor::getInputContext, message -> {
			java.awt.Component parent = editor == null ? null : editor.getParent();
			while (parent != null && !(parent instanceof com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet)) parent = parent.getParent();
			if (parent instanceof com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet sheet)
				sheet.reportImeUnsupported(message);
		});
	}

	static void installReconversionAction(JComponent editor, Supplier<InputContext> inputContextSupplier, Consumer<String> unsupportedHandler) {
		if (editor == null) return;
		InputMap inputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = editor.getActionMap();
		String actionKey = "spreadsheet.imeReconvert";
		inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CONVERT, 0), actionKey);
		actionMap.put(actionKey, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) {
				if (reconvert(editor, inputContextSupplier) == ReconversionResult.UNSUPPORTED && unsupportedHandler != null)
					unsupportedHandler.accept(com.microproject.dialog.UsabilityStrings.text("spreadsheet.imeReconversionUnsupported"));
			}
		});
	}

	public static ReconversionResult reconvert(JComponent editor) { return reconvert(editor, editor == null ? null : editor::getInputContext); }

	static ReconversionResult reconvert(JComponent editor, Supplier<InputContext> inputContextSupplier) {
		if (editor == null) return ReconversionResult.UNSUPPORTED;
		try {
			InputContext inputContext = inputContextSupplier.get();
			if (inputContext != null) {
				inputContext.reconvert();
				return ReconversionResult.REQUESTED;
			}
		} catch (RuntimeException ignored) {
			// Input methods are optional; keep the selection and editor intact.
		}
		return ReconversionResult.UNSUPPORTED;
	}
}
