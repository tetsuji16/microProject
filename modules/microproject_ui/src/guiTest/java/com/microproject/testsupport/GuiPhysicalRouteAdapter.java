/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import javax.swing.AbstractButton;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;

/** Locates physical Swing routes without dispatching actions directly. */
public final class GuiPhysicalRouteAdapter {
	private GuiPhysicalRouteAdapter() {
	}

	public static JMenuItem visibleMenuItem(Container root, String actionCommand) {
		try {
			return find(root, JMenuItem.class, actionCommand);
		} catch (AssertionError absentFromBar) {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof JMenuItem item && item.isShowing()
						&& (Objects.equals(actionCommand, item.getActionCommand())
							|| Objects.equals(actionCommand, item.getName())))
					return item;
			}
			throw absentFromBar;
		}
	}

	public static JMenuItem visiblePopupItem(JPopupMenu popup, String actionCommand) {
		Objects.requireNonNull(popup, "popup");
		return find(popup, JMenuItem.class, actionCommand);
	}

	public static AbstractButton visibleButton(Container root, String actionCommand) {
		for (Component component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && button.isShowing()
				&& (Objects.equals(actionCommand, button.getActionCommand()) || Objects.equals(actionCommand, button.getName())))
				return button;
			if (component instanceof AbstractCommandButton button && button.isShowing()
				&& Objects.equals(actionCommand, button.getName())) return RibbonGuiButton.adapt(button);
		}
		throw new AssertionError("Visible physical route is absent: " + actionCommand);
	}

	public static AbstractButton visibleButton(Robot robot, Container root, String actionCommand) throws Exception {
		return RibbonGuiSupport.findVisibleOrExpand(robot, root, actionCommand);
	}

	public static void assertRootPaneBinding(JComponent root, KeyStroke keyStroke, String actionKey) {
		InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		assertTrue(Objects.equals(actionKey, inputMap.get(keyStroke)),
			() -> "root-pane shortcut is not canonical: " + keyStroke + " -> " + inputMap.get(keyStroke));
	}

	public static void awaitVisible(BooleanSupplier visible, String message) throws Exception {
		GuiAcceptanceSupport.await(visible, message);
	}

	private static <T extends Component> T find(Container root, Class<T> type, String actionCommand) {
		for (Component component : root.getComponents()) {
			if (type.isInstance(component) && component.isShowing()
					&& (Objects.equals(actionCommand, ((AbstractButton) component).getActionCommand())
							|| Objects.equals(actionCommand, component.getName())))
				return type.cast(component);
			if (component instanceof Container child) {
				try {
					return find(child, type, actionCommand);
				} catch (AssertionError ignored) {
					// Continue searching sibling containers.
				}
			}
		}
		throw new AssertionError("Visible physical route is absent: " + actionCommand);
	}
}
