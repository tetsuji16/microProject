/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;

import com.microproject.menu.testsupport.UiComponentWalker;

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

	public static AbstractButton findFlatLafTitleButton(JFrame frame, String accessibleName) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		SwingUtilities.invokeAndWait(() -> result[0] = UiComponentWalker.flatten(frame.getRootPane()).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> button.isShowing()
				&& button.getClass().getName().startsWith("com.formdev.flatlaf.ui.FlatTitlePane$")
				&& button.getAccessibleContext() != null
				&& accessibleName.equals(button.getAccessibleContext().getAccessibleName()))
			.findFirst().orElse(null));
		assertTrue(result[0] != null, () -> "FlatLaf native title-bar button is absent: " + accessibleName);
		return result[0];
	}

	public static Rectangle boundsOnScreen(Component component) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	public static void hoverNativeTitleButton(Robot robot, JFrame frame, String accessibleName, int durationMillis)
		throws Exception {
		Rectangle bounds = boundsOnScreen(findFlatLafTitleButton(frame, accessibleName));
		Point target = robotPointFor(bounds);
		robot.mouseMove(target.x, target.y);
		robot.delay(durationMillis);
		robot.waitForIdle();
		captureDesktop(robot, frame, "native-title-button-" + accessibleName + "-hover");
	}

	public static void clickNativeTitleButton(Robot robot, JFrame frame, String accessibleName) throws Exception {
		Rectangle bounds = boundsOnScreen(findFlatLafTitleButton(frame, accessibleName));
		Point target = robotPointFor(bounds);
		robot.mouseMove(target.x, target.y);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	public static Point robotPointFor(Rectangle bounds) {
		double dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0d;
		return new Point((int) Math.round((bounds.x + bounds.width / 2) * dpiScale),
			(int) Math.round((bounds.y + bounds.height / 2) * dpiScale));
	}

	private static void captureDesktop(Robot robot, JFrame frame, String label) throws Exception {
		Rectangle[] screenBounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> screenBounds[0] = frame.getGraphicsConfiguration().getBounds());
		BufferedImage screenshot = robot.createScreenCapture(screenBounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(directory);
		String environment = (System.getProperty("user.language", "unknown") + "-"
			+ System.getProperty("sun.java2d.uiScale", "default")).replaceAll("[^A-Za-z0-9_.-]", "_");
		String artifactName = label + "-" + environment;
		GuiEnvironmentExtension.writeEnvironmentSnapshot(directory, artifactName);
		ImageIO.write(screenshot, "png", directory.resolve(artifactName + ".png").toFile());
	}
}
