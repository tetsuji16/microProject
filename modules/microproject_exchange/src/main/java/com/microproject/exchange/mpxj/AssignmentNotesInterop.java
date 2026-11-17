/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange.mpxj;

import java.io.StringReader;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

/** Converts the MPO rich-note representation to the plain-text MSPDI Notes element. */
public final class AssignmentNotesInterop {
	private AssignmentNotesInterop() { }

	/** MSPDI Notes is text-only, so rich notes are exported as their readable text. */
	public static String toMspdiText(String notes) {
		if (notes == null || !notes.stripLeading().startsWith("{\\rtf")) return notes;
		try {
			RTFEditorKit kit = new RTFEditorKit();
			DefaultStyledDocument document = new DefaultStyledDocument();
			kit.read(new StringReader(notes), document, 0);
			return document.getText(0, document.getLength());
		} catch (Exception malformedRtf) {
			// Preserve user data as text if a legacy or edited note contains malformed RTF.
			return notes;
		}
	}
}
