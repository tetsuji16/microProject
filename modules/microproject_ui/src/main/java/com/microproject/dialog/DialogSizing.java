/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.SwingUtilities;

/**
 * Sizes large tool dialogs so fixed pixel dimensions never exceed the usable
 * area of the owner's monitor (for example at 150% display scaling).
 */
public final class DialogSizing {
	/** Space left between a clamped dialog and the screen edge. */
	public static final int SCREEN_MARGIN = 24;

	private DialogSizing() {
	}

	/**
	 * Applies a minimum and an initial size clamped to the owner's screen and
	 * centers the dialog on the owner window.
	 */
	public static void applyInitialSize(Window window, Dimension min, Dimension preferred, Component owner) {
		Rectangle usable = GraphicsEnvironment.isHeadless() ? null : usableBounds(owner, window);
		Dimension minimum = usable == null ? new Dimension(min) : clamp(min, usable, SCREEN_MARGIN);
		Dimension size = usable == null ? new Dimension(preferred) : clamp(preferred, usable, SCREEN_MARGIN);
		size.width = Math.max(size.width, minimum.width);
		size.height = Math.max(size.height, minimum.height);
		window.setMinimumSize(minimum);
		window.setSize(size);
		window.setLocationRelativeTo(owner instanceof Window || owner == null ? owner : SwingUtilities.getWindowAncestor(owner));
	}

	/** Limits {@code requested} to the usable area minus a margin on every side. */
	public static Dimension clamp(Dimension requested, Rectangle usable, int margin) {
		int maxWidth = Math.max(1, usable.width - 2 * margin);
		int maxHeight = Math.max(1, usable.height - 2 * margin);
		return new Dimension(Math.min(requested.width, maxWidth), Math.min(requested.height, maxHeight));
	}

	private static Rectangle usableBounds(Component owner, Window window) {
		GraphicsConfiguration configuration = owner == null ? null : owner.getGraphicsConfiguration();
		if (configuration == null)
			configuration = window.getGraphicsConfiguration();
		if (configuration == null)
			return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Rectangle usable = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		usable.x += insets.left;
		usable.y += insets.top;
		usable.width -= insets.left + insets.right;
		usable.height -= insets.top + insets.bottom;
		return usable;
	}
}
