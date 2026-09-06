/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JRootPane;
import com.microproject.util.Environment;

import org.junit.jupiter.api.Test;

class WindowShellInstallerTest {
	@Test
	void officeRibbonShellUsesFlatLafWindowDecorationContract() {
		JRootPane rootPane = new JRootPane();
		WindowShellInstaller.installOfficeRibbonShell(rootPane);

		if (Environment.isWindows()) {
			assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
			assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.FULL_WINDOW_CONTENT));
			assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_ICON));
			assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_TITLE));
		} else {
			assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.FULL_WINDOW_CONTENT));
			assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_ICON));
			assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_TITLE));
		}
	}
}
