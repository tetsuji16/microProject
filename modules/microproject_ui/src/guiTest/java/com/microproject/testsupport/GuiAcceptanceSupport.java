/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.KeyboardFocusManager;
import java.awt.event.PaintEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
		AtomicBoolean paintCompleted = new AtomicBoolean();
		AtomicBoolean repaintRequested = new AtomicBoolean();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		AWTEventListener paintListener = event -> {
			if (!(event instanceof PaintEvent) || !(event.getSource() instanceof Component painted)
					|| !(painted == window || SwingUtilities.isDescendingFrom(painted, window))
					|| !repaintRequested.get() || !painted.isShowing())
				return;
			// AWT notifies listeners before dispatching the paint event. Queue a
			// sentinel so success means that dispatch (and its native paint) finished.
			EventQueue.invokeLater(() -> paintCompleted.set(true));
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(paintListener, AWTEvent.PAINT_EVENT_MASK);
		try {
			EventQueue.invokeLater(() -> {
				window.validate();
				visibleContent.repaint();
				window.repaint();
				repaintRequested.set(true);
			});
			Toolkit.getDefaultToolkit().sync();
			FutureTask<WindowRenderSnapshot> snapshotTask = null;
			WindowRenderSnapshot snapshot = null;
			while (System.nanoTime() < deadline) {
				if (snapshotTask == null) {
					snapshotTask = new FutureTask<>(() -> inspectWindow(window, visibleContent, paintCompleted.get()));
					EventQueue.invokeLater(snapshotTask);
				}
				try {
					snapshot = snapshotTask.get(25, TimeUnit.MILLISECONDS);
				} catch (TimeoutException waitingForEdt) {
					continue;
				} catch (java.util.concurrent.ExecutionException failure) {
					throw new AssertionError(description + " readiness inspection failed", failure.getCause());
				}
				if (snapshot.ready() && System.nanoTime() < deadline) break;
				snapshotTask = null;
			}
			if (snapshot == null || !snapshot.ready() || System.nanoTime() >= deadline)
				throw new AssertionError(description + " did not become visible, rendered, and foreground-ready");
			Toolkit.getDefaultToolkit().sync();
			assertTrue(snapshot.windowBounds().contains(snapshot.contentBounds()),
				description + " content must fit within its native window bounds");
			BufferedImage screenshot = robot.createScreenCapture(snapshot.contentBounds());
			assertTrue(screenshot.getWidth() > 0 && screenshot.getHeight() > 0,
				description + " must have a non-empty on-screen content capture");
			assertTrue(hasRenderedVariation(screenshot), description + " content capture must show rendered UI pixels");
		} finally {
			Toolkit.getDefaultToolkit().removeAWTEventListener(paintListener);
		}
	}

	private static WindowRenderSnapshot inspectWindow(Window window, Component visibleContent, boolean paintCompleted) {
		if (!window.isShowing() || !visibleContent.isShowing())
			return new WindowRenderSnapshot(false, new Rectangle(), new Rectangle());
		Rectangle windowBounds = window.getBounds();
		Rectangle contentBounds = new Rectangle(visibleContent.getLocationOnScreen(), visibleContent.getSize());
		java.awt.Point windowLocation = window.getLocationOnScreen();
		windowBounds.setLocation(windowLocation);
		boolean ready = window.isShowing() && window.isValid() && hasArea(window.getSize())
			&& visibleContent.isShowing() && visibleContent.isValid() && hasArea(visibleContent.getSize())
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() == window
			&& window.isActive() && window.isFocused() && paintCompleted;
		return new WindowRenderSnapshot(ready, windowBounds, contentBounds);
	}

	private static boolean hasArea(java.awt.Dimension size) {
		return size != null && size.width > 0 && size.height > 0;
	}

	private record WindowRenderSnapshot(boolean ready, Rectangle windowBounds, Rectangle contentBounds) { }

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
