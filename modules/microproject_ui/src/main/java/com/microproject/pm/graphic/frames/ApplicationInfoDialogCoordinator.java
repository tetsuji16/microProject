/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Frame;

import com.microproject.dialog.AboutDialog;
import com.microproject.dialog.HelpDialog;

/** Owns reusable application-level About and Help dialog presentation. */
final class ApplicationInfoDialogCoordinator {
	private AboutDialog aboutDialog;
	private HelpDialog helpDialog;

	void showAbout(Frame owner) {
		if (aboutDialog == null) {
			aboutDialog = AboutDialog.getInstance(owner);
			aboutDialog.pack();
			aboutDialog.setModal(true);
		}
		aboutDialog.setLocationRelativeTo(owner);
		aboutDialog.setVisible(true);
	}

	void showHelp(Frame owner) {
		if (helpDialog == null) {
			helpDialog = HelpDialog.getInstance(owner);
			helpDialog.pack();
			helpDialog.setModal(true);
		}
		helpDialog.setLocationRelativeTo(owner);
		helpDialog.setVisible(true);
	}
}
