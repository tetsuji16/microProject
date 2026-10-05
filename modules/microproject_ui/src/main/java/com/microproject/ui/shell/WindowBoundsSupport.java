/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Timer;

/** Keeps windows reachable inside their monitor's usable work area. */
public final class WindowBoundsSupport {
	private WindowBoundsSupport() {
	}

	/**
	 * Shrinks and moves a normal window when its current bounds extend outside
	 * the usable area of its current monitor. Maximized windows remain owned by
	 * the native window manager.
	 */
	public static void fitWithinUsableScreen(Window window) {
		if (window == null || GraphicsEnvironment.isHeadless() || isMaximized(window)) {
			return;
		}
		installResizeGuard(window);
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
			// AbstractDialog locks its packed size as a minimum. Lower that floor
			// when the monitor cannot fit the packed dialog, otherwise AWT ignores
			// the smaller bounds and leaves scrollable content clipped off-screen.
			Dimension minimum = window.getMinimumSize();
			if (minimum.width > fitted.width || minimum.height > fitted.height) {
				window.setMinimumSize(new Dimension(Math.min(minimum.width, fitted.width),
					Math.min(minimum.height, fitted.height)));
			}
			window.setBounds(fitted);
		}
	}

	private static void installResizeGuard(Window window) {
		for (var listener : window.getComponentListeners()) {
			if (listener instanceof UsableBoundsGuard)
				return;
		}
		window.addComponentListener(new UsableBoundsGuard(window));
	}

	private static final class UsableBoundsGuard extends ComponentAdapter {
		private static final int SETTLE_DELAY_MILLIS = 160;
		private final Window window;
		private final Timer settleTimer;

		private UsableBoundsGuard(Window window) {
			this.window = window;
			settleTimer = new Timer(SETTLE_DELAY_MILLIS,
				ignored -> WindowBoundsSupport.fitWithinUsableScreen(this.window));
			settleTimer.setRepeats(false);
		}

		@Override
		public void componentResized(ComponentEvent event) {
			// Native maximize/restore transitions may publish temporary full-work-area
			// bounds; clamp only after the window manager's geometry has settled.
			settleTimer.restart();
		}

		@Override
		public void componentMoved(ComponentEvent event) {
			settleTimer.restart();
		}
	}

	public static Rectangle fittedBounds(Rectangle bounds, Rectangle usable) {
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
