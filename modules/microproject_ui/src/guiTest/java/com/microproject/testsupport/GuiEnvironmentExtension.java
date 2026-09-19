/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.Toolkit;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Shared last-resort desktop cleanup for every GUI acceptance fixture. */
public final class GuiEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		cleanupDesktop("GUI fixture pre-test cleanup");
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		cleanupDesktop("GUI fixture desktop cleanup");
	}

	private static void cleanupDesktop(String description) throws Exception {
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isDisplayable()) window.dispose();
			}
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
			Toolkit.getDefaultToolkit().sync();
		}, description);
	}
}
