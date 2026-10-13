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
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
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


	/**
	 * Physically clicks a current component only after Robot hover confirms the
	 * pointer is over that same showing component. The bounds are refreshed after
	 * every move because native window geometry can still settle between snapshots.
	 */
	public static void clickComponent(Robot robot, Component component) throws Exception {
		clickComponent(robot, component, true);
	}

	/** Physically verifies that a disabled component remains inert when clicked. */
	public static void clickDisabledComponent(Robot robot, Component component) throws Exception {
		clickComponent(robot, component, false);
	}

	private static void clickComponent(Robot robot, Component component, boolean expectedEnabled) throws Exception {
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(8);
		List<JPopupMenu> popupAncestors = popupAncestors(component);
		Point lastRobotTarget = null;
		PointerSnapshot last = pointerSnapshot(component);
		PointerSnapshot initial = last;
		PointerSnapshot previous = last;
		List<String> observedTransitions = new ArrayList<>();
		while (System.nanoTime() < deadline) {
			if (last.showing() && last.enabled() == expectedEnabled && last.bounds() != null && !last.bounds().isEmpty()) {
				Point target = new Point(last.bounds().x + last.bounds().width / 2,
					last.bounds().y + last.bounds().height / 2);
				lastRobotTarget = target;
				robot.mouseMove(target.x, target.y);
				robot.waitForIdle();
				last = pointerSnapshot(component);
				appendPointerTransition(observedTransitions, previous, last);
				previous = last;
				if (last.showing() && last.enabled() == expectedEnabled && isInside(last.localMouse(), last.size()))
					break;
			}
			Thread.sleep(25);
			last = pointerSnapshot(component);
			appendPointerTransition(observedTransitions, previous, last);
			previous = last;
		}
		if (last == null || !last.showing() || last.enabled() != expectedEnabled || last.bounds() == null || last.bounds().isEmpty()
			|| !isInside(last.localMouse(), last.size())) {
			throw new AssertionError("Robot pointer did not enter the current component before click dispatch; target={"
				+ (last == null ? "unavailable" : last.identity()) + "}; bounds="
				+ (last == null ? null : last.bounds()) + "; size=" + (last == null ? null : last.size())
				+ "; localMouse=" + (last == null ? null : last.localMouse()) + "; showing="
				+ (last != null && last.showing()) + "; enabled=" + (last != null && last.enabled())
				+ "; expectedEnabled=" + expectedEnabled + "; initial={" + pointerDescription(initial)
				+ "}; transitions=" + (observedTransitions.isEmpty() ? "none" : String.join(" -> ", observedTransitions)
					+ (observedTransitions.size() == 20 ? " (first 20 transitions; further transitions omitted)" : ""))
				+ "; test=" + currentTestCaller() + "; windows={" + showingWindows() + "}"
				+ diagnosticDetails(expectedEnabled, lastRobotTarget, popupAncestors));
		}
		System.out.println("[GUI_PHYSICAL_CLICK] test=" + currentTestCaller() + " target=" + last.identity()
			+ " bounds=" + last.bounds() + " localHover=" + last.localMouse());
		try {
			robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		} finally {
			robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		}
		robot.waitForIdle();
		robot.delay(150);
	}

	private static List<JPopupMenu> popupAncestors(Component component) throws Exception {
		AtomicReference<List<JPopupMenu>> result = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			List<JPopupMenu> ancestors = new ArrayList<>();
			for (Component current = component; current != null; current = current.getParent())
				if (current instanceof JPopupMenu popup) ancestors.add(popup);
			result.set(ancestors);
		});
		return result.get();
	}

	private static String diagnosticDetails(boolean enabledExpected, Point robotTarget, List<JPopupMenu> popups)
		throws Exception {
		if (enabledExpected) return "";
		Point pointer = null;
		try {
			if (MouseInfo.getPointerInfo() != null) pointer = MouseInfo.getPointerInfo().getLocation();
		} catch (java.awt.HeadlessException ignored) { }
		AtomicReference<List<String>> popupStates = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			List<String> states = new ArrayList<>();
			for (JPopupMenu popup : popups)
				states.add("visible=" + popup.isVisible() + ",showing=" + popup.isShowing()
					+ ",bounds=" + popup.getBounds() + ",invoker=" + (popup.getInvoker() == null ? "null"
						: popup.getInvoker().getClass().getSimpleName() + "@" + safeScreenLocation(popup.getInvoker())));
			popupStates.set(states);
		});
		return "; diagnostic={robotRequestedCenter=" + robotTarget
			+ ",awtPointer=" + pointer + ",popupAncestors=" + popupStates.get() + "}";
	}

	private static void appendPointerTransition(List<String> history, PointerSnapshot before, PointerSnapshot after) {
		if (before == null || after == null) return;
		boolean visibilityChanged = before.showing() != after.showing();
		boolean boundsChanged = !java.util.Objects.equals(before.bounds(), after.bounds());
		if (!visibilityChanged && !boundsChanged) return;
		if (history.size() >= 20) return;
		history.add(pointerDescription(before) + " => " + pointerDescription(after));
	}

	private static Point safeScreenLocation(Component component) {
		try { return component.getLocationOnScreen(); }
		catch (java.awt.IllegalComponentStateException ignored) { return null; }
	}

	private static String pointerDescription(PointerSnapshot snapshot) {
		if (snapshot == null) return "unavailable";
		return "showing=" + snapshot.showing() + ",enabled=" + snapshot.enabled()
			+ ",bounds=" + snapshot.bounds() + ",size=" + snapshot.size()
			+ ",mouse=" + snapshot.localMouse() + ",target=" + snapshot.identity();
	}

	private record PointerSnapshot(boolean showing, boolean enabled, Rectangle bounds,
		java.awt.Dimension size, Point localMouse, String identity) {
	}

	private static PointerSnapshot pointerSnapshot(Component component) throws Exception {
		PointerSnapshot[] result = new PointerSnapshot[1];
		SwingUtilities.invokeAndWait(() -> {
			boolean showing = component.isShowing();
			Rectangle bounds = null;
			if (showing) {
				try {
					bounds = new Rectangle(component.getLocationOnScreen(), component.getSize());
				} catch (java.awt.IllegalComponentStateException ignored) {
					showing = false;
				}
			}
			Point localMouse = showing ? component.getMousePosition() : null;
			String identity = component.getClass().getName() + "[name=" + component.getName() + "]";
			if (component instanceof AbstractButton button)
				identity += "[action=" + button.getActionCommand() + ",text=" + button.getText() + "]";
			result[0] = new PointerSnapshot(showing, component.isEnabled(), bounds,
				component.getSize(), localMouse, identity);
		});
		return result[0];
	}

	private static boolean isInside(Point point, java.awt.Dimension size) {
		return point != null && size != null && point.x >= 0 && point.y >= 0
			&& point.x < size.width && point.y < size.height;
	}

	private static String currentTestCaller() {
		for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
			String className = frame.getClassName();
			String methodName = frame.getMethodName();
			if (className.equals(WindowAcceptanceAssertions.class.getName())
				|| className.startsWith("com.microproject.testsupport.")
				|| className.equals("java.lang.Thread")
				|| methodName.equals("click") || methodName.equals("clickAndAwaitSelected"))
				continue;
			return className + "#" + methodName + ":" + frame.getLineNumber();
		}
		return "unknown";
	}

	private static String showingWindows() throws Exception {
		String[] result = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			StringBuilder windows = new StringBuilder();
			for (java.awt.Window window : java.awt.Window.getWindows()) {
				if (!window.isShowing()) continue;
				if (windows.length() > 0) windows.append("; ");
				windows.append(window.getClass().getSimpleName()).append("[active=").append(window.isActive())
					.append(",focused=").append(window.isFocused()).append(",bounds=").append(window.getBounds()).append("]");
			}
			result[0] = windows.toString();
		});
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
