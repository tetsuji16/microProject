/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/** Shared visual invariants for text-bearing controls in dialogs. */
public final class DialogLayoutAssertions {
	private DialogLayoutAssertions() {
	}

	/** Ensures visible, non-empty text controls retain their font-derived height. */
	public static void assertTextControlsAtPreferredHeight(Container root, String context) throws Exception {
		final String[] clipping = new String[1];
		Runnable inspect = () -> {
			layoutTree(root);
			clipping[0] = findClippedTextControl(root);
		};
		if (SwingUtilities.isEventDispatchThread())
			inspect.run();
		else
			SwingUtilities.invokeAndWait(inspect);
		assertTrue(clipping[0] == null, () -> context + " clips a text control: " + clipping[0]);
	}

	/** Scrolls a dialog body with physical wheel input until a clipped control is fully reachable. */
	public static void physicallyScrollIntoView(Robot robot, Component component, String description) throws Exception {
		Rectangle[] target = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> target[0] = new Rectangle(0, 0, component.getWidth(), component.getHeight()));
		physicallyScrollIntoView(robot, component, target[0], description);
	}

	/** Scrolls a component's local target rectangle into every enclosing viewport with physical wheel input. */
	public static void physicallyScrollIntoView(Robot robot, Component component, Rectangle localTarget,
			String description) throws Exception {
		List<JViewport> viewports = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
				if (parent instanceof JViewport candidate) viewports.add(candidate);
			}
		});
		assertTrue(!viewports.isEmpty(), description + " must be hosted by a scrollable dialog viewport");
		for (int pass = 0; pass <= viewports.size(); pass++) {
			for (int index = viewports.size() - 1; index >= 0; index--)
				scrollTargetWithinViewport(robot, component, localTarget, viewports.get(index), description);
			if (targetInsideAllViewports(component, localTarget, viewports)) return;
		}
		assertTargetFullyVisible(component, localTarget, viewports, description);
	}

	/** Scrolls a target into view and clicks its center through the physical Robot route. */
	public static void physicallyClick(Robot robot, Component component, Rectangle localTarget,
			String description) throws Exception {
		physicallyScrollIntoView(robot, component, localTarget, description);
		Point[] screenPoint = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			assertTargetVisibleOnEdt(component, localTarget, ancestorViewports(component), description);
			Point location = component.getLocationOnScreen();
			screenPoint[0] = new Point(location.x + localTarget.x + localTarget.width / 2,
				location.y + localTarget.y + localTarget.height / 2);
		});
		robot.mouseMove(screenPoint[0].x, screenPoint[0].y);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	/** Scrolls a component fully into view and clicks its physical center. */
	public static void physicallyClick(Robot robot, Component component, String description) throws Exception {
		Rectangle[] target = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> target[0] = new Rectangle(0, 0,
			Math.max(1, component.getWidth()), Math.max(1, component.getHeight())));
		physicallyClick(robot, component, target[0], description);
	}

	private static void scrollTargetWithinViewport(Robot robot, Component component, Rectangle localTarget,
			JViewport viewport, String description) throws Exception {
		for (int attempt = 0; attempt < 20; attempt++) {
			Rectangle[] target = new Rectangle[1];
			Rectangle[] visible = new Rectangle[1];
			boolean[] showing = new boolean[1];
			String[] diagnostic = new String[1];
			SwingUtilities.invokeAndWait(() -> {
				target[0] = SwingUtilities.convertRectangle(component, localTarget, viewport.getView());
				visible[0] = viewport.getViewRect();
				showing[0] = viewport.isShowing();
				diagnostic[0] = visibilityDiagnostic(component, localTarget, ancestorViewports(component));
			});
			assertTrue(showing[0], description + " viewport is not showing: " + diagnostic[0]);
			if (visible[0].contains(target[0])) return;
			boolean vertical = target[0].y < visible[0].y
				|| target[0].y + target[0].height > visible[0].y + visible[0].height;
			Point wheelPoint = scrollBarWheelPoint(viewport, vertical, description);
			robot.mouseMove(wheelPoint.x, wheelPoint.y);
			if (vertical) {
				robot.mouseWheel(target[0].y < visible[0].y ? -3 : 3);
			} else {
				robot.keyPress(java.awt.event.KeyEvent.VK_SHIFT);
				try {
					robot.mouseWheel(target[0].x < visible[0].x ? -3 : 3);
				} finally {
					robot.keyRelease(java.awt.event.KeyEvent.VK_SHIFT);
				}
			}
			robot.waitForIdle();
			robot.delay(100);
		}
		Rectangle[] target = new Rectangle[1];
		Rectangle[] visible = new Rectangle[1];
		String[] diagnostic = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			target[0] = SwingUtilities.convertRectangle(component, localTarget, viewport.getView());
			visible[0] = viewport.getViewRect();
			diagnostic[0] = visibilityDiagnostic(component, localTarget, ancestorViewports(component));
		});
		assertTrue(visible[0].contains(target[0]), description + " remained clipped after physical scrolling: target="
			+ target[0] + " viewport=" + visible[0] + "; "
			+ diagnostic[0]);
	}

	private static Point scrollBarWheelPoint(JViewport viewport, boolean vertical, String description) throws Exception {
		Point[] point = new Point[1];
		String[] diagnostic = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			JScrollPane scrollPane = scrollPaneFor(viewport);
			JScrollBar bar = vertical ? scrollPane.getVerticalScrollBar() : scrollPane.getHorizontalScrollBar();
			Rectangle visible = bar.getVisibleRect();
			boolean showing = bar.isShowing() && !visible.isEmpty();
			diagnostic[0] = "scrollbar=" + (vertical ? "vertical" : "horizontal") + " showing=" + bar.isShowing()
				+ " visibleRect=" + visible + "; "
				+ visibilityDiagnostic(viewport, new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight()),
					ancestorViewports(viewport));
			if (showing) {
				Point location = bar.getLocationOnScreen();
				point[0] = new Point(location.x + visible.x + visible.width / 2,
					location.y + visible.y + visible.height / 2);
			}
		});
		assertTrue(point[0] != null, () -> description + " cannot reach the " + (vertical ? "vertical" : "horizontal")
			+ " scrollbar for physical wheel input: " + diagnostic[0]);
		return point[0];
	}

	private static JScrollPane scrollPaneFor(JViewport viewport) {
		for (Container parent = viewport.getParent(); parent != null; parent = parent.getParent())
			if (parent instanceof JScrollPane scrollPane && scrollPane.getViewport() == viewport) return scrollPane;
		throw new AssertionError("viewport is not owned by a JScrollPane: " + viewport);
	}

	private static List<JViewport> ancestorViewports(Component component) {
		List<JViewport> viewports = new ArrayList<>();
		for (Container parent = component.getParent(); parent != null; parent = parent.getParent())
			if (parent instanceof JViewport viewport) viewports.add(viewport);
		return viewports;
	}

	private static void assertTargetFullyVisible(Component component, Rectangle localTarget, List<JViewport> viewports,
			String description) throws Exception {
		SwingUtilities.invokeAndWait(() -> assertTargetVisibleOnEdt(component, localTarget, viewports, description));
	}

	private static boolean targetInsideAllViewports(Component component, Rectangle localTarget,
			List<JViewport> viewports) throws Exception {
		boolean[] visible = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle componentVisible = componentVisibleRect(component);
			visible[0] = component.isShowing() && componentVisible.contains(localTarget);
			for (JViewport viewport : viewports) {
				Rectangle target = SwingUtilities.convertRectangle(component, localTarget, viewport.getView());
				visible[0] &= viewport.isShowing() && viewport.getViewRect().contains(target);
			}
		});
		return visible[0];
	}

	private static void assertTargetVisibleOnEdt(Component component, Rectangle localTarget, List<JViewport> viewports,
			String description) {
		Rectangle visible = componentVisibleRect(component);
		assertTrue(component.isShowing() && visible.contains(localTarget),
			() -> description + " target is not fully visible within its component ancestors: "
				+ visibilityDiagnostic(component, localTarget, viewports));
		for (JViewport viewport : viewports) {
			Rectangle target = SwingUtilities.convertRectangle(component, localTarget, viewport.getView());
			assertTrue(viewport.isShowing() && viewport.getViewRect().contains(target),
				() -> description + " target is outside an ancestor viewport: "
					+ visibilityDiagnostic(component, localTarget, viewports));
		}
		Window window = SwingUtilities.getWindowAncestor(component);
		JRootPane rootPane = window instanceof javax.swing.RootPaneContainer rootPaneContainer
			? rootPaneContainer.getRootPane() : null;
		if (rootPane != null) {
			Rectangle target = SwingUtilities.convertRectangle(component, localTarget, rootPane);
			assertTrue(rootPane.isShowing() && rootPane.getVisibleRect().contains(target),
				() -> description + " target is outside its window content: "
					+ visibilityDiagnostic(component, localTarget, viewports));
		}
	}

	private static String visibilityDiagnostic(Component component, Rectangle localTarget, List<JViewport> viewports) {
		StringBuilder state = new StringBuilder("component=").append(component.getClass().getName())
			.append(" localTarget=").append(localTarget).append(" visibleRect=").append(componentVisibleRect(component));
		for (JViewport viewport : viewports) {
			Rectangle target = SwingUtilities.convertRectangle(component, localTarget, viewport.getView());
			state.append("; viewportView=").append(viewport.getView().getClass().getSimpleName())
				.append(" target=").append(target).append(" viewRect=").append(viewport.getViewRect());
		}
		Window window = SwingUtilities.getWindowAncestor(component);
		if (window != null) state.append("; window=").append(window.getBounds());
		return state.toString();
	}

	private static Rectangle componentVisibleRect(Component component) {
		return component instanceof JComponent swingComponent
			? swingComponent.getVisibleRect() : new Rectangle(0, 0, component.getWidth(), component.getHeight());
	}

	/**
	 * FlatLaf's two vertically stacked spinner step buttons share one border
	 * pixel so their adjoining outlines render as a single control. This is the
	 * only button overlap exempted by dialog layout probes; glyph and other
	 * control overlaps remain failures.
	 */
	public static boolean isExpectedSpinnerStepperBorderSeam(Container parent, Component first, Component second) {
		if (!(parent instanceof JSpinner spinner)
				|| !spinner.getUI().getClass().getName().equals("com.formdev.flatlaf.ui.FlatSpinnerUI")
				|| !(first instanceof AbstractButton) || !(second instanceof AbstractButton))
			return false;
		boolean stepperPair = "Spinner.nextButton".equals(first.getName())
				&& "Spinner.previousButton".equals(second.getName())
			|| "Spinner.previousButton".equals(first.getName())
				&& "Spinner.nextButton".equals(second.getName());
		if (!stepperPair)
			return false;
		Rectangle a = first.getBounds();
		Rectangle b = second.getBounds();
		Rectangle intersection = a.intersection(b);
		return a.x == b.x && a.width == b.width && intersection.width == a.width
				&& intersection.height == 1
				&& (a.y + a.height == b.y + 1 || b.y + b.height == a.y + 1);
	}

	/** Grows a visible top-level dialog/window and checks its content and usable-screen bounds. */
	public static void assertResizeKeepsTextControls(Window window, Container content, int widthGrowth,
			int heightGrowth, String context) throws Exception {
		final Rectangle[] resized = new Rectangle[1];
		Runnable resize = () -> {
			window.setSize(window.getWidth() + widthGrowth, window.getHeight() + heightGrowth);
			window.validate();
			resized[0] = window.getBounds();
		};
		if (SwingUtilities.isEventDispatchThread()) resize.run();
		else SwingUtilities.invokeAndWait(resize);
		assertTrue(resized[0].width > 0 && resized[0].height > 0, context + " did not resize");
		assertWithinUsableScreen(window, context);
		assertTextControlsAtPreferredHeight(content, context + " after resize");
	}

	/** Checks the top-level window stays on-screen after packing, centering, or resizing. */
	public static void assertWithinUsableScreen(Window window, String context) throws Exception {
		final Rectangle[] bounds = new Rectangle[1];
		final Rectangle[] usable = new Rectangle[1];
		Runnable inspect = () -> {
			GraphicsConfiguration configuration = window.getGraphicsConfiguration();
			Rectangle screen = new Rectangle(configuration.getBounds());
			Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
			screen.x += insets.left;
			screen.y += insets.top;
			screen.width -= insets.left + insets.right;
			screen.height -= insets.top + insets.bottom;
			bounds[0] = window.getBounds();
			usable[0] = screen;
		};
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
		boolean contained = false;
		do {
			if (SwingUtilities.isEventDispatchThread()) inspect.run();
			else SwingUtilities.invokeAndWait(inspect);
			contained = usable[0].contains(bounds[0]);
			if (contained) break;
			Thread.sleep(25);
		} while (System.nanoTime() < deadline);
		assertTrue(contained,
			context + " escapes usable screen after native geometry settles bounds=" + bounds[0] + " usable=" + usable[0]);
	}

	private static String findClippedTextControl(Container root) {
		Component[] children = root.getComponents();
		for (int first = 0; first < children.length; first++) {
			Component child = children[first];
			if (!child.isVisible() || !hasVisibleText(child))
				continue;
			Rectangle bounds = child.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0 || bounds.x < 0 || bounds.y < 0
					|| bounds.x + bounds.width > root.getWidth() || bounds.y + bounds.height > root.getHeight())
				return child.getClass().getName() + " escapes parent bounds=" + bounds + " parent=" + root.getSize();
			if (child.getHeight() < child.getPreferredSize().height)
				return child.getClass().getName() + " bounds=" + bounds + " preferred=" + child.getPreferredSize();
			for (int second = first + 1; second < children.length; second++) {
				Component sibling = children[second];
				if (sibling.isVisible() && hasVisibleText(sibling) && bounds.intersects(sibling.getBounds()))
					return child.getClass().getName() + " overlaps " + sibling.getClass().getName()
						+ " bounds=" + bounds + " sibling=" + sibling.getBounds();
			}
		}
		for (Component child : root.getComponents()) {
			if (!child.isVisible())
				continue;
			if (child instanceof Container nested) {
				String clipping = findClippedTextControl(nested);
				if (clipping != null)
					return clipping;
			}
		}
		return null;
	}

	private static boolean hasVisibleText(Component component) {
		if (component instanceof JTextComponent)
			return true;
		if (component instanceof JComboBox<?>)
			return true;
		if (component instanceof JLabel label)
			return label.getText() != null && !label.getText().isBlank();
		return component instanceof AbstractButton button && button.getText() != null && !button.getText().isBlank();
	}

	private static void layoutTree(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container nested)
				layoutTree(nested);
	}
}
