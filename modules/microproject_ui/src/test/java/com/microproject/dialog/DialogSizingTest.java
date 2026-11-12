/*
 * MIT License
 *
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
 */
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class DialogSizingTest {
	@Test
	void clampLeavesSizesThatFitUntouched() {
		Dimension clamped = DialogSizing.clamp(new Dimension(900, 600), new Rectangle(0, 0, 1920, 1040), 24);

		assertEquals(new Dimension(900, 600), clamped);
	}

	@Test
	void clampShrinksSizesToUsableAreaMinusMargins() {
		// 1280x720 at 150% scaling leaves 1280x680 of usable work area.
		Dimension clamped = DialogSizing.clamp(new Dimension(1200, 760), new Rectangle(0, 0, 1280, 680), 24);

		assertEquals(new Dimension(1200, 632), clamped);
	}

	@Test
	void clampNeverReturnsNonPositiveSizes() {
		Dimension clamped = DialogSizing.clamp(new Dimension(500, 500), new Rectangle(0, 0, 20, 20), 24);

		assertEquals(new Dimension(1, 1), clamped);
	}

	@Test
	void fontSizeModelSnapsSmallValuesAndTreatsZeroAsAutomatic() {
		PreferencesDialogBox.FontSizeModel model = new PreferencesDialogBox.FontSizeModel(0);
		assertEquals(0, model.getNumber().intValue());
		assertEquals(8, model.getNextValue());
		model.setValue(5);
		assertEquals(8, model.getNumber().intValue());
		assertEquals(0, model.getPreviousValue());
		model.setValue(32);
		assertNull(model.getNextValue());
		assertEquals(8, new PreferencesDialogBox.FontSizeModel(3).getNumber().intValue());
	}
}
