/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.testsupport;

import java.awt.Component;

import javax.swing.AbstractButton;

/** Names the Swing ribbon button boundary used by physical GUI journeys. */
public final class RibbonGuiButton {
	private RibbonGuiButton() { }

	public static AbstractButton adapt(Component component) {
		if (component instanceof AbstractButton button) return button;
		throw new IllegalArgumentException("Not a Swing ribbon command button: " + component);
	}

	public static Component component(AbstractButton button) {
		return button;
	}

	public static Component findCommand(Component root, String commandId) {
		return RibbonGuiSupport.findCommand(root, commandId);
	}
}
