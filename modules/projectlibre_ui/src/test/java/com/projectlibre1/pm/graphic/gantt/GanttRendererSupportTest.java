package com.projectlibre1.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.util.MondayComPalette;
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
		com.projectlibre1.field.Field field = new com.projectlibre1.field.Field();
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
