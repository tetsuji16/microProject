package com.projectlibre1.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.projectlibre1.job.JobQueue;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.ProjectFactory;
import com.projectlibre1.session.LocalSession;
import com.projectlibre1.session.SaveOptions;
import com.projectlibre1.session.SessionFactory;

class ProjectDocumentWorkflowTest {
	@BeforeEach
	void initializeJobQueue() {
		SessionFactory.getInstance().setJobQueue(new JobQueue("test", false));
	}

	@Test
	void prepareSaveOptionsUsesFilePoliciesWithoutCollaboration() {
		Project project = ProjectFactory.getInstance().createProject();

		SaveOptions options = ProjectDocumentWorkflow.prepareSaveOptions(
			project,
			"plan.pod",
			false,
			true,
			null,
			"user",
			null,
			null);

		assertNotNull(options);
		assertEquals("plan.pod", options.getFileName());
		assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, options.getImporter());
		assertFalse(options.isSaveAs());
	}
}
