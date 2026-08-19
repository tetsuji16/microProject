/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import javax.swing.JEditorPane;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

class LicenseDialogResourceTest {
	@Test
	void dialogTitleDoesNotRepeatApplicationName() {
		assertEquals("ProjectLibre License",
			LicenseDialog.buildDialogTitle("ProjectLibre", "ProjectLibre License"));
		assertEquals("ProjectLibre ライセンス",
			LicenseDialog.buildDialogTitle("ProjectLibre", "ライセンス"));
	}

	@Test
	void licenseHtmlUsesTheStandardUiFont() {
		JEditorPane pane = new JEditorPane();

		LicenseDialog.configureReadableHtml(pane);

		assertEquals(Boolean.TRUE, pane.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES));
		assertEquals(UIManager.getFont("Label.font"), pane.getFont());
	}

	@Test
	void licenseDialogResourcesLoadWithoutAcceptedLicensePreference() throws Exception {
		Preferences preferences = Preferences.userNodeForPackage(LicenseDialog.class);
		preferences.remove("validatedLicense");
		preferences.remove("licenseValidationDate");
		preferences.flush();

		assertEditorPaneCanLoad("license/index.html");
		assertEditorPaneCanLoad("license/third-party/index.html");
		assertInstallerLicenseCopyIsReadable();
		assertCurrentOpenSourceNotices();
	}

	private void assertInstallerLicenseCopyIsReadable() throws Exception {
		String license = readResource("license/license.txt");
		assertTrue(license.startsWith("MICROPROJECT LICENSE"));
		assertTrue(license.contains("Common Public Attribution License"));
		assertFalse(license.contains("Attibution"));

		String licenseHtml = readResource("license/index.html");
		assertFalse(licenseHtml.contains("FONT SIZE=2"));
		assertTrue(licenseHtml.contains("FONT SIZE=3"));
	}

	private void assertCurrentOpenSourceNotices() throws Exception {
		String notices = readResource("license/third-party/index.html");
		assertTrue(notices.contains("Open Source Software Notices"));
		assertTrue(notices.contains("FlatLaf"));
		assertTrue(notices.contains("Jackson"));
		assertTrue(notices.contains("MPXJ"));
		assertFalse(notices.contains("Apache ANT"));
		assertFalse(notices.contains("JUnit"));
	}

	private String readResource(String resourceName) throws Exception {
		URL resource = LicenseDialog.class.getClassLoader().getResource(resourceName);
		assertNotNull(resource, resourceName + " must be packaged with micrproject_ui.jar");
		try (InputStream stream = resource.openStream()) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void assertEditorPaneCanLoad(String resourceName) throws Exception {
		URL resource = LicenseDialog.class.getClassLoader().getResource(resourceName);
		assertNotNull(resource, resourceName + " must be packaged with micrproject_ui.jar");
		assertNotNull(new JEditorPane(resource).getDocument());
	}
}
