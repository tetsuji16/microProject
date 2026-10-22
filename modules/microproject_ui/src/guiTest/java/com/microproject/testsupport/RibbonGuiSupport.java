/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.testsupport;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.ModernRibbonPanel;

/** Physical Robot helpers for the application's Swing ribbon and overflow menus. */
public final class RibbonGuiSupport {
	private RibbonGuiSupport() { }

	public static AbstractButton findVisibleOrExpand(Robot robot, Component root, String commandId) throws Exception {
		AtomicReference<AbstractButton> visible = new AtomicReference<>();
		AtomicReference<AbstractButton> collapsedTrigger = new AtomicReference<>();
		AtomicReference<JPopupMenu> collapsedPopup = new AtomicReference<>();
		AtomicReference<AbstractButton> popupCommand = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			for (Component component : UiComponentWalker.flatten(root)) {
				if (component instanceof AbstractButton button && button.isShowing() && fullyVisibleInViewport(button)
					&& commandId.equals(buttonId(button))) {
					visible.set(button);
					return;
				}
			}
			for (Component component : UiComponentWalker.flatten(root)) {
				if (!(component instanceof AbstractButton button) || !button.isShowing()
					|| !(button instanceof JComponent swingButton)) continue;
				Object value = swingButton.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
				if (!(value instanceof JPopupMenu popup)) continue;
				AbstractButton command = findSwingCommand(popup, commandId);
				if (command != null) {
					if (fullyVisibleInViewport(button)) {
						collapsedTrigger.set(button);
						collapsedPopup.set(popup);
						popupCommand.set(command);
						return;
					}
					// A clipped proxy can be reached by scrolling its containing ribbon band.
					collapsedTrigger.set(button);
					collapsedPopup.set(popup);
					popupCommand.set(command);
				}
			}
		});
		if (visible.get() != null) return visible.get();
		if (collapsedTrigger.get() != null) {
			if (!fullyVisibleInViewportOnEdt(collapsedTrigger.get()))
				scrollComponentIntoRibbonViewport(robot, root, collapsedTrigger.get(), commandId);
			WindowAcceptanceAssertions.clickComponent(robot, collapsedTrigger.get());
			GuiAcceptanceSupport.await(() -> {
				AbstractButton current = visiblePopupCommand(collapsedPopup.get(), commandId);
				if (current == null) return false;
				popupCommand.set(current);
				return collapsedPopup.get().isVisible();
			}, "Swing ribbon overflow did not expose a current visible command after its physical click: " + commandId);
			return popupCommand.get();
		}
		AbstractButton viewportCommand = findCommand(root, commandId) instanceof AbstractButton button ? button : null;
		if (viewportCommand != null && scrollIntoRibbonViewport(robot, root, commandId))
			return visibleCommand(root, commandId);
		throw new AssertionError("Ribbon command is neither visible nor reachable through a ribbon viewport or Swing overflow group: " + commandId);
	}

	private static boolean fullyVisibleInViewportOnEdt(Component component) throws Exception {
		AtomicReference<Boolean> visible = new AtomicReference<>(false);
		SwingUtilities.invokeAndWait(() -> visible.set(fullyVisibleInViewport(component)));
		return visible.get();
	}

	private static boolean scrollIntoRibbonViewport(Robot robot, Component root, String commandId) throws Exception {
		return scrollComponentIntoRibbonViewport(robot, root, null, commandId);
	}

	private static boolean scrollComponentIntoRibbonViewport(Robot robot, Component root, Component requiredTarget,
			String commandId) throws Exception {
		for (int attempt = 0; attempt < 32; attempt++) {
			AtomicReference<AbstractButton> target = new AtomicReference<>();
			AtomicReference<JViewport> viewport = new AtomicReference<>();
			AtomicReference<AbstractButton> arrow = new AtomicReference<>();
			AtomicReference<Boolean> fullyVisible = new AtomicReference<>(false);
			SwingUtilities.invokeAndWait(() -> {
				for (Component component : UiComponentWalker.flatten(root)) {
					if (!(component instanceof AbstractButton button)
						|| (requiredTarget == null ? !commandId.equals(buttonId(button)) : component != requiredTarget)) continue;
					target.set(button);
					Container parent = button.getParent();
					while (parent != null && !(parent instanceof JViewport)) parent = parent.getParent();
					if (!(parent instanceof JViewport candidate) || candidate.getView() == null) return;
					viewport.set(candidate);
					Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), candidate.getView());
					Rectangle visible = candidate.getViewRect();
					if (visible.contains(bounds) && fullyInsideUsableScreen(button)) {
						fullyVisible.set(true);
						return;
					}
					String property = scrollProperty(button, bounds, visible);
					for (Component candidateButton : UiComponentWalker.flatten(root)) {
						if (candidateButton instanceof AbstractButton scrollButton && scrollButton.isShowing()
							&& scrollButton instanceof JComponent swingButton
							&& Boolean.TRUE.equals(swingButton.getClientProperty(property))) {
							arrow.set(scrollButton);
							break;
						}
					}
					return;
				}
			});
			if (target.get() == null) {
				if (requiredTarget == null) return false;
				throw new AssertionError("Ribbon proxy disappeared before it could be scrolled into view: " + commandId);
			}
			if (fullyVisible.get()) return true;
			if (viewport.get() == null || arrow.get() == null)
				throw new AssertionError("Ribbon command is outside its viewport and no visible scroll arrow can reach it: " + commandId);
			Rectangle before = viewportPosition(viewport.get());
			WindowAcceptanceAssertions.clickComponent(robot, arrow.get());
			Rectangle after = viewportPosition(viewport.get());
			if (before.equals(after))
				throw new AssertionError("Ribbon viewport scroll arrow did not move toward command: " + commandId);
		}
		throw new AssertionError("Ribbon viewport did not fully expose command after bounded physical scrolling: " + commandId);
	}

	private static Rectangle viewportPosition(JViewport viewport) throws Exception {
		Rectangle[] position = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> position[0] = new Rectangle(viewport.getViewPosition(), viewport.getExtentSize()));
		return position[0];
	}

	private static boolean fullyVisibleInViewport(Component component) {
		Container parent = component.getParent();
		while (parent != null && !(parent instanceof JViewport)) parent = parent.getParent();
		if (!(parent instanceof JViewport viewport) || viewport.getView() == null) return true;
		Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), viewport.getView());
		return viewport.getViewRect().contains(bounds) && fullyInsideUsableScreen(component);
	}

	private static boolean fullyInsideUsableScreen(Component component) {
		if (!component.isShowing()) return false;
		Rectangle screenBounds = new Rectangle(component.getLocationOnScreen(), component.getSize());
		GraphicsConfiguration configuration = component.getGraphicsConfiguration();
		if (configuration == null) return false;
		Rectangle usable = new Rectangle(configuration.getBounds());
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		usable.x += insets.left;
		usable.y += insets.top;
		usable.width -= insets.left + insets.right;
		usable.height -= insets.top + insets.bottom;
		return usable.contains(screenBounds);
	}

	private static String scrollProperty(Component target, Rectangle targetInView, Rectangle viewRect) {
		Rectangle screenBounds = new Rectangle(target.getLocationOnScreen(), target.getSize());
		GraphicsConfiguration configuration = target.getGraphicsConfiguration();
		if (configuration != null) {
			Rectangle usable = new Rectangle(configuration.getBounds());
			Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
			usable.x += insets.left;
			usable.y += insets.top;
			usable.width -= insets.left + insets.right;
			usable.height -= insets.top + insets.bottom;
			if (screenBounds.x < usable.x) return ModernRibbonPanel.SCROLL_PREVIOUS_PROPERTY;
			if (screenBounds.x + screenBounds.width > usable.x + usable.width)
				return ModernRibbonPanel.SCROLL_NEXT_PROPERTY;
		}
		return targetInView.x < viewRect.x ? ModernRibbonPanel.SCROLL_PREVIOUS_PROPERTY
			: ModernRibbonPanel.SCROLL_NEXT_PROPERTY;
	}

	private static AbstractButton visibleCommand(Component root, String commandId) throws Exception {
		AtomicReference<AbstractButton> result = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			for (Component component : UiComponentWalker.flatten(root)) {
				if (component instanceof AbstractButton button && button.isShowing() && commandId.equals(buttonId(button))) {
					result.set(button);
					return;
				}
			}
		});
		if (result.get() == null) throw new AssertionError("Ribbon command did not remain visible after viewport scroll: " + commandId);
		return result.get();
	}

	private static AbstractButton visiblePopupCommand(JPopupMenu popup, String commandId) {
		AtomicReference<AbstractButton> current = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (!popup.isVisible()) return;
				for (Component component : UiComponentWalker.flatten(popup)) {
					if (component instanceof AbstractButton button && button.isShowing()
						&& commandId.equals(buttonId(button))) {
						current.set(button);
						return;
					}
				}
			});
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while reacquiring visible ribbon overflow command " + commandId, interrupted);
		} catch (Exception failure) {
			throw new AssertionError("Could not reacquire visible ribbon overflow command " + commandId, failure);
		}
		return current.get();
	}

	private static Rectangle onScreenBounds(Component component) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	public static Component findCommand(Component root, String commandId) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && commandId.equals(buttonId(button))) return button;
			if (component instanceof JComponent swingComponent) {
				Object value = swingComponent.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
				if (value instanceof JPopupMenu popup) {
					AbstractButton command = findSwingCommand(popup, commandId);
					if (command != null) return command;
				}
			}
		}
		return null;
	}

	private static AbstractButton findSwingCommand(Component root, String commandId) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && commandId.equals(buttonId(button))) return button;
		}
		return null;
	}

	private static String buttonId(AbstractButton button) {
		String actionCommand = button.getActionCommand();
		return actionCommand == null || actionCommand.isBlank() ? button.getName() : actionCommand;
	}
}
