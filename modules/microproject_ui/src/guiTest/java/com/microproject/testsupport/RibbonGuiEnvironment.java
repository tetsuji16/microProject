/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;

/** Applies the production ribbon look-and-feel baseline to GUI acceptance fixtures. */
public final class RibbonGuiEnvironment {
	private RibbonGuiEnvironment() {
	}

	/** Initializes the same theme and ribbon flags used by the desktop application. */
	public static void initialize() {
		FlatLafSupport.initialize();
		if (!FlatLafSupport.isFlatLafLookAndFeel()) {
			throw new IllegalStateException("Ribbon GUI acceptance tests must run with the production FlatLaf theme.");
		}
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
	}
}
