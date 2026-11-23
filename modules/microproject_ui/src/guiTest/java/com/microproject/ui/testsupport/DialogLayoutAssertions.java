/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
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
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/** Shared visual invariants for text-bearing controls in dialogs. */
public final class DialogLayoutAssertions {
	private static final int MAX_SCROLL_ATTEMPTS = 40;
	/**
	 * Pixels of slack allowed between a visible descendant and its owning content
	 * edge. Layout managers keep their own border insets, which are added to this
	 * value, so only an unexplained band wider than a font line is a failure.
	 */
	private static final int DEAD_BAND_TOLERANCE = 16;
	/** Pixels of slack inside a scroll viewport, where the view edge is exact. */
	private static final int VIEWPORT_BAND_TOLERANCE = 8;
	/** Share of a window growth the declared work surface must absorb, in percent. */
	private static final int GROWTH_ABSORPTION_PERCENT = 50;

	private DialogLayoutAssertions() {
	}

	/** Ensures visible, non-empty text controls retain their font-derived height. */
	public static void assertTextControlsAtPreferredHeight(Container root, String context) throws Exception {
		HeadlessLayoutAssertions.assertTextControlsAtPreferredHeight(root, context);
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
		for (int attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
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
			JScrollPane pane = scrollPaneFor(viewport);
			JScrollBar vertical = pane.getVerticalScrollBar();
			JScrollBar horizontal = pane.getHorizontalScrollBar();
			state.append("; viewportView=").append(viewport.getView().getClass().getSimpleName())
				.append(" target=").append(target).append(" viewRect=").append(viewport.getViewRect())
				.append(" viewSize=").append(viewport.getView().getSize())
				.append(" verticalScroll={value=").append(vertical.getValue()).append(",maximum=")
				.append(vertical.getMaximum()).append(",extent=").append(vertical.getVisibleAmount())
				.append(",unitIncrement=").append(vertical.getUnitIncrement()).append('}')
				.append(" horizontalScroll={value=").append(horizontal.getValue()).append(",maximum=")
				.append(horizontal.getMaximum()).append(",extent=").append(horizontal.getVisibleAmount())
				.append(",unitIncrement=").append(horizontal.getUnitIncrement()).append('}');
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

	/**
	 * Asserts the #724 fixed-size form contract: the dialog cannot be resized by
	 * the user, keeps its packed minimum, keeps text controls at their preferred
	 * height, and stays inside the usable screen. Data views that must grow use
	 * {@link #assertResizeKeepsTextControls(Window, Container, int, int, String)}.
	 */
	public static void assertFixedSizeForm(Window window, Container content, String context) throws Exception {
		final boolean[] resizable = new boolean[1];
		final Dimension[] minimum = new Dimension[1];
		final Dimension[] size = new Dimension[1];
		SwingUtilities.invokeAndWait(() -> {
			resizable[0] = isUserResizable(window);
			minimum[0] = window.getMinimumSize();
			size[0] = window.getSize();
		});
		assertFalse(resizable[0], context + " must declare a fixed-size form (no user resize)");
		assertTrue(minimum[0].width > 0 && minimum[0].height > 0,
			context + " must lock its packed minimum size, minimum=" + minimum[0]);
		assertTrue(size[0].width >= minimum[0].width && size[0].height >= minimum[0].height,
			context + " must not be smaller than its packed minimum, size=" + size[0] + " minimum=" + minimum[0]);
		assertTextControlsAtPreferredHeight(content, context);
		assertNoDeadBand(content, context);
		assertWithinUsableScreen(window, context);
	}

	/**
	 * Asserts the size contract a window declares for itself (#863): a window the
	 * user cannot resize must keep its packed form, and a resizable window must
	 * spend growth on its work surface instead of leaving an empty band.
	 */
	public static void assertWindowSizeContract(Window window, Container content, String context) throws Exception {
		final boolean[] resizable = new boolean[1];
		SwingUtilities.invokeAndWait(() -> resizable[0] = isUserResizable(window));
		if (!resizable[0]) {
			assertFixedSizeForm(window, content, context);
			return;
		}
		assertResizeKeepsWorkArea(window, content, context);
	}

	/**
	 * Grows a resizable window, checks that no text control is clipped and that the
	 * declared work surface takes the growth, then returns to the packed minimum
	 * and checks the same layout invariants there.
	 */
	public static void assertResizeKeepsWorkArea(Window window, Container content, String context) throws Exception {
		final Dimension[] packed = new Dimension[1];
		final Rectangle[] usable = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			packed[0] = window.getSize();
			usable[0] = usableScreen(window);
		});
		int widthGrowth = Math.max(0, Math.min(200, usable[0].width - packed[0].width - DEAD_BAND_TOLERANCE));
		int heightGrowth = Math.max(0, Math.min(140, usable[0].height - packed[0].height - DEAD_BAND_TOLERANCE));
		assertTrue(widthGrowth > 0 || heightGrowth > 0, context + " must have room to grow inside the usable screen; size="
			+ packed[0] + " usable=" + usable[0]);
		Component surface = workSurface(content);
		Rectangle surfaceBefore = boundsWithin(content, surface);
		resizeWindow(window, packed[0].width + widthGrowth, packed[0].height + heightGrowth);
		assertTextControlsAtPreferredHeight(content, context + " after growth");
		assertNoDeadBand(content, context + " after growth");
		Rectangle surfaceAfter = boundsWithin(content, surface);
		assertWorkSurfaceAbsorbsGrowth(context, surface, surfaceBefore, surfaceAfter, widthGrowth, heightGrowth);
		resizeWindow(window, packed[0].width, packed[0].height);
		assertTextControlsAtPreferredHeight(content, context + " back at its packed minimum");
		assertNoDeadBand(content, context + " back at its packed minimum");
		if (surface != null) {
			Rectangle surfaceAtMinimum = boundsWithin(content, surface);
			assertTrue(surfaceAtMinimum.width > 0 && surfaceAtMinimum.height > 0,
				() -> context + " work surface must remain usable at the packed minimum: surface="
					+ describeSurface(surface) + " bounds=" + surfaceAtMinimum + " content=" + content.getSize());
		}
		assertWithinUsableScreen(window, context);
	}

	/**
	 * Fails when a visible descendant stops short of its content edge by more than
	 * the owning border inset plus a font line, or when a scroll viewport shows an
	 * unexplained band around a view that could have filled it. Those two bands are
	 * what "the window is mostly empty" looks like to a user.
	 */
	public static void assertNoDeadBand(Container content, String context) throws Exception {
		final String[] band = new String[1];
		SwingUtilities.invokeAndWait(() -> band[0] = deadBandDiagnostic(content));
		assertTrue(band[0] == null, () -> context + " leaves an unexplained empty band: " + band[0]);
	}

	/** Returns the widest bottom/right band diagnostic, or null when the content is filled. */
	public static String deadBandDiagnostic(Container content) {
		if (content.getWidth() <= 0 || content.getHeight() <= 0) return null;
		int[] extent = new int[2];
		collectExtent(content, content, extent);
		Insets insets = content.getInsets();
		int rightGap = content.getWidth() - extent[0];
		int bottomGap = content.getHeight() - extent[1];
		if (rightGap > insets.right + DEAD_BAND_TOLERANCE || bottomGap > insets.bottom + DEAD_BAND_TOLERANCE)
			return "rightGap=" + rightGap + " bottomGap=" + bottomGap + " content=" + content.getSize()
				+ " insets=" + insets + " filled=" + extent[0] + "x" + extent[1]
				+ " owner=" + content.getClass().getSimpleName();
		return viewportBandDiagnostic(content);
	}

	private static String viewportBandDiagnostic(Container container) {
		for (Component child : container.getComponents()) {
			if (!child.isVisible()) continue;
			if (child instanceof JViewport viewport) {
				Component view = viewport.getView();
				if (view == null || !view.isShowing()) continue;
				String band = viewportBand(viewport, view);
				if (band != null) return band;
			}
			if (child instanceof Container nested) {
				String band = viewportBandDiagnostic(nested);
				if (band != null) return band;
			}
		}
		return null;
	}

	private static String viewportBand(JViewport viewport, Component view) {
		if (fillsViewport(view)) return null;
		int bottomGap = tracksViewportHeight(view) ? 0 : viewport.getHeight() - view.getHeight();
		int rightGap = tracksViewportWidth(view) ? 0 : viewport.getWidth() - view.getWidth();
		if (bottomGap > VIEWPORT_BAND_TOLERANCE || rightGap > VIEWPORT_BAND_TOLERANCE)
			return "viewportBand rightGap=" + rightGap + " bottomGap=" + bottomGap + " viewport=" + viewport.getSize()
				+ " view=" + view.getClass().getSimpleName() + " viewSize=" + view.getSize();
		return null;
	}

	private static boolean tracksViewportHeight(Component view) {
		return view instanceof javax.swing.Scrollable scrollable && scrollable.getScrollableTracksViewportHeight();
	}

	private static boolean tracksViewportWidth(Component view) {
		return view instanceof javax.swing.Scrollable scrollable && scrollable.getScrollableTracksViewportWidth();
	}

	/**
	 * A data view paints its own viewport area: an empty spreadsheet, list, or text
	 * canvas is the intended work area (#724 A10), not an unexplained band.
	 */
	private static boolean fillsViewport(Component view) {
		return isDataView(view) || view instanceof javax.swing.JScrollPane;
	}

	/** Compact scroll-viewport state used in the #863 cross-window report. */
	public static String layoutDiagnostic(Container content) {
		StringBuilder state = new StringBuilder("content=").append(content.getSize());
		appendViewports(content, state);
		return state.toString();
	}

	private static void appendViewports(Container container, StringBuilder state) {
		for (Component child : container.getComponents()) {
			if (!child.isVisible()) continue;
			if (child instanceof JViewport viewport) {
				Component view = viewport.getView();
				state.append("; viewport=").append(viewport.getSize()).append(" view=")
					.append(view == null ? "null" : view.getClass().getSimpleName() + view.getSize());
			}
			if (child instanceof Container nested) appendViewports(nested, state);
		}
	}

	private static void collectExtent(Container content, Container container, int[] extent) {
		for (Component child : container.getComponents()) {
			if (!child.isVisible() || child.getWidth() <= 0 || child.getHeight() <= 0) continue;
			Rectangle bounds = SwingUtilities.convertRectangle(container, child.getBounds(), content);
			extent[0] = Math.max(extent[0], Math.min(content.getWidth(), bounds.x + bounds.width));
			extent[1] = Math.max(extent[1], Math.min(content.getHeight(), bounds.y + bounds.height));
			if (child instanceof Container nested) collectExtent(content, nested, extent);
		}
	}

	/**
	 * Resolves the surface that must absorb window growth: a tabbed pane, or the
	 * largest scrollable data view, or the largest scroll pane. Bounded forms
	 * return null and are expected to declare a fixed size instead.
	 */
	public static Component workSurface(Container content) throws Exception {
		final Component[] surface = new Component[1];
		SwingUtilities.invokeAndWait(() -> surface[0] = findWorkSurface(content));
		return surface[0];
	}

	private static Component findWorkSurface(Container content) {
		List<Component> candidates = new ArrayList<>();
		collectWorkSurfaces(content, candidates);
		Component largest = null;
		for (Component candidate : candidates) {
			Rectangle bounds = boundsWithin(content, candidate);
			if (largest == null) largest = candidate;
			else {
				Rectangle current = boundsWithin(content, largest);
				if ((long) bounds.width * bounds.height > (long) current.width * current.height) largest = candidate;
			}
		}
		return largest;
	}

	private static void collectWorkSurfaces(Container container, List<Component> candidates) {
		for (Component child : container.getComponents()) {
			if (!child.isVisible()) continue;
			if (child instanceof javax.swing.JTabbedPane) candidates.add(child);
			else if (child instanceof JScrollPane scrollPane) {
				java.awt.Component view = scrollPane.getViewport().getView();
				// Measure the scroll pane: a table or canvas view keeps its own
				// preferred size, and the viewport is what the user sees grow.
				if (isDataView(view) || isPaintCanvas(view)) {
					candidates.add(scrollPane);
					continue;
				}
			} else if (isDataView(child)) candidates.add(child);
			if (child instanceof Container nested) collectWorkSurfaces(nested, candidates);
		}
	}

	/** A scrollable leaf component that paints its own area, such as a Gantt or calendar canvas. */
	private static boolean isPaintCanvas(Component view) {
		if (view == null || view instanceof javax.swing.table.JTableHeader) return false;
		return !(view instanceof Container container) || container.getComponentCount() == 0;
	}

	private static boolean isDataView(Component component) {
		return component instanceof javax.swing.JTable || component instanceof javax.swing.JTree
			|| component instanceof javax.swing.JList || component instanceof javax.swing.JTextArea
			|| component instanceof javax.swing.JEditorPane || component instanceof javax.swing.JTextPane;
	}

	private static Rectangle boundsWithin(Container content, Component component) {
		if (component == null || !component.isShowing()) return new Rectangle();
		return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), content);
	}

	private static void assertWorkSurfaceAbsorbsGrowth(String context, Component surface, Rectangle before,
			Rectangle after, int widthGrowth, int heightGrowth) {
		if (surface == null) return;
		int widthDelta = after.width - before.width;
		int heightDelta = after.height - before.height;
		int requiredWidth = widthGrowth * GROWTH_ABSORPTION_PERCENT / 100;
		int requiredHeight = heightGrowth * GROWTH_ABSORPTION_PERCENT / 100;
		assertTrue(widthDelta >= requiredWidth || heightDelta >= requiredHeight,
			() -> context + " wastes its growth outside the work surface: surface=" + describeSurface(surface)
				+ " before=" + before + " after=" + after
				+ " windowGrowth=" + widthGrowth + "x" + heightGrowth);
	}

	private static String describeSurface(Component surface) {
		if (surface == null) return "none";
		return surface.getClass().getSimpleName() + "[name=" + surface.getName() + ",showing=" + surface.isShowing() + "]";
	}

	private static void resizeWindow(Window window, int width, int height) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window.setSize(width, height);
			window.validate();
			window.repaint();
		});
	}

	private static boolean isUserResizable(Window window) {
		if (window instanceof Dialog dialog) return dialog.isResizable();
		if (window instanceof Frame frame) return frame.isResizable();
		return true;
	}

	private static Rectangle usableScreen(Window window) {
		GraphicsConfiguration configuration = window.getGraphicsConfiguration();
		Rectangle screen = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		screen.x += insets.left;
		screen.y += insets.top;
		screen.width -= insets.left + insets.right;
		screen.height -= insets.top + insets.bottom;
		return screen;
	}

	/** Grows a visible top-level dialog/window and checks its content and usable-screen bounds. */
	public static void assertResizeKeepsTextControls(Window window, Container content, int widthGrowth,
			int heightGrowth, String context) throws Exception {
		assertResizeWithinUsableScreen(window, widthGrowth, heightGrowth, context);
		assertTextControlsAtPreferredHeight(content, context + " after resize");
	}

	/** Resizes a visible window and checks its usable-screen bounds without text-layout assertions. */
	public static void assertResizeWithinUsableScreen(Window window, int widthGrowth, int heightGrowth,
			String context) throws Exception {
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
	}

	/** Checks the top-level window stays on-screen after packing, centering, or resizing. */
	public static void assertWithinUsableScreen(Window window, String context) throws Exception {
		final Rectangle[] bounds = new Rectangle[1];
		final Rectangle[] usable = new Rectangle[1];
		Runnable inspect = () -> {
			bounds[0] = window.getBounds();
			usable[0] = usableScreen(window);
		};
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
		boolean contained = false;
		do {
			if (SwingUtilities.isEventDispatchThread()) inspect.run();
			else SwingUtilities.invokeAndWait(inspect);
			contained = usable[0].contains(bounds[0]);
			if (contained) break;
			// Fixed sleep kept: each pass re-reads native window geometry on the EDT, which can throw checked exceptions.
			Thread.sleep(25);
		} while (System.nanoTime() < deadline);
		assertTrue(contained,
			context + " escapes usable screen after native geometry settles bounds=" + bounds[0] + " usable=" + usable[0]);
	}

}
