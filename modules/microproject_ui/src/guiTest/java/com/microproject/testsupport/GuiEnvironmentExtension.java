/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.awt.KeyboardFocusManager;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/** Shared last-resort desktop cleanup for every GUI acceptance fixture. */
public final class GuiEnvironmentExtension implements BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		cleanupDesktop("GUI fixture pre-test cleanup");
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		cleanupDesktop("GUI fixture desktop cleanup");
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable {
		try {
			captureFailure(context);
		} catch (Throwable captureFailure) {
			failure.addSuppressed(captureFailure);
		}
		throw failure;
	}

	private static void captureFailure(ExtensionContext context) throws Exception {
		String configuredDirectory = System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts");
		Path directory = Path.of(configuredDirectory);
		Files.createDirectories(directory);
		String safeName = (context.getRequiredTestClass().getSimpleName() + "-" + context.getDisplayName())
			.replaceAll("[^A-Za-z0-9_.-]", "_");
		Rectangle desktop = null;
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			Rectangle bounds = device.getDefaultConfiguration().getBounds();
			desktop = desktop == null ? new Rectangle(bounds) : desktop.union(bounds);
		}
		if (desktop != null && desktop.width > 0 && desktop.height > 0) {
			BufferedImage screenshot = new Robot().createScreenCapture(desktop);
			ImageIO.write(screenshot, "png", directory.resolve(safeName + ".failure.png").toFile());
		}
		StringBuilder windows = new StringBuilder();
		for (Window window : Window.getWindows()) {
			windows.append(window.getClass().getName())
				.append(" showing=").append(window.isShowing())
				.append(" displayable=").append(window.isDisplayable())
				.append(" bounds=").append(window.getBounds())
				.append(" title=").append(window instanceof java.awt.Frame frame ? frame.getTitle() : "")
				.append(System.lineSeparator());
		}
		Files.writeString(directory.resolve(safeName + ".windows.txt"), windows.toString());
	}

	private static void cleanupDesktop(String description) throws Exception {
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isDisplayable()) window.dispose();
			}
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
			Toolkit.getDefaultToolkit().sync();
		}, description);
	}
}
