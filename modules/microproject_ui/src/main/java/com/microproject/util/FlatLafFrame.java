/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import javax.swing.JFrame;

/**
 * Application frame whose Swing root pane is created after FlatLaf is active.
 */
public class FlatLafFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	static {
		FlatLafSupport.ensureInitialized();
	}

	public FlatLafFrame() {
		super();
	}

	public FlatLafFrame(String title) {
		super(title);
	}
}
