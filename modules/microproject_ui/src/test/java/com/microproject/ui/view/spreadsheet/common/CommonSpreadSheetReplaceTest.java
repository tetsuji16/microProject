/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommonSpreadSheetReplaceTest {
	@Test
	void replacesEveryOccurrenceIgnoringCaseUnlessMatchCaseIsOn() {
		assertEquals("X b X", CommonSpreadSheet.replaceText("a b A", "a", "X", false));
		assertEquals("X b A", CommonSpreadSheet.replaceText("a b A", "a", "X", true));
	}

	@Test
	void treatsTheSearchAndTheReplacementAsPlainText() {
		assertEquals("c$1d", CommonSpreadSheet.replaceText("c.*d", ".*", "$1", false));
		assertEquals("abc", CommonSpreadSheet.replaceText("a(b)c", "(b)", "b", true));
	}
}
