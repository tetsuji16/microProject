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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import com.microproject.graphic.configuration.TexturedShape;

/** Caches dark themed arrow copies without mutating user-configured link shapes. */
final class DarkLinkArrowShapeCache {
	private final Map<TexturedShape, Entry> entries = new IdentityHashMap<>();

	TexturedShape resolve(TexturedShape source, Color color) {
		Entry cached = entries.get(source);
		if (cached != null && cached.matches(source, color)) return cached.themed;

		TexturedShape themed = new TexturedShape();
		themed.setShape(source.getShape());
		themed.setColor(color);
		themed.setPaint(color);
		themed.setStroke(source.getStroke());
		themed.setShapeScaleX(source.getShapeScaleX());
		themed.setShapeScaleY(source.getShapeScaleY());
		entries.put(source, new Entry(source, color, themed));
		return themed;
	}

	private record Entry(Color sourceColor, Paint sourcePaint, Object sourceShape, Stroke sourceStroke,
		double sourceScaleX, double sourceScaleY, Color themeColor, TexturedShape themed) {
		private Entry(TexturedShape source, Color themeColor, TexturedShape themed) {
			this(source.getColor(), source.getPaint(), source.getShape(), source.getStroke(),
				source.getShapeScaleX(), source.getShapeScaleY(), themeColor, themed);
		}

		boolean matches(TexturedShape source, Color color) {
			return Objects.equals(sourceColor, source.getColor()) && sourcePaint == source.getPaint()
				&& sourceShape == source.getShape() && sourceStroke == source.getStroke()
				&& sourceScaleX == source.getShapeScaleX() && sourceScaleY == source.getShapeScaleY()
				&& themeColor.equals(color);
		}
	}
}
