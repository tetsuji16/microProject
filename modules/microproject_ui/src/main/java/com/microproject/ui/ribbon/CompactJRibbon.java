/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import org.pushingpixels.flamingo.api.ribbon.JRibbon;

/** JRibbon using the compact Flamingo delegate for the microProject shell. */
final class CompactJRibbon extends JRibbon {
	private boolean compactUiInstalled;

	CompactJRibbon() {
		compactUiInstalled = true;
		setUI(new CompactRibbonUI());
	}

	@Override
	public void updateUI() {
		if (compactUiInstalled) {
			setUI(new CompactRibbonUI());
			FlamingoRibbonController.styleRibbonSurface(this);
		} else {
			super.updateUI();
		}
	}
}
