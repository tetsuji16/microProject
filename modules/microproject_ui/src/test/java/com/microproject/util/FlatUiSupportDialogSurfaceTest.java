/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class FlatUiSupportDialogSurfaceTest {
	@Test
	void stylesDefaultPanelsButPreservesSemanticCanvasSurfaces() throws Exception {
		EventQueue.invokeAndWait(() -> {
			JPanel root = new JPanel(new FlowLayout());
			JPanel standardPanel = new JPanel();
			JPanel canvas = new JPanel();
			Color canvasSurface = new Color(0x123456);
			canvas.setBackground(canvasSurface);
			root.add(standardPanel);
			root.add(canvas);

			FlatUiSupport.styleDialogComponents(root);

			assertEquals(FlatUiSupport.panelBackground(), standardPanel.getBackground());
			assertEquals(canvasSurface, canvas.getBackground(),
				"common dialog styling must preserve a canvas's explicit semantic surface");
			assertEquals(FlatUiSupport.panelBackground(), root.getBackground(),
				"the root panel should be styled when it begins with the Look-and-Feel default");
		});
	}
}
