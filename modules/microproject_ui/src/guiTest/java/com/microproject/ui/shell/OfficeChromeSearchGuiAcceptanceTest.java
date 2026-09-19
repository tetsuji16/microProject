/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Verifies the title-bar search hit area using a realized Swing window. */
class OfficeChromeSearchGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
	}

	@Test
	void clickingTheSearchBoxMarginFocusesTheSearchField() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for this acceptance test.");
		final OfficeChromePanel[] panel = new OfficeChromePanel[1];
		final JTextField[] field = new JTextField[1];
		final Component[] box = new Component[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new OfficeChromePanel(MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> { });
			field[0] = find(panel[0], OfficeChromePanel.SEARCH_FIELD_NAME, JTextField.class);
			box[0] = find(panel[0], OfficeChromePanel.SEARCH_BOX_NAME, Component.class);
			frame = new JFrame("Office chrome search acceptance");
			frame.add(panel[0]);
			frame.setSize(900, 180);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			frame.toFront();
		});

		SwingUtilities.invokeAndWait(() -> box[0].dispatchEvent(new MouseEvent(
			box[0], MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1)));
		GuiAcceptanceSupport.await(field[0]::isFocusOwner, "search box margin did not focus the text field");
		SwingUtilities.invokeAndWait(() -> field[0].dispatchEvent(new KeyEvent(
			field[0], KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'x')));
		GuiAcceptanceSupport.await(() -> "x".equals(field[0].getText()), "focused search field did not accept typed text");
	}

	private static <T extends Component> T find(Component root, String name, Class<T> type) {
		if (type.isInstance(root) && name.equals(root.getName()))
			return type.cast(root);
		if (root instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				try {
					return find(child, name, type);
				} catch (IllegalArgumentException ignored) {
					// Continue searching siblings.
				}
			}
		}
		throw new IllegalArgumentException("Component not found: " + name);
	}

}
