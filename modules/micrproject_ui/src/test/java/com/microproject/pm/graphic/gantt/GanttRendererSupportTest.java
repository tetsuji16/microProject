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
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.util.MondayComPalette;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttRendererSupportTest {
	@Test
	void annotationLayoutPrefersRightSideWhenSpaceExists() {
		GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(
				new Rectangle(0, 0, 200, 40),
				20.0d,
				90.0d,
				8,
				50);

		assertEquals(98, layout.x);
		assertEquals(64, layout.availableWidth);
	}

	@Test
	void annotationLayoutReturnsNullWhenNothingIsVisible() {
		assertNull(GanttRendererSupport.resolveAnnotationLayout(
				new Rectangle(0, 0, 80, 40),
				200.0d,
				260.0d,
				8,
				40));
	}

	@Test
	void clipAnnotationTextShortensWithEllipsis() {
		FontMetrics metrics = createMetrics();
		String clipped = GanttRendererSupport.clipAnnotationText(metrics, "project milestone", 40);

		assertTrue(clipped.endsWith("..."));
		assertTrue(clipped.length() < "project milestone".length());
	}

	@Test
	void endpointColorFollowsUniformEndpointRule() {
		BarFormat format = new BarFormat();
		format.setId("Bar.task");
		Color status = Color.RED;
		Color accent = Color.BLUE;

		assertEquals(status, GanttRendererSupport.resolveEndpointColor(format, status, accent));

		format.setId("Bar.custom");
		assertEquals(accent, GanttRendererSupport.resolveEndpointColor(format, status, accent));
	}

	@Test
	void annotationKeyUsesBothFieldAndFormatIds() {
		BarFormat format = new BarFormat();
		format.setId("Bar.task");
		com.microproject.field.Field field = new com.microproject.field.Field();
		field.setName("name");

		assertEquals("name|Bar.task", GanttRendererSupport.annotationKey(field, format));
	}

	@Test
	void ganttRendererDefaultsToMondayComPalette() {
		GanttRenderer renderer = new GanttRenderer();
		assertTrue(renderer.getPalette() instanceof MondayComPalette);
	}

	private static FontMetrics createMetrics() {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			return g2.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
		} finally {
			g2.dispose();
		}
	}
}
