/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

	/** Drains Robot input and then the AWT queue under one bounded deadline. */
	public static void awaitInputAndEventQueueIdle(Robot robot, String description) throws Exception {
		if (robot == null) throw new IllegalArgumentException("Robot is required");
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		FutureTask<Void> robotIdleTask = new FutureTask<>(() -> {
			robot.waitForIdle();
			return null;
		});
		Thread robotIdleWaiter = new Thread(robotIdleTask, "gui-acceptance-robot-idle");
		robotIdleWaiter.setDaemon(true);
		robotIdleWaiter.start();
		try {
			awaitTask(robotIdleTask, deadline, description + " Robot input drain");
		} catch (Exception | Error failure) {
			robotIdleTask.cancel(false);
			throw failure;
		}
		FutureTask<Void> eventQueueBarrier = new FutureTask<>(() -> null);
		EventQueue.invokeLater(eventQueueBarrier);
		awaitTask(eventQueueBarrier, deadline, description + " AWT event-queue drain");
	}

	/** Finds a visible button by its semantic label, including the LAF mnemonic suffix. */
	public static AbstractButton findShowingButtonByText(Component root, String expectedText) throws Exception {
		if (root == null || expectedText == null)
			throw new IllegalArgumentException("A component root and expected button label are required");
		AbstractButton[] result = new AbstractButton[1];
		Runnable lookup = () -> result[0] = findShowingButton(root, expectedText);
		if (EventQueue.isDispatchThread()) lookup.run();
		else SwingUtilities.invokeAndWait(lookup);
		if (result[0] == null)
			throw new AssertionError("Showing button not found for semantic label: " + expectedText);
		return result[0];
	}

	private static AbstractButton findShowingButton(Component component, String expectedText) {
		if (component instanceof AbstractButton button && button.isShowing()
			&& hasButtonLabel(button, expectedText)) return button;
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				AbstractButton found = findShowingButton(child, expectedText);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static boolean hasButtonLabel(AbstractButton button, String expectedText) {
		String actualText = button.getText();
		if (expectedText.equals(actualText)) return true;
		int mnemonic = button.getMnemonic();
		return mnemonic != 0 && (expectedText + " (" + KeyEvent.getKeyText(mnemonic) + ")").equals(actualText);
	}

	/**
	 * Physically activates an application window, waits for real on-screen content,
	 * and checks focus and captured pixels before the next physical command.
	 */
	public static void activateAndAwaitRenderedWindow(Window window, Component visibleContent, Robot robot,
		boolean alwaysOnTop, String description) throws Exception {
		if (window == null || visibleContent == null || robot == null)
			throw new IllegalArgumentException("Window, visible content, and Robot are required");
		if (!(visibleContent instanceof javax.swing.JComponent))
			throw new IllegalArgumentException("Visible content must be a Swing component");
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		FutureTask<Point> activationPointTask = new FutureTask<>(() -> {
			window.toFront();
			window.requestFocus();
			window.setAlwaysOnTop(alwaysOnTop);
			return captionActivationPoint(window);
		});
		EventQueue.invokeLater(activationPointTask);
		Point activationPoint = awaitTask(activationPointTask, deadline, description + " activation setup");
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyRelease(KeyEvent.VK_ALT);
		robot.mouseMove(activationPoint.x, activationPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		if (System.nanoTime() >= deadline)
			throw new AssertionError(description + " physical activation exceeded its existing deadline");
		awaitRenderedWindow(window, visibleContent, robot, description, deadline);
	}

	private static Point captionActivationPoint(Window window) {
		List<JComponent> captionTargets = new ArrayList<>();
		collectCaptionTargets(window, captionTargets);
		JComponent target = captionTargets.stream()
			.filter(Component::isShowing)
			.filter(component -> component.getComponentCount() == 0)
			.filter(component -> !component.isFocusable())
			.filter(component -> component.getWidth() > 0 && component.getHeight() > 0)
			.max(Comparator.comparingLong(component -> (long) component.getWidth() * component.getHeight()))
			.orElse(null);
		if (target != null) {
			Point location = target.getLocationOnScreen();
			Point windowLocation = window.getLocationOnScreen();
			Point activationPoint = new Point(location.x + target.getWidth() / 2,
				location.y + target.getHeight() / 2);
			Component hitTarget = SwingUtilities.getDeepestComponentAt(window,
				activationPoint.x - windowLocation.x, activationPoint.y - windowLocation.y);
			if (hitTarget == target) return activationPoint;
			throw new AssertionError("Semantic title-bar caption point did not resolve to its marked component");
		}
		java.awt.Insets insets = window.getInsets();
		if (insets.top <= 0)
			throw new AssertionError("Window has no visible semantic caption target or native title-bar inset");
		Point location = window.getLocationOnScreen();
		int rightCaptionLimit = Math.max(1, window.getWidth() - insets.right - 24);
		int leftCaptionOffset = Math.min(Math.max(40, insets.left + 24), rightCaptionLimit);
		return new Point(location.x + leftCaptionOffset, location.y + Math.max(1, insets.top / 2));
	}

	private static void collectCaptionTargets(Container parent, List<JComponent> targets) {
		for (Component child : parent.getComponents()) {
			if (child instanceof JComponent swingChild
				&& Boolean.TRUE.equals(swingChild.getClientProperty("JComponent.titleBarCaption")))
				targets.add(swingChild);
			if (child instanceof Container container) collectCaptionTargets(container, targets);
		}
	}

	private static void awaitRenderedWindow(Window window, Component visibleContent, Robot robot, String description,
		long deadline) throws Exception {
		javax.swing.JComponent swingContent = (javax.swing.JComponent) visibleContent;
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
		while (System.nanoTime() < deadline && !paintTask.isDone()) waitForTask(paintTask, deadline);
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
			snapshot = awaitTask(snapshotTask, deadline, description + " readiness inspection");
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

	private static void waitForTask(FutureTask<?> task, long deadline) throws Exception {
		long remaining = deadline - System.nanoTime();
		if (remaining <= 0) return;
		try {
			task.get(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(25)), TimeUnit.NANOSECONDS);
		} catch (TimeoutException waitingForEdt) {
			// Keep waiting for the current bounded EDT operation.
		} catch (ExecutionException failure) {
			throw new AssertionError("Swing readiness EDT operation failed", failure.getCause());
		}
	}

	private static <T> T awaitTask(FutureTask<T> task, long deadline, String description) throws Exception {
		while (System.nanoTime() < deadline) {
			long remaining = deadline - System.nanoTime();
			try {
				return task.get(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(25)), TimeUnit.NANOSECONDS);
			} catch (TimeoutException waitingForEdt) {
				// Keep waiting until the shared readiness deadline.
			} catch (ExecutionException failure) {
				throw new AssertionError(description + " failed", failure.getCause());
			}
		}
		throw new AssertionError(description + " exceeded the existing readiness deadline");
	}

	private static WindowRenderSnapshot inspectWindow(Window window, Component visibleContent, boolean paintCompleted) {
		boolean windowShowing = window.isShowing();
		boolean contentShowing = visibleContent.isShowing();
		boolean windowValid = window.isValid();
		boolean contentValid = visibleContent.isValid();
		Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		boolean activeWindowMatches = activeWindow == window;
		boolean windowActive = window.isActive();
		boolean windowFocused = window.isFocused();
		String activeWindowIdentity = windowIdentity(activeWindow);
		String visibleModalBlockers = java.util.Arrays.stream(Window.getWindows())
			.filter(candidate -> candidate != window && candidate.isShowing())
			.filter(candidate -> candidate instanceof java.awt.Dialog dialog && dialog.isModal())
			.map(GuiAcceptanceSupport::windowIdentity)
			.collect(java.util.stream.Collectors.joining(","));
		if (!windowShowing || !contentShowing)
			return new WindowRenderSnapshot(false, windowShowing, contentShowing, windowValid, contentValid, false,
				activeWindowMatches, windowActive, windowFocused, activeWindowIdentity, visibleModalBlockers,
				paintCompleted, new Rectangle(), new Rectangle());
		Rectangle windowBounds = window.getBounds();
		Rectangle contentBounds = new Rectangle(visibleContent.getLocationOnScreen(), visibleContent.getSize());
		java.awt.Point windowLocation = window.getLocationOnScreen();
		windowBounds.setLocation(windowLocation);
		boolean sized = hasArea(window.getSize()) && hasArea(visibleContent.getSize());
		boolean active = activeWindowMatches && windowActive && windowFocused;
		boolean ready = windowShowing && contentShowing && contentValid && sized && active && paintCompleted;
		return new WindowRenderSnapshot(ready, windowShowing, contentShowing, windowValid, contentValid, sized,
			activeWindowMatches, windowActive, windowFocused, activeWindowIdentity, visibleModalBlockers,
			paintCompleted, windowBounds, contentBounds);
	}

	private static String windowIdentity(Window window) {
		return window == null ? "none" : window.getClass().getName() + "@"
			+ Integer.toHexString(System.identityHashCode(window));
	}

	private static boolean hasArea(java.awt.Dimension size) {
		return size != null && size.width > 0 && size.height > 0;
	}

	private record WindowRenderSnapshot(boolean ready, boolean showing, boolean contentShowing, boolean windowValid,
		boolean contentValid, boolean sized, boolean activeWindowMatches, boolean windowActive, boolean windowFocused,
		String activeWindowIdentity, String visibleModalBlockers, boolean paintCompleted, Rectangle windowBounds,
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
		// Click the JComboBox itself so it owns keyboard focus while its popup is
		// open. Clicking the arrow child can open the popup without transferring
		// focus from a previously active combo, causing HOME/DOWN to target that
		// other component even though this popup is visible.
		WindowAcceptanceAssertions.clickComponent(robot, combo);
		await(combo::isPopupVisible, "the physically clicked combo-box dropdown did not open");
		boolean taskSortCombo = combo.getName() != null && combo.getName().startsWith("microproject.taskSort.");
		if (taskSortCombo) setComboDiagnostic(combo, "microproject.combo.beforeKeys", comboState(combo));
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		for (int current = 0; current < index; current++) {
			robot.keyPress(KeyEvent.VK_DOWN);
			robot.keyRelease(KeyEvent.VK_DOWN);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
		if (taskSortCombo) setComboDiagnostic(combo, "microproject.combo.afterKeys", comboState(combo));
	}

	private static void setComboDiagnostic(JComboBox<?> combo, String key, String state) throws Exception {
		SwingUtilities.invokeAndWait(() -> combo.putClientProperty(key, state));
	}

	public static String comboState(JComboBox<?> combo) throws Exception {
		AtomicReference<String> state = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			StringBuilder result = new StringBuilder("name=").append(combo.getName())
				.append(" selectedIndex=").append(combo.getSelectedIndex())
				.append(" selectedItem=").append(combo.getSelectedItem())
				.append(" popupVisible=").append(combo.isPopupVisible())
				.append(" focusOwner=").append(focus == null ? "null"
					: focus.getClass().getName() + "/" + focus.getName());
			for (int key : new int[] { KeyEvent.VK_HOME, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER }) {
				javax.swing.KeyStroke stroke = javax.swing.KeyStroke.getKeyStroke(key, 0);
				Object binding = combo.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
				Object action = binding == null ? null : combo.getActionMap().get(binding);
				result.append(" key[").append(KeyEvent.getKeyText(key)).append("]=" ).append(binding)
					.append('/').append(action == null ? "null" : action.getClass().getName());
			}
			state.set(result.toString());
		});
		return state.get();
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
