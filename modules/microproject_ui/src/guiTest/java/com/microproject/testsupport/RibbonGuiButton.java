/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.testsupport;

import java.awt.Component;
import java.awt.Point;

import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;

/** Adapts native Flamingo controls to the existing Robot test vocabulary. */
public final class RibbonGuiButton {
	private RibbonGuiButton() { }

	public static AbstractButton adapt(Component component) {
		if (component instanceof AbstractButton button) return button;
		if (!(component instanceof AbstractCommandButton commandButton))
			throw new IllegalArgumentException("Not a ribbon command control: " + component);
		return new JToggleButton(commandButton.getText()) {
			@Override public boolean isShowing() { return commandButton.isShowing(); }
			@Override public boolean isEnabled() { return commandButton.isEnabled(); }
			@Override public int getWidth() { return commandButton.getWidth(); }
			@Override public int getHeight() { return commandButton.getHeight(); }
			@Override public Point getLocationOnScreen() { return commandButton.getLocationOnScreen(); }
			@Override public String getActionCommand() { return commandButton.getName(); }
			@Override public boolean isSelected() {
				if (commandButton instanceof JCommandToggleButton toggle)
					return toggle.getActionModel().isSelected();
				JRibbon ribbon = (JRibbon) SwingUtilities.getAncestorOfClass(JRibbon.class, commandButton);
				return ribbon != null && ribbon.getSelectedTask() != null
					&& ribbon.getSelectedTask().getTitle().equals(commandButton.getText());
			}
		};
	}
}
