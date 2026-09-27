/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.microproject.dialog.ResourceMappingDialog;
import com.microproject.exchange.ResourceMappingForm;

/** Owns the reusable modal resource mapping dialog lifecycle. */
final class ResourceMappingDialogCoordinator {
	private ResourceMappingDialog dialog;

	boolean show(ResourceMappingForm form, Component relativeTo) {
		if (dialog == null) {
			dialog = ResourceMappingDialog.getInstance(form);
			dialog.pack();
			dialog.setModal(true);
		} else {
			dialog.setForm(form);
		}
		dialog.bind(true);
		dialog.setLocationRelativeTo(relativeTo);
		dialog.setVisible(true);
		return dialog.getDialogResult() == JOptionPane.OK_OPTION;
	}
}
