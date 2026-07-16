package com.projectlibre1.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import javax.swing.JEditorPane;

import org.junit.jupiter.api.Test;

class LicenseDialogResourceTest {
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
		assertTrue(license.startsWith("PROJECTLIBRE LICENSE"));
		assertTrue(license.contains("Common Public Attribution License"));
		assertFalse(license.contains("Attibution"));
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
		assertNotNull(resource, resourceName + " must be packaged with projectlibre_ui.jar");
		try (InputStream stream = resource.openStream()) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void assertEditorPaneCanLoad(String resourceName) throws Exception {
		URL resource = LicenseDialog.class.getClassLoader().getResource(resourceName);
		assertNotNull(resource, resourceName + " must be packaged with projectlibre_ui.jar");
		assertNotNull(new JEditorPane(resource).getDocument());
	}
}
