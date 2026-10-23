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

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JLabel;
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

	/** Scrolls a component's local target rectangle fully into its nearest viewport with physical wheel input. */
	public static void physicallyScrollIntoView(Robot robot, Component component, Rectangle localTarget,
			String description) throws Exception {
		JViewport[] viewport = new JViewport[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
				if (parent instanceof JViewport candidate) {
					viewport[0] = candidate;
					return;
				}
			}
		});
		assertTrue(viewport[0] != null, description + " must be hosted by a scrollable dialog viewport");
		String[] visibility = new String[1];
		boolean[] viewportShowing = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			StringBuilder state = new StringBuilder("component=").append(component.getClass().getName())
				.append(" showing=").append(component.isShowing())
				.append(" viewportShowing=").append(viewport[0].isShowing());
			for (Component current = component; current != null; current = current.getParent())
				state.append(" <- ").append(current.getClass().getSimpleName()).append("[showing=")
					.append(current.isShowing()).append(",visible=").append(current.isVisible()).append(']');
			visibility[0] = state.toString();
			viewportShowing[0] = viewport[0].isShowing();
		});
		assertTrue(viewportShowing[0], () -> description + " viewport is not showing before physical scroll: " + visibility[0]);
		for (int attempt = 0; attempt < 20; attempt++) {
			Rectangle[] target = new Rectangle[1];
			Rectangle[] visible = new Rectangle[1];
			Point[] viewportLocation = new Point[1];
			Point[] wheelPoint = new Point[1];
			SwingUtilities.invokeAndWait(() -> {
				JViewport current = viewport[0];
				Component view = current.getView();
				target[0] = SwingUtilities.convertRectangle(component, localTarget, view);
				visible[0] = current.getViewRect();
				viewportLocation[0] = current.getLocationOnScreen();
				wheelPoint[0] = new Point(viewportLocation[0].x + visible[0].width / 2,
					viewportLocation[0].y + visible[0].height / 2);
			});
			if (visible[0].contains(target[0])) return;
			robot.mouseMove(wheelPoint[0].x, wheelPoint[0].y);
			boolean scrollVertically = target[0].y < visible[0].y
				|| target[0].y + target[0].height > visible[0].y + visible[0].height;
			if (scrollVertically) {
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
		SwingUtilities.invokeAndWait(() -> {
			JViewport current = viewport[0];
			target[0] = SwingUtilities.convertRectangle(component, localTarget, current.getView());
			visible[0] = current.getViewRect();
		});
		assertTrue(visible[0].contains(target[0]), () -> description + " remained clipped after physical scrolling: target="
			+ target[0] + " viewport=" + visible[0]);
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
