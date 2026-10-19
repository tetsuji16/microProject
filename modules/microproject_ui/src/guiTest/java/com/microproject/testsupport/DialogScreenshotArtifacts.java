/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Writes reviewable screenshots of visible dialog surfaces under the GUI artifact directory. */
public final class DialogScreenshotArtifacts {
	private DialogScreenshotArtifacts() { }

	public static Path capture(Dialog dialog, String name) throws Exception {
		GuiAcceptanceSupport.await(() -> isForeground(dialog),
			name + " dialog must own the active window before screenshot capture");
		Robot robot = new Robot();
		robot.waitForIdle();
		robot.delay(200);
		robot.waitForIdle();
		assertTrue(isForeground(dialog), name + " dialog lost foreground before screenshot capture");
		Rectangle[] bounds = new Rectangle[1];
		String[] title = new String[1];
		BufferedImage[] renderedDialog = new BufferedImage[1];
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(isForeground(dialog), name + " dialog must be active and visible before capture");
			title[0] = dialog.getTitle();
			bounds[0] = new Rectangle(dialog.getLocationOnScreen(), dialog.getSize());
			renderedDialog[0] = new BufferedImage(dialog.getWidth(), dialog.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = renderedDialog[0].createGraphics();
			dialog.printAll(graphics);
			graphics.dispose();
		});
		assertTrue(title[0] != null && !title[0].isBlank(), name + " dialog must have a visible title");
		assertTrue(bounds[0].width > 0 && bounds[0].height > 0, name + " dialog must have non-empty bounds");
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		String environment = (System.getProperty("user.language", "unknown") + "-"
			+ System.getProperty("sun.java2d.uiScale", "default")).replaceAll("[^A-Za-z0-9_.-]", "_");
		Path artifact = directory.resolve(name + "-" + environment + ".png");
		assertTrue(ImageIO.write(image, "png", artifact.toFile()), "PNG writer is unavailable for " + artifact);
		int matchingPixels = countSimilarPixels(image, renderedDialog[0]);
		assertTrue(matchingPixels >= image.getWidth() * image.getHeight() / 5,
			() -> name + " screenshot does not contain the rendered dialog body; bounds=" + bounds[0]
				+ " matchingPixels=" + matchingPixels
				+ " image=" + image.getWidth() + "x" + image.getHeight());
		return artifact;
	}

	private static boolean isForeground(Dialog dialog) {
		return dialog.isShowing() && dialog.isActive()
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() == dialog;
	}

	private static int countSimilarPixels(BufferedImage image, BufferedImage expected) {
		int count = 0;
		int maxY = Math.min(image.getHeight(), expected.getHeight());
		int maxX = Math.min(image.getWidth(), expected.getWidth());
		for (int y = maxY / 10; y < maxY; y++) {
			for (int x = 0; x < maxX; x++) {
				int pixel = image.getRGB(x, y);
				int expectedPixel = expected.getRGB(x, y);
				if (Math.abs(((pixel >>> 16) & 0xff) - ((expectedPixel >>> 16) & 0xff)) <= 48
						&& Math.abs(((pixel >>> 8) & 0xff) - ((expectedPixel >>> 8) & 0xff)) <= 48
						&& Math.abs((pixel & 0xff) - (expectedPixel & 0xff)) <= 48)
					count++;
			}
		}
		return count;
	}
}
