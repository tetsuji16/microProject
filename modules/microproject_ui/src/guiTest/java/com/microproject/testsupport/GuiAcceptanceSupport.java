/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

/** Shared deterministic waits for non-headless Swing acceptance tests. */
public final class GuiAcceptanceSupport {
	private GuiAcceptanceSupport() {
	}

	public static void await(BooleanSupplier condition, String message) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) return;
			Thread.sleep(25);
		}
		assertTrue(false, message);
	}

	/**
	 * Waits for an already-activated application window to paint real on-screen
	 * content before a physical command is sent. Call after the Robot activation
	 * route; this keeps the command's own existing readiness deadline unchanged.
	 */
	public static void awaitRenderedWindow(Window window, Component visibleContent, Robot robot, String description)
		throws Exception {
		if (window == null || visibleContent == null || robot == null)
			throw new IllegalArgumentException("Window, visible content, and Robot are required");
		if (!(visibleContent instanceof javax.swing.JComponent swingContent))
			throw new IllegalArgumentException("Visible content must be a Swing component");
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		FutureTask<Boolean> paintTask = new FutureTask<>(() -> {
			window.validate();
			if (!window.isShowing() || !swingContent.isShowing() || !hasArea(swingContent.getSize()))
				return false;
			// Swing repaint requests can be coalesced without a corresponding AWT
			// PaintEvent. paintImmediately paints this visible component to its real
			// onscreen peer; the completed FutureTask is the EDT completion sentinel.
			swingContent.paintImmediately(0, 0, swingContent.getWidth(), swingContent.getHeight());
			return true;
		});
		EventQueue.invokeLater(paintTask);
		while (System.nanoTime() < deadline && !paintTask.isDone()) {
			try {
				paintTask.get(25, TimeUnit.MILLISECONDS);
			} catch (TimeoutException waitingForEdt) {
				// Keep waiting for the actual EDT paint, bounded by the shared deadline.
			} catch (ExecutionException failure) {
				throw new AssertionError(description + " onscreen paint failed", failure.getCause());
			}
		}
		if (!paintTask.isDone()) paintTask.cancel(false);
		boolean paintCompleted = false;
		if (paintTask.isDone() && !paintTask.isCancelled()) {
			try {
				paintCompleted = paintTask.get();
			} catch (ExecutionException failure) {
				throw new AssertionError(description + " onscreen paint failed", failure.getCause());
			}
		}
		final boolean onscreenPaintCompleted = paintCompleted;
		Toolkit.getDefaultToolkit().sync();
		FutureTask<WindowRenderSnapshot> snapshotTask = null;
		WindowRenderSnapshot snapshot = null;
		while (System.nanoTime() < deadline) {
			if (snapshotTask == null) {
				snapshotTask = new FutureTask<>(() -> inspectWindow(window, visibleContent, onscreenPaintCompleted));
				EventQueue.invokeLater(snapshotTask);
			}
			try {
				snapshot = snapshotTask.get(25, TimeUnit.MILLISECONDS);
			} catch (TimeoutException waitingForEdt) {
				continue;
			} catch (ExecutionException failure) {
				throw new AssertionError(description + " readiness inspection failed", failure.getCause());
			}
			if (snapshot.ready() && System.nanoTime() < deadline) break;
			snapshotTask = null;
		}
		if (snapshot == null || !snapshot.ready() || System.nanoTime() >= deadline)
			throw new AssertionError(description + " did not become visible, rendered, and foreground-ready: paintCompleted="
				+ paintCompleted + ", lastSnapshot=" + snapshot + ", remainingMs="
				+ TimeUnit.NANOSECONDS.toMillis(Math.max(0L, deadline - System.nanoTime())));
		Toolkit.getDefaultToolkit().sync();
		assertTrue(snapshot.windowBounds().contains(snapshot.contentBounds()),
			description + " content must fit within its native window bounds");
		BufferedImage screenshot = robot.createScreenCapture(snapshot.contentBounds());
		assertTrue(screenshot.getWidth() > 0 && screenshot.getHeight() > 0,
			description + " must have a non-empty on-screen content capture");
		assertTrue(hasRenderedVariation(screenshot), description + " content capture must show rendered UI pixels");
		assertTrue(System.nanoTime() < deadline, description + " rendered readiness exceeded its existing deadline");
	}

	private static WindowRenderSnapshot inspectWindow(Window window, Component visibleContent, boolean paintCompleted) {
		if (!window.isShowing() || !visibleContent.isShowing())
			return new WindowRenderSnapshot(false, window.isShowing(), visibleContent.isShowing(), window.isValid(),
				visibleContent.isValid(), false, false, paintCompleted, new Rectangle(), new Rectangle());
		Rectangle windowBounds = window.getBounds();
		Rectangle contentBounds = new Rectangle(visibleContent.getLocationOnScreen(), visibleContent.getSize());
		java.awt.Point windowLocation = window.getLocationOnScreen();
		windowBounds.setLocation(windowLocation);
		boolean showing = window.isShowing() && visibleContent.isShowing();
		boolean valid = window.isValid() && visibleContent.isValid();
		boolean sized = hasArea(window.getSize()) && hasArea(visibleContent.getSize());
		boolean active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() == window
			&& window.isActive() && window.isFocused();
		boolean ready = showing && valid && sized && active && paintCompleted;
		return new WindowRenderSnapshot(ready, showing, visibleContent.isShowing(), valid, visibleContent.isValid(),
			sized, active, paintCompleted, windowBounds, contentBounds);
	}

	private static boolean hasArea(java.awt.Dimension size) {
		return size != null && size.width > 0 && size.height > 0;
	}

	private record WindowRenderSnapshot(boolean ready, boolean showing, boolean contentShowing, boolean valid,
		boolean contentValid, boolean sized, boolean active, boolean paintCompleted, Rectangle windowBounds,
		Rectangle contentBounds) { }

	private static boolean hasRenderedVariation(BufferedImage image) {
		int first = image.getRGB(0, 0);
		int stepX = Math.max(1, image.getWidth() / 32);
		int stepY = Math.max(1, image.getHeight() / 32);
		for (int y = 0; y < image.getHeight(); y += stepY) {
			for (int x = 0; x < image.getWidth(); x += stepX) {
				if (image.getRGB(x, y) != first) return true;
			}
		}
		return false;
	}

	/** Selects a combo-box item through its physical dropdown and keyboard route. */
	public static void selectComboBoxItem(Robot robot, JComboBox<?> combo, int index) throws Exception {
		if (combo == null || index < 0 || index >= combo.getItemCount())
			throw new AssertionError("Invalid physical combo-box selection index " + index);
		WindowAcceptanceAssertions.clickComponent(robot, combo);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		for (int current = 0; current < index; current++) {
			robot.keyPress(KeyEvent.VK_DOWN);
			robot.keyRelease(KeyEvent.VK_DOWN);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
	}

	/**
	 * Runs Swing cleanup without allowing a native-window/EDT deadlock to hang a
	 * whole GUI test worker forever.  The action is still executed on the EDT;
	 * the timeout only bounds the caller's wait and leaves the failure observable.
	 */
	public static void runOnEdtWithTimeout(Runnable action, String description) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		FutureTask<Void> task = new FutureTask<>(() -> {
			action.run();
			return null;
		});
		SwingUtilities.invokeLater(task);
		try {
			task.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException exception) {
			task.cancel(true);
			throw new AssertionError(description + " timed out on the EDT", exception);
		} catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof Exception checked) throw checked;
			if (cause instanceof Error error) throw error;
			throw new AssertionError(description + " failed on the EDT", cause);
		}
	}
}
