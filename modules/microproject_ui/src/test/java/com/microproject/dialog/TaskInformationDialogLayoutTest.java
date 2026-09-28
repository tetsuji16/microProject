/*******************************************************************************
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
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class TaskInformationDialogLayoutTest {
	@Test
	void viewportUsesContentHeightButNeverExceedsMonitorAllowance() {
		assertEquals(460, TaskInformationDialog.preferredViewportHeight(320, 900));
		assertEquals(700, TaskInformationDialog.preferredViewportHeight(700, 900));
		assertEquals(340, TaskInformationDialog.preferredViewportHeight(700, 500));
		assertEquals(240, TaskInformationDialog.preferredViewportHeight(700, 400));
	}

	@Test
	void dialogLocationIsConstrainedWithinSecondaryMonitorWithNegativeOrigin() {
		Rectangle usable = new Rectangle(-1920, 40, 1920, 1040);
		assertEquals(new Rectangle(-1920, 40, 700, 700),
				TaskInformationDialog.constrainToUsableBounds(new Rectangle(-2500, -100, 700, 700), usable));
		assertEquals(new Rectangle(-1000, 380, 700, 700),
				TaskInformationDialog.constrainToUsableBounds(new Rectangle(-1000, 900, 700, 700), usable));
	}
}
