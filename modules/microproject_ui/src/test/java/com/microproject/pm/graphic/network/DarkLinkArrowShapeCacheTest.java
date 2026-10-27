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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *******************************************************************************/
package com.microproject.pm.graphic.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;
import java.awt.Stroke;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.graphic.configuration.shape.PredefinedShape;
import com.microproject.graphic.configuration.shape.PredefinedStroke;

class DarkLinkArrowShapeCacheTest {
	@Test
	void reusesThemedArrowUntilTheMutableSourceSignatureChanges() {
		DarkLinkArrowShapeCache cache = new DarkLinkArrowShapeCache();
		TexturedShape source = new TexturedShape();
		Color originalColor = new Color(0x37474f);
		Color darkConnector = new Color(0x8a8a8a);
		Stroke originalStroke = PredefinedStroke.SOLID;
		source.setShape(PredefinedShape.find("LINK_ARROW1"));
		source.setColor(originalColor);
		source.setPaint(originalColor);
		source.setStroke(originalStroke);
		source.setShapeScaleX(10d);
		source.setShapeScaleY(5d);

		TexturedShape first = cache.resolve(source, darkConnector);
		assertSame(first, cache.resolve(source, darkConnector), "unchanged arrows reuse their cached render copy");
		assertEquals(originalColor, source.getColor(), "the cached path must not mutate the source color");
		assertEquals(originalColor, source.getPaint(), "the cached path must not mutate the source paint");
		assertEquals(originalStroke, source.getStroke(), "the cached path must not mutate the source stroke");
		assertNotSame(source, first, "the renderer must use an isolated copy");

		source.setShapeScaleX(11d);
		TexturedShape updated = cache.resolve(source, darkConnector);
		assertNotSame(first, updated, "a changed arrow geometry invalidates the cached copy");
		assertEquals(11d, updated.getShapeScaleX());
		assertSame(updated, cache.resolve(source, darkConnector));

		source.setShape(PredefinedShape.find("ARROW_UP"));
		TexturedShape changedShape = cache.resolve(source, darkConnector);
		assertNotSame(updated, changedShape, "a changed mutable source shape invalidates the cached copy");
		assertSame(source.getShape(), changedShape.getShape());

		TexturedShape newTheme = cache.resolve(source, new Color(0x8b8b8b));
		assertNotSame(changedShape, newTheme, "a changed theme color rebuilds the cached copy");
	}
}
