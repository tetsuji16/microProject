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
import java.awt.geom.AffineTransform;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

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
		if (failure instanceof TestAbortedException) throw failure;
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
		writeEnvironmentSnapshot(directory, safeName);
		if (GraphicsEnvironment.isHeadless()) return;
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

	static void writeEnvironmentSnapshot(Path directory, String safeName) throws Exception {
		Files.createDirectories(directory);
		StringBuilder environment = new StringBuilder()
			.append("os.name=").append(System.getProperty("os.name")).append(System.lineSeparator())
			.append("os.version=").append(System.getProperty("os.version")).append(System.lineSeparator())
			.append("os.kernelVersion=").append(windowsKernelVersion()).append(System.lineSeparator())
			.append("os.arch=").append(System.getProperty("os.arch")).append(System.lineSeparator())
			.append("java.version=").append(System.getProperty("java.version")).append(System.lineSeparator())
			.append("locale.default=").append(Locale.getDefault().toLanguageTag()).append(System.lineSeparator())
			.append("locale.language=").append(System.getProperty("user.language")).append(System.lineSeparator())
			.append("locale.country=").append(System.getProperty("user.country")).append(System.lineSeparator())
			.append("java2d.uiScale=").append(System.getProperty("sun.java2d.uiScale", "default"))
				.append(System.lineSeparator());
		if (!GraphicsEnvironment.isHeadless()) {
			for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
				var configuration = device.getDefaultConfiguration();
				Rectangle bounds = configuration.getBounds();
				java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
				Rectangle usable = new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
					bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
				AffineTransform transform = configuration.getDefaultTransform();
				environment.append("screen=").append(device.getIDstring())
					.append(" bounds=").append(bounds)
					.append(" insets=").append(insets)
					.append(" usable=").append(usable)
					.append(" scale=").append(transform.getScaleX()).append('x').append(transform.getScaleY())
					.append(System.lineSeparator());
			}
		}
		Files.writeString(directory.resolve(safeName + ".environment.txt"), environment.toString());
	}

	private static String windowsKernelVersion() {
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) return "not-applicable";
		Process process = null;
		try {
			process = new ProcessBuilder("cmd.exe", "/c", "ver").redirectErrorStream(true).start();
			if (!process.waitFor(3, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return "query timed out";
			}
			String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.Charset.defaultCharset())
				.replace('\r', ' ').replace('\n', ' ').trim();
			return process.exitValue() == 0 ? output : "query failed: " + output;
		} catch (Exception exception) {
			if (process != null) process.destroyForcibly();
			return "query failed: " + exception.getClass().getSimpleName();
		}
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
