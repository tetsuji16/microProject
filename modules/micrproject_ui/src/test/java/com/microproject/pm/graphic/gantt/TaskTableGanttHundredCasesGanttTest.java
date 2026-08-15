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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.BarFormat;

class TaskTableGanttHundredCasesGanttTest {
	private record GeometryCase(double center, double shapeHeight, double selectionSquare) {}
	private record LayoutCase(Rectangle clip, double x0, double x1, int offset, int width, boolean visible) {}
	private record ClipCase(String text, int width, String expectation) {}
	private record KeyCase(String fieldName, String formatId, String expected) {}

	@TestFactory
	Stream<DynamicTest> milestoneSelectionGeometryCases() {
		List<GeometryCase> cases = List.of(
			new GeometryCase(0, 1, 1), new GeometryCase(10, 2, 8),
			new GeometryCase(25, 12, 4), new GeometryCase(50, 0, 10),
			new GeometryCase(75, 15, 15), new GeometryCase(100, 24, 12),
			new GeometryCase(125, 7, 18), new GeometryCase(150, 30, 5),
			new GeometryCase(175, 9, 20), new GeometryCase(200, 40, 40),
			new GeometryCase(-10, 6, 14), new GeometryCase(-50, 22, 10),
			new GeometryCase(0.5, 3.5, 9.5), new GeometryCase(99.25, 11.5, 4.5),
			new GeometryCase(1000, 60, 16), new GeometryCase(2048, 16, 64),
			new GeometryCase(5, 0, 0), new GeometryCase(12, 0.5, 0.25),
			new GeometryCase(300, 13, 14), new GeometryCase(400, 14, 13));
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id("G", index + 1), () -> {
				GeometryCase c = cases.get(index);
				double expectedWidth = Math.max(c.shapeHeight, c.selectionSquare);
				double start = GanttSelectionGeometrySupport.milestoneSelectionStart(
					c.center, c.shapeHeight, c.selectionSquare);
				double end = GanttSelectionGeometrySupport.milestoneSelectionEnd(
					c.center, c.shapeHeight, c.selectionSquare);
				assertEquals(c.center, (start + end) / 2.0d, 0.000001d);
				assertEquals(expectedWidth, end - start, 0.000001d);
			}));
	}

	@TestFactory
	Stream<DynamicTest> annotationLayoutCases() {
		Rectangle normal = new Rectangle(0, 0, 200, 40);
		List<LayoutCase> cases = List.of(
			new LayoutCase(normal, 10, 20, 8, 40, true),
			new LayoutCase(normal, -10, 10, 8, 60, true),
			new LayoutCase(normal, 190, 210, 8, 50, true),
			new LayoutCase(normal, 50, 150, 4, 80, true),
			new LayoutCase(normal, 0, 0, 0, 20, true),
			new LayoutCase(normal, 200, 200, 0, 20, true),
			new LayoutCase(normal, -100, -20, 8, 50, true),
			new LayoutCase(normal, 220, 250, 8, 50, true),
			new LayoutCase(new Rectangle(100, 0, 300, 40), 90, 110, 5, 70, true),
			new LayoutCase(new Rectangle(-100, 0, 200, 40), -20, 20, 10, 90, true),
			new LayoutCase(normal, -400, -350, 8, 40, false),
			new LayoutCase(normal, 350, 400, 8, 40, false),
			new LayoutCase(normal, -1000, -900, 0, 10, false),
			new LayoutCase(normal, 900, 1000, 0, 10, false),
			new LayoutCase(new Rectangle(50, 0, 20, 40), -100, -80, 2, 10, false),
			new LayoutCase(new Rectangle(50, 0, 20, 40), 120, 140, 2, 10, false),
			new LayoutCase(new Rectangle(0, 0, 0, 40), 50, 60, 8, 40, false),
			new LayoutCase(new Rectangle(-200, 0, 100, 40), 0, 20, 8, 30, false),
			new LayoutCase(new Rectangle(500, 0, 100, 40), 0, 20, 8, 30, false),
			new LayoutCase(null, 0, 20, 8, 30, false));
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id("L", index + 21), () -> {
				LayoutCase c = cases.get(index);
				GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(
					c.clip, c.x0, c.x1, c.offset, c.width);
				if (!c.visible) {
					assertNull(layout);
					return;
				}
				assertNotNull(layout);
				assertTrue(layout.availableWidth > 0);
				assertTrue(layout.x <= c.clip.x + c.clip.width);
				assertTrue(layout.x + layout.availableWidth <= c.clip.x + c.clip.width - 4);
			}));
	}

	@TestFactory
	Stream<DynamicTest> annotationClippingCases() {
		List<ClipCase> cases = List.of(
			new ClipCase(null, 40, "null"),
			new ClipCase("   ", 40, "empty"),
			new ClipCase("Task", 0, "null"),
			new ClipCase(" Task ", 200, "Task"),
			new ClipCase("A", 1, "A"),
			new ClipCase("Milestone alpha", 30, "fits"),
			new ClipCase("Long task annotation", 45, "fits"),
			new ClipCase("日本語の工程注釈", 35, "fits"),
			new ClipCase("12345678901234567890", 60, "fits"),
			new ClipCase("Finish-to-start dependency", 90, "fits"));
		FontMetrics metrics = createMetrics();
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id("C", index + 41), () -> {
				ClipCase c = cases.get(index);
				String actual = GanttRendererSupport.clipAnnotationText(metrics, c.text, c.width);
				switch (c.expectation) {
					case "null" -> assertNull(actual);
					case "empty" -> assertEquals("", actual);
					case "Task" -> assertEquals("Task", actual);
					case "A" -> assertEquals("A", actual);
					default -> {
						assertNotNull(actual);
						assertTrue(actual.length() <= c.text.trim().length());
						if (actual.length() > 1)
							assertTrue(metrics.stringWidth(actual) <= c.width);
					}
				}
			}));
	}

	@TestFactory
	Stream<DynamicTest> annotationKeyCases() {
		List<KeyCase> cases = List.of(
			new KeyCase(null, null, "|"), new KeyCase("name", null, "name|"),
			new KeyCase(null, "Bar.task", "|Bar.task"), new KeyCase("name", "Bar.task", "name|Bar.task"),
			new KeyCase("start", "Bar.critical", "start|Bar.critical"),
			new KeyCase("finish", "", "finish|"), new KeyCase("", "Bar.summary", "|Bar.summary"),
			new KeyCase("Field.進捗", "Bar.progress", "Field.進捗|Bar.progress"),
			new KeyCase("a|b", "c|d", "a|b|c|d"), new KeyCase(" notes ", " custom ", " notes | custom "));
		return IntStream.range(0, cases.size()).mapToObj(index -> DynamicTest.dynamicTest(
			id("K", index + 51), () -> {
				KeyCase c = cases.get(index);
				Field field = c.fieldName == null ? null : new Field();
				if (field != null) field.setName(c.fieldName);
				BarFormat format = c.formatId == null ? null : new BarFormat();
				if (format != null) format.setId(c.formatId);
				assertEquals(c.expected, GanttRendererSupport.annotationKey(field, format));
			}));
	}

	private static String id(String group, int number) {
		return "TC100-" + group + String.format("%03d", number);
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
