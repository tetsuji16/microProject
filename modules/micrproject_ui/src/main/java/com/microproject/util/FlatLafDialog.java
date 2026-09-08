/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;

/**
 * Application dialog whose root pane is created after FlatLaf is active.
 */
public class FlatLafDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	static {
		FlatLafSupport.ensureInitialized();
	}

	public FlatLafDialog() {
		super();
	}

	public FlatLafDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
	}

	public FlatLafDialog(Window owner, String title, Dialog.ModalityType modalityType) {
		super(owner, title, modalityType);
	}
}
