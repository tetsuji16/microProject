/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.text.AbstractDocument;
import javax.swing.text.PlainDocument;
import org.junit.jupiter.api.Test;

class FixedSizeFilterTest {
	@Test
	void truncatesInsertionAndReplacementToRemainingCapacity() throws Exception {
		PlainDocument document = new PlainDocument();
		((AbstractDocument) document).setDocumentFilter(new FixedSizeFilter(3));

		document.insertString(0, "abcd", null);
		assertEquals("abc", document.getText(0, document.getLength()));

		document.replace(1, 1, "XYZ", null);
		assertEquals("aXc", document.getText(0, document.getLength()));
	}
}
