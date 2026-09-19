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
package com.microproject.pm.graphic.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.border.Border;

import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.pm.graphic.gantt.GanttParamsImpl;
import com.microproject.util.FlatUiSupport;

class TimeScaleComponentThemeTest {
	@Test
	void timeScaleHeaderUsesSharedHeaderPaletteAndFont() {
		MicroProjectTheme.installLight();
		TimeScaleComponent component = new TimeScaleComponent(null);

		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), component.getBackground());
		assertEquals(FlatUiSupport.headerForeground(), component.getForeground());
		assertEquals(FlatUiSupport.ganttHeaderFont().getSize2D(), component.getFont().getSize2D());
		Border border = component.getBorder();
		assertEquals(FlatUiSupport.tableHeaderBorder().getClass(), border.getClass());
	}

	@Test
	void ganttParamsExposeReadableHeaderFont() {
		GanttParamsImpl params = new GanttParamsImpl();

		assertEquals(FlatUiSupport.ganttHeaderFont().getSize2D(), params.getColumnHeaderFont().getSize2D());
	}

	@Test
	void denseMonthLabelsAreSkippedUntilThereIsRoomAfterThePreviousLabel() {
		// Simulates three month labels at a dense zoom level. The middle label
		// would overlap "Aug" and is omitted; "Oct" remains visible once its
		// start clears the previous label's right edge plus the visual gap.
		assertTrue(TimeScaleComponent.canPaintLabel(2.0d, 24.0d, Double.NEGATIVE_INFINITY));
		assertFalse(TimeScaleComponent.canPaintLabel(14.0d, 24.0d, 26.0d));
		assertTrue(TimeScaleComponent.canPaintLabel(31.0d, 24.0d, 26.0d));
	}
}
