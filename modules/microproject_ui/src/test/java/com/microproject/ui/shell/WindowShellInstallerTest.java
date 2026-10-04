/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JRootPane;

import org.junit.jupiter.api.Test;

import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

class WindowShellInstallerTest {
	@Test
	void officeRibbonShellUsesFlatLafWindowDecorationContract() {
		JRootPane rootPane = new JRootPane();
		WindowShellInstaller.installOfficeRibbonShell(rootPane, true);

		assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
		assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.FULL_WINDOW_CONTENT));
		assertEquals(FlatUiSupport.ribbonChromeHeight(),
			rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_HEIGHT));
		assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_ICON));
		assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_TITLE));
	}

	@Test
	void unsupportedNativeShellRetainsPlatformTitleAndIcon() {
		JRootPane rootPane = new JRootPane();
		WindowShellInstaller.installOfficeRibbonShell(rootPane, false);

		if (Environment.isWindows()) {
			assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
		} else {
			assertEquals(null, rootPane.getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
		}
		assertEquals(Boolean.FALSE, rootPane.getClientProperty(WindowShellInstaller.FULL_WINDOW_CONTENT));
		assertNull(rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_HEIGHT));
		assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_ICON));
		assertEquals(Boolean.TRUE, rootPane.getClientProperty(WindowShellInstaller.TITLE_BAR_SHOW_TITLE));
	}
}
