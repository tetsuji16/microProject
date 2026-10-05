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
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
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
				if (component instanceof AbstractButton button && button.isShowing()
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
					collapsedTrigger.set(button);
					collapsedPopup.set(popup);
					popupCommand.set(command);
					return;
				}
			}
		});
		if (visible.get() != null) return visible.get();
		if (collapsedTrigger.get() == null)
			throw new AssertionError("Ribbon command is neither visible nor in a Swing overflow group: " + commandId);

		Rectangle bounds = onScreenBounds(collapsedTrigger.get());
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> collapsedPopup.get().isVisible()
			&& popupCommand.get().isShowing(),
			"Swing ribbon overflow did not expose " + commandId + " after its physical click");
		return popupCommand.get();
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
