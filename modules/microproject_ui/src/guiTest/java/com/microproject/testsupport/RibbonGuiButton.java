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
		return new RibbonButton(commandButton);
	}

	public static Component component(AbstractButton button) {
		return button instanceof RibbonButton ribbonButton ? ribbonButton.delegate : button;
	}

	public static Component findCommand(Component root, String commandId) {
		return RibbonGuiSupport.findCommand(root, commandId);
	}

	private static final class RibbonButton extends JToggleButton {
		private final AbstractCommandButton delegate;
		private RibbonButton(AbstractCommandButton delegate) {
			super();
			this.delegate = delegate;
			setText(delegate.getText());
		}
		@Override public javax.accessibility.AccessibleContext getAccessibleContext() {
			if (delegate == null) return super.getAccessibleContext();
			javax.accessibility.AccessibleContext context = delegate.getAccessibleContext();
			return context == null ? super.getAccessibleContext() : context;
		}
		@Override public boolean isShowing() { return delegate == null ? super.isShowing() : delegate.isShowing(); }
		@Override public boolean isEnabled() { return delegate == null ? super.isEnabled() : delegate.isEnabled(); }
		@Override public int getWidth() { return delegate == null ? super.getWidth() : delegate.getWidth(); }
		@Override public int getHeight() { return delegate == null ? super.getHeight() : delegate.getHeight(); }
		@Override public java.awt.Dimension getSize() { return delegate == null ? super.getSize() : delegate.getSize(); }
		@Override public Point getLocationOnScreen() { return delegate == null ? super.getLocationOnScreen() : delegate.getLocationOnScreen(); }
		@Override public String getText() { return delegate == null ? super.getText() : delegate.getText(); }
		@Override public String getActionCommand() { return delegate == null ? super.getActionCommand() : delegate.getName(); }
		@Override public boolean isSelected() {
			if (delegate == null) return super.isSelected();
			if (delegate instanceof JCommandToggleButton toggle) return toggle.getActionModel().isSelected();
			JRibbon ribbon = (JRibbon) SwingUtilities.getAncestorOfClass(JRibbon.class, delegate);
			return ribbon != null && ribbon.getSelectedTask() != null
				&& ribbon.getSelectedTask().getTitle().equals(delegate.getText());
		}
	}
}
