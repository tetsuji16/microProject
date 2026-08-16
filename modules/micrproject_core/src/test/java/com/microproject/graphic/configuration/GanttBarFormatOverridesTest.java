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
package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;

class GanttBarFormatOverridesTest {
	@Test
	void storesFormattingByStableTaskIdAndView() {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, new BarFormat(null, 0x123456, null));
		overrides.set(GanttBarFormatOverrides.TRACKING_VIEW, 42L, new BarFormat(0x654321, null, null));

		assertEquals(0x123456, overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 42L).getMiddleRgb());
		assertEquals(0x654321, overrides.get(GanttBarFormatOverrides.TRACKING_VIEW, 42L).getStartRgb());
		assertTrue(overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 43L).isAutomatic());
	}

	@Test
	void automaticFormattingRemovesStoredOverride() {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, new BarFormat(null, 0x123456, null));
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 42L, BarFormat.automatic());

		assertTrue(overrides.isEmpty());
		assertTrue(overrides.get(GanttBarFormatOverrides.STANDARD_VIEW, 42L).isAutomatic());
	}

	@Test
	void fillColorAppliesToAllBarPartsAndCanResetThemTogether() {
		BarFormat colored = new BarFormat(0x111111, 0x222222, 0x333333).withFillRgb(0xABCDEF);

		assertEquals(0xABCDEF, colored.getStartRgb());
		assertEquals(0xABCDEF, colored.getMiddleRgb());
		assertEquals(0xABCDEF, colored.getEndRgb());
		assertTrue(colored.withFillRgb(null).isAutomatic());
	}

	@Test
	void normalizesRgbAndPreservesItAcrossSerialization() throws Exception {
		GanttBarFormatOverrides overrides = new GanttBarFormatOverrides();
		overrides.set(GanttBarFormatOverrides.STANDARD_VIEW, 7L, new BarFormat(0xFFABCDEF, null, 0x00123456));

		byte[] serialized;
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(overrides);
			serialized = bytes.toByteArray();
		}

		GanttBarFormatOverrides restored;
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
			restored = (GanttBarFormatOverrides) input.readObject();
		}

		BarFormat format = restored.get(GanttBarFormatOverrides.STANDARD_VIEW, 7L);
		assertEquals(0xABCDEF, format.getStartRgb());
		assertEquals(0x123456, format.getEndRgb());
		assertNull(format.getMiddleRgb());
	}
}
