/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import javax.swing.JComponent;

/** Window-level host for the Backstage view, which replaces the document area. */
public interface RibbonBackstageHost {
	void show(JComponent backstageView, Runnable dismiss);
	void hide();
}
