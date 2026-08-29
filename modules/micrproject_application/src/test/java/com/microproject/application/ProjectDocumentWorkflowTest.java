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
package com.microproject.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.job.JobQueue;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.session.SessionFactory;

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

	@Test
	void normalSaveRunsTheCompletionCallbackThatClearsTheDocumentDirtyState() {
		Project project = ProjectFactory.getInstance().createProject();
		AtomicInteger completions = new AtomicInteger();
		ProjectDocumentWorkflow.SaveCallbacks callbacks = new ProjectDocumentWorkflow.SaveCallbacks() {
			@Override public void persistWorkspace(Project ignored) { }
			@Override public int resolveSaveDecision(Project ignored, com.microproject.collaboration.CollaborationSession session) { return 0; }
			@Override public String chooseSaveAsCopyFileName(Project ignored) { return null; }
			@Override public void afterSave(Project saved, boolean saveAs, boolean fileNameChanged, boolean collaborationEnabled) {
				completions.incrementAndGet();
			}
		};

		SaveOptions options = ProjectDocumentWorkflow.prepareSaveOptions(project, "plan.pod", false, true, null, "user", null, callbacks);

		assertNotNull(options.getPostSaving());
		options.getPostSaving().accept(null);
		assertEquals(1, completions.get(), "ordinary Save must run the same completion path as Save As");
	}
}
