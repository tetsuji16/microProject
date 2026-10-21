/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputMethodEvent;
import java.awt.im.InputContext;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.text.AttributedCharacterIterator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.pm.graphic.frames.DocumentFrame;

/** Shared boundary between Swing commands and text-input methods. */
public final class ImeTextInputSupport {
	public static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";
	public static final String RECONVERT_ACTION_KEY = "spreadsheet.imeReconvert";
	/** {@code REQUESTED} means the input method accepted the request, not that text changed. */
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
		if (editor == null) return;
		editor.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_CONVERT, 0), RECONVERT_ACTION_KEY);
		editor.getActionMap().put(RECONVERT_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) {
				requestReconversion(editor);
			}
		});
	}

	/**
	 * The single Convert-key reconversion route, shared by the editor-local
	 * binding and the spreadsheet's start-editing-for-Convert path.  Native
	 * reconversion is requested from the editor's input context; when the
	 * platform cannot provide it the edited text and selection are left
	 * untouched and the owning document reports that reconversion is
	 * unavailable instead of silently claiming success.
	 * <p>
	 * The JDK's Windows input-method adapter ({@code sun.awt.windows.WInputMethod})
	 * does not override {@code InputMethodAdapter.reconvert()}, so on Windows
	 * {@link InputContext#reconvert()} always throws
	 * {@link UnsupportedOperationException} regardless of the active IME (#866).
	 */
	public static ReconversionResult requestReconversion(JComponent editor) {
		return requestReconversion(editor, editor == null ? null : editor::getInputContext,
			message -> reportToDocument(editor, message));
	}

	static ReconversionResult requestReconversion(JComponent editor, Supplier<InputContext> inputContextSupplier,
			Consumer<String> unsupportedHandler) {
		ReconversionResult result = reconvert(editor, inputContextSupplier);
		if (result == ReconversionResult.UNSUPPORTED && unsupportedHandler != null)
			unsupportedHandler.accept(UsabilityStrings.text("spreadsheet.imeReconversionUnsupported"));
		return result;
	}

	private static ReconversionResult reconvert(JComponent editor, Supplier<InputContext> inputContextSupplier) {
		if (editor == null || inputContextSupplier == null) return ReconversionResult.UNSUPPORTED;
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

	private static void reportToDocument(JComponent editor, String message) {
		if (SwingUtilities.getAncestorOfClass(DocumentFrame.class, editor) instanceof DocumentFrame frame)
			frame.showStatusMessage(message);
	}
}
