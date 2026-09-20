/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

/** Routes a physical click to a button when native modal hit testing bypasses Swing. */
public final class PhysicalButtonRoute {
	private PhysicalButtonRoute() { }

	public static void install(Window owner, AbstractButton button, Runnable action, java.util.function.BooleanSupplier handled) {
		AWTEventListener listener = event -> {
			if (!(event instanceof MouseEvent mouse) || mouse.getID() != MouseEvent.MOUSE_RELEASED
				|| !SwingUtilities.isLeftMouseButton(mouse) || !button.isShowing() || !button.isEnabled() || handled.getAsBoolean()) return;
			if (!(mouse.getSource() instanceof Component source)
				|| SwingUtilities.getWindowAncestor(source) != owner) return;
			try {
				Point screen = mouse.getLocationOnScreen();
				Point buttonScreen = button.getLocationOnScreen();
				if (screen.x < buttonScreen.x || screen.y < buttonScreen.y
					|| screen.x >= buttonScreen.x + button.getWidth() || screen.y >= buttonScreen.y + button.getHeight()) return;
				SwingUtilities.invokeLater(action);
			} catch (java.awt.IllegalComponentStateException ignored) {
				// The owner may have been disposed between the native release and
				// Swing's event dispatch; the normal button route already completed.
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);
		owner.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed(WindowEvent event) {
				Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
			}
		});
	}
}
