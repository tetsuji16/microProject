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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.session.SaveOptions;
import com.microproject.undo.DataFactoryUndoController;

class ProjectFactoryClosingTest {
	@Test
	void loaderFailuresKeepInvalidFilesDistinctFromMissingAndAccessDeniedReferences() throws Exception {
		assertEquals(SubProj.LoadStatus.INVALID, ProjectFactory.subprojectLoadFailureStatus(new IOException("invalid archive")));
		assertEquals(SubProj.LoadStatus.MISSING, ProjectFactory.subprojectLoadFailureStatus(new FileNotFoundException("moved.mpo")));
		assertEquals(SubProj.LoadStatus.ACCESS_DENIED,
			ProjectFactory.subprojectLoadFailureStatus(new AccessDeniedException("locked.mpo")));
	}

	@Test
	void loaderFailureDetailsAreSafeForOneLineWarnings() {
		assertEquals(" Details: broken archive.", ProjectFactory.failureDetail(new IOException("broken\narchive")));
	}

	@Test
	void standaloneLoadFailuresNameTheFileAndPreserveExistingDocuments() {
		String message = ProjectFactory.projectLoadFailureMessage("C:/plans/broken.mpo", new IOException("bad archive"));
		assertTrue(message.contains("C:/plans/broken.mpo"));
		assertTrue(message.contains("invalid or could not be imported"));
		assertTrue(message.contains("already open were not changed"));
	}

	@Test
	void savePromptUsesTheExistingFileNameBeforeTheProjectInternalName() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("default-format", undo), undo);
		project.setName("CCPM Visualization Sample");
		project.setFileName("C:\\projects\\CCPM sample English.mpo");

		assertEquals("CCPM sample English.mpo", ProjectFactory.getDisplayNameForSavePrompt(project));
	}

	@Test
	void savePromptUsesTheProjectNameForAnUnsavedProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("default-format", undo), undo);
		project.setName("New project");

		assertEquals("New project", ProjectFactory.getDisplayNameForSavePrompt(project));
	}

	@Test
	void masterSaveIncludesADirtyChildEvenWhenTheMasterItselfIsClean() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("master pool", undo), undo);
		Project child = Project.createProject(ResourcePool.createRourcePool("child pool", undo), undo);
		master.setLocal(false);
		child.setLocal(false);
		master.setDirty(false);
		master.setGroupDirty(false);
		child.setDirty(true);
		SaveOptions options = new SaveOptions();

		assertFalse(master.needsSaving());
		assertTrue(child.needsSaving());
		assertFalse(ProjectFactory.shouldIncludeInBranchSave(master, options));
		assertTrue(ProjectFactory.shouldIncludeInBranchSave(child, options));
	}

	@Test
	void closeCallbacksRunOnlyAfterTheProjectLeavesTheClosingState() {
		ProjectFactory factory = ProjectFactory.createInstance();
		long projectId = 4242L;
		List<String> events = new ArrayList<>();

		factory.addClosingProject(projectId);
		factory.runAfterProjectClosed(projectId, () -> events.add("first"));
		factory.runAfterProjectClosed(projectId, () -> events.add("second"));

		assertTrue(factory.isProjectClosing(projectId));
		assertTrue(events.isEmpty());

		factory.completeProjectClosing(projectId);

		assertFalse(factory.isProjectClosing(projectId));
		assertEquals(List.of("first", "second"), events);
	}

	@Test
	void closeCallbackRunsImmediatelyWhenNoCloseIsPending() {
		ProjectFactory factory = ProjectFactory.createInstance();
		List<String> events = new ArrayList<>();

		factory.runAfterProjectClosed(5252L, () -> events.add("now"));

		assertEquals(List.of("now"), events);
	}

	@Test
	void completingABranchClearsEveryClosingIdBeforeCallbacksRun() {
		ProjectFactory factory = ProjectFactory.createInstance();
		Set<Long> ids = Set.of(6101L, 6102L);
		List<Boolean> childStillClosing = new ArrayList<>();
		factory.addClosingProjects(ids);
		factory.runAfterProjectClosed(6101L,
				() -> childStillClosing.add(factory.isProjectClosing(6102L)));

		factory.completeProjectClosings(ids);

		assertFalse(factory.isProjectClosing(6101L));
		assertFalse(factory.isProjectClosing(6102L));
		assertEquals(List.of(false), childStillClosing);
	}
}
