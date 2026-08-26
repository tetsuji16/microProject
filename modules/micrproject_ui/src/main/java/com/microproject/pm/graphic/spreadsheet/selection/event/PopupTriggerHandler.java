/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.spreadsheet.selection.event;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

/**
 * Shared mouse-handler base for spreadsheet surfaces that raise a context
 * popup. Encapsulates the duplicated {@code popupShown} flag and the
 * press/release popup-trigger dedupe so each surface only implements
 * {@link #showPopup(MouseEvent)}.
 *
 * <p>The pattern (present in the task table, row header, and column header
 * handlers before consolidation) is: a right-click/popup-trigger shows the
 * popup exactly once per gesture — on press if the platform raises the
 * trigger there, otherwise on release — and never twice.
 */
public abstract class PopupTriggerHandler extends MouseAdapter {
	protected boolean popupShown;

	@Override
	public void mousePressed(MouseEvent event) {
		popupShown = showPopup(event);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (!popupShown) showPopup(event);
		popupShown = false;
	}

	/** Returns true if a popup was shown for this event. */
	protected abstract boolean showPopup(MouseEvent event);

	/** True for a right-click or platform popup-trigger gesture. */
	protected static boolean isPopupTrigger(MouseEvent event) {
		return event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event);
	}
}
