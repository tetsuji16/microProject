/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Verifies the Resource Information General tab's shared preferred-row policy (#718). */
class ResourceInformationLayoutGuiAcceptanceTest {
	private ResourceInformationDialog resourceDialog;
	private JFrame host;

	@AfterEach
	void closeWindow() throws Exception {
		if (host != null) SwingUtilities.invokeAndWait(host::dispose);
		if (resourceDialog != null) SwingUtilities.invokeAndWait(resourceDialog::dispose);
	}

	@Test
	void resourceInformationGeneralRowsKeepLocalizedFieldsAtPreferredHeight() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for layout acceptance coverage.");
		SwingUtilities.invokeAndWait(() -> {
			resourceDialog = ResourceInformationDialog.getInstance(null, null);
			host = new JFrame("Resource Information General layout acceptance");
			host.setContentPane(resourceDialog.createGeneralPanel());
			host.pack();
			host.setLocationByPlatform(true);
			host.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> host.isShowing(), "Resource Information General tab did not render");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(host.getContentPane(),
			"Resource Information General tab (#718)");
		DialogLayoutAssertions.assertWithinUsableScreen(host, "Resource Information General host (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(host, host.getContentPane(), 100, 50,
			"Resource Information General host (#724)");
	}
}
