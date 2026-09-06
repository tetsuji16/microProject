/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import javax.swing.JFrame;
import javax.swing.JRootPane;

import com.microproject.util.Environment;

/**
 * Configures the one supported top-level window shell for the Office ribbon.
 *
 * <p>FlatLaf owns the Windows border, caption drag, system menu, and window
 * buttons. The ribbon only extends its content into that title-bar area; it
 * must never replace those operating-system interactions with Swing buttons
 * or a manual drag listener.</p>
 */
public final class WindowShellInstaller {
	static final String USE_WINDOW_DECORATIONS = "JRootPane.useWindowDecorations";
	static final String FULL_WINDOW_CONTENT = "FlatLaf.fullWindowContent";
	static final String TITLE_BAR_SHOW_ICON = "JRootPane.titleBarShowIcon";
	static final String TITLE_BAR_SHOW_TITLE = "JRootPane.titleBarShowTitle";

	private WindowShellInstaller() {
	}

	/**
	 * Applies the FlatLaf full-window-content contract before {@code frame}
	 * becomes displayable. On unsupported platforms FlatLaf ignores these
	 * client properties and the platform decorations remain the fallback.
	 */
	public static void installOfficeRibbonShell(JFrame frame) {
		installOfficeRibbonShell(frame.getRootPane());
	}

	static void installOfficeRibbonShell(JRootPane rootPane) {
		if (Environment.isWindows()) {
			rootPane.putClientProperty(USE_WINDOW_DECORATIONS, Boolean.TRUE);
			rootPane.putClientProperty(FULL_WINDOW_CONTENT, Boolean.TRUE);
			// The brand icon is rendered in the Office header in this mode.
			rootPane.putClientProperty(TITLE_BAR_SHOW_TITLE, Boolean.FALSE);
			rootPane.putClientProperty(TITLE_BAR_SHOW_ICON, Boolean.FALSE);
		} else {
			// Do not hide the platform title/icon on a platform where FlatLaf's
			// Windows full-content decoration is unavailable.
			rootPane.putClientProperty(FULL_WINDOW_CONTENT, Boolean.FALSE);
			rootPane.putClientProperty(TITLE_BAR_SHOW_TITLE, Boolean.TRUE);
			rootPane.putClientProperty(TITLE_BAR_SHOW_ICON, Boolean.TRUE);
		}
	}
}
