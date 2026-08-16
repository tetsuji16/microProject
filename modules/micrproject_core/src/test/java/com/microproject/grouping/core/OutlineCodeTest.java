/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
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
