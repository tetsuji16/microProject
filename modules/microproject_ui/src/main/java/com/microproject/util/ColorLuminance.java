/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.awt.Color;

/** WCAG relative luminance, shared by the callers that pick readable text colours. */
public final class ColorLuminance {
	private ColorLuminance() {
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
