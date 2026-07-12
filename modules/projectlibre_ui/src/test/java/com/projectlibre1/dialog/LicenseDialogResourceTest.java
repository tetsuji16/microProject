package com.projectlibre1.dialog;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
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
	}

	private void assertEditorPaneCanLoad(String resourceName) throws Exception {
		URL resource = LicenseDialog.class.getClassLoader().getResource(resourceName);
		assertNotNull(resource, resourceName + " must be packaged with projectlibre_ui.jar");
		assertNotNull(new JEditorPane(resource).getDocument());
	}
}
