/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Window;

import com.microproject.dialog.assignment.AssignmentDialog;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;

/** Owns the reusable assignment dialog's presentation lifecycle. */
final class AssignmentDialogCoordinator {
	private AssignmentDialog dialog;

	void show(DocumentFrame documentFrame) {
		if (dialog == null) {
			dialog = new AssignmentDialog(documentFrame);
			dialog.pack();
			dialog.setModal(false);
		}
		dialog.setLocationRelativeTo(documentFrame);
		dialog.setVisible(true);
	}

	void selectionChanged(SelectionNodeEvent event) {
		if (dialog != null)
			dialog.selectionChanged(event);
	}

	void windowClosed(Window window) {
		if (window == dialog)
			dialog = null;
	}
}
