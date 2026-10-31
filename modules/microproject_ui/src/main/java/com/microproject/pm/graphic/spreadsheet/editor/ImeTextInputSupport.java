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
import javax.swing.text.JTextComponent;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.pm.graphic.frames.DocumentFrame;

/** Shared boundary between Swing commands and text-input methods. */
public final class ImeTextInputSupport {
	public static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";
	public static final String RECONVERT_ACTION_KEY = "spreadsheet.imeReconvert";
	/**
	 * {@code REQUESTED} means the request was passed on, not that text changed; on
	 * Windows an IME may still decline it, which is reported to the document later.
	 */
	public enum ReconversionResult { REQUESTED, UNSUPPORTED, NOTHING_TO_CONVERT }
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

	/**
	 * True for a plain printable KEY_TYPED character that an editable text component
	 * should insert itself: no Ctrl/Alt/Meta, not a control character. Shared by the
	 * text and date editors that must not lose a physical multi-character burst.
	 */
	public static boolean isPlainTypedCharacter(KeyEvent event, JTextComponent field) {
		return event.getID() == KeyEvent.KEY_TYPED && event.getKeyChar() != KeyEvent.CHAR_UNDEFINED
				&& !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()
				&& !Character.isISOControl(event.getKeyChar()) && field.isEditable() && field.isEnabled();
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
	 * binding and the spreadsheet's start-editing-for-Convert path.  When the
	 * platform cannot provide reconversion the edited text and selection are
	 * left untouched and the owning document reports that it is unavailable
	 * instead of silently claiming success.
	 * <p>
	 * The JDK's Windows input-method adapter ({@code sun.awt.windows.WInputMethod})
	 * does not override {@code InputMethodAdapter.reconvert()}, so on Windows
	 * {@link InputContext#reconvert()} always throws
	 * {@link UnsupportedOperationException} regardless of the active IME.  Windows
	 * text editors therefore go through {@link WindowsImeReconversion} (#991).
	 */
	public static ReconversionResult requestReconversion(JComponent editor) {
		Consumer<String> report = message -> reportToDocument(editor, message);
		if (WindowsImeReconversion.isWindows() && editor instanceof JTextComponent text)
			return requestWindowsReconversion(text, report);
		return requestReconversion(editor, editor == null ? null : editor::getInputContext, report);
	}

	private static ReconversionResult requestWindowsReconversion(JTextComponent text, Consumer<String> report) {
		if (isCompositionActive(text))
			return ReconversionResult.REQUESTED;
		String value = text.getText();
		if (value.isEmpty())
			return ReconversionResult.NOTHING_TO_CONVERT;
		WindowsImeReconversion.request(text, value, text.getSelectionStart(), text.getSelectionEnd(),
			() -> report.accept(UsabilityStrings.text("spreadsheet.imeReconversionUnsupported")));
		return ReconversionResult.REQUESTED;
	}

	/** Lets a physical acceptance test pick the oracle matching the desktop it runs on. */
	public static boolean isJapaneseImeActive() {
		return WindowsImeReconversion.isJapaneseImeActive();
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
