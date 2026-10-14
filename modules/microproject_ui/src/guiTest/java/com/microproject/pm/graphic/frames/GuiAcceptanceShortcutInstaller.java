/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import javax.swing.JComponent;
import javax.swing.RootPaneContainer;

/** Installs the same document-root shortcut layer used by a production window. */
public final class GuiAcceptanceShortcutInstaller {
	private GuiAcceptanceShortcutInstaller() { }

	public static void install(GraphicManager manager, RootPaneContainer window) {
		manager.applyMicrosoftShortcuts(
			window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
			window.getRootPane().getActionMap());
	}
}
