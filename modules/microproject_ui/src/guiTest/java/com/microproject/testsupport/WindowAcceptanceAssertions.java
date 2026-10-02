/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Shared native-window geometry assertions for physical Robot acceptance journeys. */
public final class WindowAcceptanceAssertions {
	private WindowAcceptanceAssertions() {
	}

	public static void assertWithinUsableWorkArea(JFrame frame, String description) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicsConfiguration configuration = frame.getGraphicsConfiguration();
			assertTrue(configuration != null, description + " must have a graphics configuration");
			Rectangle screen = configuration.getBounds();
			Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
			Rectangle usable = new Rectangle(screen.x + insets.left, screen.y + insets.top,
				screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom);
			Rectangle bounds = frame.getBounds();
			assertTrue(usable.contains(bounds), () -> description + " must fit the usable work area; frame=" + bounds
				+ ", usable=" + usable + ", screen=" + screen + ", insets=" + insets);
		});
	}
}
