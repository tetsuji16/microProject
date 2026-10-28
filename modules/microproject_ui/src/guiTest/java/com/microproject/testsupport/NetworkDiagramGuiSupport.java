/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Shared physical helpers for WBS and PERT diagram interaction journeys. */
public final class NetworkDiagramGuiSupport {
	private NetworkDiagramGuiSupport() { }

	public static <T> T onEdt(Supplier<T> action) {
		if (SwingUtilities.isEventDispatchThread()) return action.get();
		AtomicReference<T> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> result.set(action.get()));
		} catch (Exception failure) {
			throw new IllegalStateException("Swing state read failed on the EDT", failure);
		}
		return result.get();
	}

	public static Rectangle screenBounds(Component component) {
		return onEdt(() -> {
			Point location = component.getLocationOnScreen();
			return new Rectangle(location.x, location.y, component.getWidth(), component.getHeight());
		});
	}

	public static Point onScreen(Component component) {
		return onEdt(component::getLocationOnScreen);
	}

	public static void click(Robot robot, Rectangle bounds) {
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	public static void capture(Robot robot, Component component, String name) throws Exception {
		Rectangle bounds = screenBounds(component);
		Path output = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"),
			"issue-748", name);
		Files.createDirectories(output.getParent());
		BufferedImage image = robot.createScreenCapture(bounds);
		ImageIO.write(image, "png", output.toFile());
	}
}
