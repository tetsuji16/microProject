/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/** Keeps newly shown document windows reachable inside their monitor's work area. */
public final class WindowBoundsSupport {
	private WindowBoundsSupport() {
	}

	/**
	 * Shrinks and moves a normal window when its current bounds extend outside
	 * the usable area of its current monitor. Maximized windows remain owned by
	 * the native window manager.
	 */
	public static void fitWithinUsableScreen(Window window) {
		if (window == null || !window.isShowing() || isMaximized(window)) {
			return;
		}
		GraphicsConfiguration configuration = window.getGraphicsConfiguration();
		if (configuration == null) {
			return;
		}

		Rectangle usable = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		usable.x += insets.left;
		usable.y += insets.top;
		usable.width -= insets.left + insets.right;
		usable.height -= insets.top + insets.bottom;
		if (usable.width <= 0 || usable.height <= 0) {
			return;
		}

		Rectangle fitted = fittedBounds(window.getBounds(), usable);
		if (!fitted.equals(window.getBounds())) {
			window.setBounds(fitted);
		}
	}

	static Rectangle fittedBounds(Rectangle bounds, Rectangle usable) {
		Rectangle fitted = new Rectangle(bounds);
		fitted.width = Math.min(fitted.width, usable.width);
		fitted.height = Math.min(fitted.height, usable.height);
		fitted.x = Math.max(usable.x, Math.min(fitted.x, usable.x + usable.width - fitted.width));
		fitted.y = Math.max(usable.y, Math.min(fitted.y, usable.y + usable.height - fitted.height));
		return fitted;
	}

	private static boolean isMaximized(Window window) {
		return window instanceof Frame frame && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
	}
}
