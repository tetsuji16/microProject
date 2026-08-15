package com.microproject.grouping.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutlineCodeTest {
	@Test
	void validatesAndFormatsConfiguredOutlineCodes() {
		OutlineCode outline = new OutlineCode();
		outline.addMask(new OutlineCode.Mask(OutlineCode.NUMBERS, 0, "."));
		outline.addMask(new OutlineCode.Mask(OutlineCode.UPPERCASE_LETTERS, 2, "."));

		assertTrue(outline.isValid("12"));
		assertTrue(outline.isValid("12.AB"));
		assertFalse(outline.isValid("12.ab"));
		assertEquals("12.AB", outline.format("12.AB"));
		assertThrows(IllegalArgumentException.class, () -> outline.format("12.ab"));
	}

	@Test
	void incrementsFixedWidthNumericMask() {
		OutlineCode.Mask mask = new OutlineCode.Mask(OutlineCode.NUMBERS, 3, ".");

		assertEquals("010", mask.nextValue("009"));
	}
}
