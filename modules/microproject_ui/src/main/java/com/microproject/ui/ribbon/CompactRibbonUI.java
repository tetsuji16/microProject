/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import org.pushingpixels.flamingo.internal.ui.ribbon.BasicRibbonUI;

/**
 * Removes Flamingo 5.0's fixed 24 px taskbar strip because the Office chrome
 * already provides the title and quick-access area above the ribbon tabs. All
 * rendering and layout continue to use Flamingo's existing implementation.
 */
final class CompactRibbonUI extends BasicRibbonUI {
	@Override
	public int getTaskbarHeight() {
		return 0;
	}

	@Override
	public int getTaskToggleButtonHeight() {
		return 30;
	}
}
