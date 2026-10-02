/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.testsupport;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.popup.PopupPanelManager;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;

import com.microproject.menu.testsupport.UiComponentWalker;

/** Robot helpers for commands that Flamingo places in a collapsed ribbon band. */
public final class RibbonGuiSupport {
	private RibbonGuiSupport() { }

	public static AbstractButton findVisibleOrExpand(Robot robot, Component root, String commandId) throws Exception {
		AtomicReference<AbstractButton> visible = new AtomicReference<>();
		AtomicReference<AbstractCommandButton> collapsedTrigger = new AtomicReference<>();
		AtomicReference<AbstractCommandButton> popupCommand = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			for (Component component : UiComponentWalker.flatten(root)) {
				AbstractButton button = adaptIfCommand(component);
				if (button != null && button.isShowing() && commandId.equals(button.getActionCommand())) {
					visible.set(button);
					return;
				}
			}
			for (PopupPanelManager.PopupInfo popup : PopupPanelManager.defaultManager().getShownPath()) {
				AbstractCommandButton command = findShowingCommand(popup.getPopupPanel(), commandId);
				if (command != null) {
					visible.set(RibbonGuiButton.adapt(command));
					return;
				}
			}
			for (Component component : UiComponentWalker.flatten(root)) {
				if (!(component instanceof JRibbonBand band)) continue;
				if (band.getPopupRibbonBand() == null) continue;
				AbstractCommandButton command = findCommand(band.getPopupRibbonBand(), commandId);
				if (command == null) continue;
				for (Component child : band.getComponents()) {
					if (child instanceof AbstractCommandButton button && button.isShowing()
						&& !commandId.equals(button.getName())) {
						collapsedTrigger.set(button);
						popupCommand.set(command);
						return;
					}
				}
			}
		});
		if (visible.get() != null) return visible.get();
		if (collapsedTrigger.get() == null)
			throw new AssertionError("Ribbon command is neither visible nor in a collapsed Flamingo band: " + commandId);

		Rectangle bounds = onScreenBounds(collapsedTrigger.get());
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> {
			AtomicReference<AbstractCommandButton> showing = new AtomicReference<>();
			try {
				SwingUtilities.invokeAndWait(() -> {
				for (PopupPanelManager.PopupInfo popup : PopupPanelManager.defaultManager().getShownPath()) {
					AbstractCommandButton command = findShowingCommand(popup.getPopupPanel(), commandId);
					if (command != null) {
						showing.set(command);
						break;
					}
				}
				});
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupted while inspecting ribbon popup", interrupted);
			} catch (java.lang.reflect.InvocationTargetException failure) {
				throw new AssertionError("Could not inspect ribbon popup", failure.getCause());
			}
			popupCommand.set(showing.get());
			return popupCommand.get() != null;
		},
			"Flamingo collapsed band did not expose " + commandId + " after its expand control was clicked");
		return RibbonGuiButton.adapt(popupCommand.get());
	}

	private static Rectangle onScreenBounds(Component component) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	private static AbstractCommandButton findCommand(Component root, String commandId) {
		for (Component component : UiComponentWalker.flatten(root))
			if (component instanceof AbstractCommandButton button && commandId.equals(button.getName())) return button;
		return null;
	}

	private static AbstractCommandButton findShowingCommand(Component root, String commandId) {
		for (Component component : UiComponentWalker.flatten(root))
			if (component instanceof AbstractCommandButton button && button.isShowing()
				&& commandId.equals(button.getName())) return button;
		return null;
	}

	private static AbstractButton adaptIfCommand(Component component) {
		if (component instanceof AbstractButton button) return button;
		if (component instanceof AbstractCommandButton) return RibbonGuiButton.adapt(component);
		return null;
	}
}
