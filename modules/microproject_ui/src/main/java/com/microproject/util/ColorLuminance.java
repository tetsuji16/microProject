/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.awt.Color;

/**
 * Shared colour maths: WCAG relative luminance, the single light/dark
 * classification and the single RGB mix used by theme and palette code.
 */
public final class ColorLuminance {
	private ColorLuminance() {
	}

	/**
	 * Relative luminance at which black and white text have equal WCAG contrast;
	 * colours at or below it are dark (white text reads better on them).
	 */
	public static final double DARK_LUMINANCE_THRESHOLD = 0.179;

	/** The one dark/light classification: a null colour is not dark. */
	public static boolean isDark(Color color) {
		return color != null && relativeLuminance(color) <= DARK_LUMINANCE_THRESHOLD;
	}

	/**
	 * Returns {@code base * (1 - targetWeight) + target * targetWeight} per
	 * channel (opaque result, weight clamped to 0..1, float arithmetic).
	 */
	public static Color mix(Color base, Color target, float targetWeight) {
		float weight = Math.max(0f, Math.min(1f, targetWeight));
		float baseWeight = 1f - weight;
		int red = Math.round(base.getRed() * baseWeight + target.getRed() * weight);
		int green = Math.round(base.getGreen() * baseWeight + target.getGreen() * weight);
		int blue = Math.round(base.getBlue() * baseWeight + target.getBlue() * weight);
		return new Color(red, green, blue);
	}

	public static double relativeLuminance(Color color) {
		return 0.2126 * linearize(color.getRed() / 255.0)
				+ 0.7152 * linearize(color.getGreen() / 255.0)
				+ 0.0722 * linearize(color.getBlue() / 255.0);
	}

	private static double linearize(double channel) {
		return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
	}
}
