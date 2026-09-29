/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import java.awt.event.MouseEvent;

/** Ensures a popup gesture is dispatched once across platform press/release triggers. */
public final class PopupTriggerController {
	private boolean triggeredOnPress;

	public boolean mousePressed(MouseEvent event) {
		triggeredOnPress = event.isPopupTrigger();
		return triggeredOnPress;
	}

	public boolean mouseReleased(MouseEvent event) {
		boolean shouldTrigger = event.isPopupTrigger() && !triggeredOnPress;
		triggeredOnPress = false;
		return shouldTrigger;
	}
}
