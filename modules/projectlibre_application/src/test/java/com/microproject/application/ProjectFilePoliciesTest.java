package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;

class ProjectFilePoliciesTest {
	@Test
	void resolvesImporterForProjectLibreLoadWhenLocalOnly() {
		assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.pod", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xml", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xlsx", true));
	}

	@Test
	void resolvesImporterForProjectLibreLoadWhenServerBacked() {
		assertEquals(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.pod", false));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xml", false));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xlsx", false));
	}

	@Test
	void resolvesMicrosoftImporterForNonProjectLibreFiles() {
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.mpp", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.mpp", false));
		assertFalse(ProjectFilePolicies.isProjectLibreFile("plan.mpp"));
		assertTrue(ProjectFilePolicies.isProjectLibreFile("plan.pod"));
	}

	@Test
	void configuresLoadAndSaveOptions() {
		LoadOptions loadOptions = new LoadOptions();
		ProjectFilePolicies.configureLoadOptions(loadOptions, "plan.pod", false);
		assertEquals("plan.pod", loadOptions.getFileName());
		assertEquals(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, loadOptions.getImporter());

		SaveOptions saveOptions = new SaveOptions();
		ProjectFilePolicies.configureSaveOptions(saveOptions, "plan.mpp");
		assertEquals("plan.mpp", saveOptions.getFileName());
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, saveOptions.getImporter());
	}
}
