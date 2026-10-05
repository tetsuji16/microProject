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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.BarFormat;

class GanttAnnotationAndMilestoneGeometryTest {
	private record GeometryCase(String name, double center, double shapeHeight, double selectionSquare,
		double expectedStart, double expectedEnd) {}
	private record LayoutCase(String name, Rectangle clip, double x0, double x1, int offset, int width,
		Integer expectedX, Integer expectedAvailableWidth) {}
	private record ClipCase(String name, String text, int width, String expectation) {}
	private record KeyCase(String name, String fieldName, String formatId, String expected) {}

	@TestFactory
	Stream<DynamicTest> milestoneSelectionGeometryCases() {
		List<GeometryCase> cases = List.of(
			new GeometryCase("selection-square-wider-than-shape", 100, 8, 12, 94, 106),
			new GeometryCase("shape-wider-than-selection-square", 100, 20, 12, 90, 110),
			new GeometryCase("equal-extents", 100, 12, 12, 94, 106),
			new GeometryCase("zero-extents", 100, 0, 0, 100, 100),
			new GeometryCase("fractional-extents", 99.25, 11.5, 4.5, 93.5, 105),
			new GeometryCase("negative-center", -10, 6, 14, -17, -3));
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			double start = GanttSelectionGeometrySupport.milestoneSelectionStart(
				c.center, c.shapeHeight, c.selectionSquare);
			double end = GanttSelectionGeometrySupport.milestoneSelectionEnd(
				c.center, c.shapeHeight, c.selectionSquare);
			assertEquals(c.expectedStart, start, 0.000001d);
			assertEquals(c.expectedEnd, end, 0.000001d);
		}));
	}

	@TestFactory
	Stream<DynamicTest> annotationLayoutCases() {
		Rectangle normal = new Rectangle(0, 0, 200, 40);
		List<LayoutCase> cases = List.of(
			new LayoutCase("visible-bar-uses-right-side", normal, 20, 90, 8, 50, 98, 64),
			new LayoutCase("right-edge-lack-of-space-falls-back-left", normal, 190, 210, 8, 50, 132, 64),
			new LayoutCase("partially-visible-left-edge-bar-keeps-right-label", normal, -10, 10, 8, 60, 18, 64),
			new LayoutCase("label-can-remain-visible-after-bar-leaves-left-clip", normal, -100, -20, 8, 50, -12, 64),
			new LayoutCase("bar-and-label-outside-clip", normal, -400, -350, 8, 40, null, null),
			new LayoutCase("missing-clip", null, 0, 20, 8, 30, null, null));
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(
				c.clip, c.x0, c.x1, c.offset, c.width);
			if (c.expectedX == null) {
				assertNull(layout);
				return;
			}
			assertNotNull(layout);
			assertEquals(c.expectedX.intValue(), layout.x);
			assertEquals(c.expectedAvailableWidth.intValue(), layout.availableWidth);
		}));
	}

	@TestFactory
	Stream<DynamicTest> annotationClippingCases() {
		List<ClipCase> cases = List.of(
			new ClipCase("null-text", null, 40, "null"),
			new ClipCase("blank-text", "   ", 40, "empty"),
			new ClipCase("nonpositive-width", "Task", 0, "null"),
			new ClipCase("trimmed-text-fits", " Task ", 200, "Task"),
			new ClipCase("ellipsis-too-wide", "A", 1, "A"),
			new ClipCase("text-fits-unmodified", "Milestone alpha", 200, "fits"),
			new ClipCase("ascii-text-clips", "Long task annotation", 40, "clipped"),
			new ClipCase("multibyte-text-clips", "日本語の工程注釈", 40, "clipped"));
		FontMetrics metrics = createMetrics();
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			String actual = GanttRendererSupport.clipAnnotationText(metrics, c.text, c.width);
			switch (c.expectation) {
				case "null" -> assertNull(actual);
				case "empty" -> assertEquals("", actual);
				case "Task" -> assertEquals("Task", actual);
				case "A" -> assertEquals("A", actual);
				case "fits" -> assertEquals(c.text.trim(), actual);
				default -> {
					assertNotNull(actual);
					String prefix = actual.substring(0, actual.length() - 3);
					assertTrue(actual.endsWith("..."), "clipped text must have an ellipsis: " + actual);
					assertTrue(c.text.trim().startsWith(prefix), "clipped text must retain a source prefix");
					assertTrue(actual.length() < c.text.trim().length(), "clipping must shorten the text");
					assertTrue(metrics.stringWidth(actual) <= c.width, "clipped text must fit the available width");
				}
			}
		}));
	}

	@TestFactory
	Stream<DynamicTest> annotationKeyCases() {
		List<KeyCase> cases = List.of(
			new KeyCase("null-field-and-format", null, null, "|"),
			new KeyCase("null-format", "name", null, "name|"),
			new KeyCase("null-field", null, "Bar.task", "|Bar.task"),
			new KeyCase("ordinary-pair", "name", "Bar.task", "name|Bar.task"),
			new KeyCase("alternate-format", "start", "Bar.critical", "start|Bar.critical"),
			new KeyCase("empty-format-id", "finish", "", "finish|"),
			new KeyCase("empty-field-name", "", "Bar.summary", "|Bar.summary"),
			new KeyCase("unicode-field-name", "Field.進捗", "Bar.progress", "Field.進捗|Bar.progress"),
			new KeyCase("delimiter-in-values", "a|b", "c|d", "a|b|c|d"),
			new KeyCase("preserves-whitespace", " notes ", " custom ", " notes | custom "));
		return cases.stream().map(c -> DynamicTest.dynamicTest(c.name, () -> {
			Field field = c.fieldName == null ? null : new Field();
			if (field != null) field.setName(c.fieldName);
			BarFormat format = c.formatId == null ? null : new BarFormat();
			if (format != null) format.setId(c.formatId);
			assertEquals(c.expected, GanttRendererSupport.annotationKey(field, format));
		}));
	}

	private static FontMetrics createMetrics() {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			return graphics.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
		} finally {
			graphics.dispose();
		}
	}
}
