package com.projectlibre1.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.session.LocalSession;
import com.projectlibre1.session.LoadOptions;

class ProjectLoadWorkflowTest {
	@Test
	void preparesLoadOptionsForProjectLibreCollaborationFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.pod", true, "alice");

		assertEquals("sample.pod", options.getFileName());
		assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, options.getImporter());
		assertTrue(options.isCollaborationEnabled());
		assertEquals("alice", options.getCollaborationUserKey());
		assertTrue(options.getSidecarFileName().endsWith(".projectlibre-sync.json"));
	}

	@Test
	void preparesLoadOptionsForMicrosoftFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.mpp", false, "alice");

		assertEquals("sample.mpp", options.getFileName());
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, options.getImporter());
		assertFalse(options.isCollaborationEnabled());
	}
}
