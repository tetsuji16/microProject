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
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectEvent;
import com.microproject.pm.task.ProjectListener;
import com.microproject.undo.DataFactoryUndoController;

class LocalSessionSaveTest {
	@TempDir
	Path tempDirectory;

	@Test
	void saveAsCompletesOnEventDispatchThreadAfterEachFileIsCommitted() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("save-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		LocalSession session = new LocalSession();
		session.setJobQueue(new JobQueue("save-test", false));

		Path original = tempDirectory.resolve("original.pod");
		saveAndAwait(session, project, original, false);
		assertEquals(original.toString(), project.getFileName());

		Path copy = tempDirectory.resolve("another.pod");
		saveAndAwait(session, project, copy, true);
		assertEquals(copy.toString(), project.getFileName());
		assertTrue(Files.size(original) > 0L);
		assertTrue(Files.size(copy) > 0L);
	}

	@Test
	void saveClearsDirtyAndNotifiesListenersOnEventDispatchThread() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("save-dirty-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		// Simulate an edit so the project is dirty before saving. Drive the precondition
		// off the model flag directly (needsSaving() also depends on isSavable(), which is
		// environmental and would make the precondition brittle in a headless run).
		project.setGroupDirty(true);
		assertTrue(project.isGroupDirty(), "project must be dirty before save");

		// The title's "*" marker is driven by groupDirtyChanged, not by reading the flag
		// directly. Register a listener so we verify the notification actually reaches a
		// listener on the Swing EDT and reports a clean state -- the exact path issue #20
		// reported broken (UI not updated after Save).
		CountDownLatch notified = new CountDownLatch(1);
		AtomicBoolean notifiedOnEdt = new AtomicBoolean();
		AtomicBoolean notifiedClean = new AtomicBoolean();
		project.addProjectListener(new ProjectListener() {
			@Override
			public void nameChanged(ProjectEvent e) {
			}

			@Override
			public void groupDirtyChanged(ProjectEvent e) {
				notifiedOnEdt.set(SwingUtilities.isEventDispatchThread());
				notifiedClean.set(!e.getProject().isGroupDirty());
				notified.countDown();
			}
		});

		LocalSession session = new LocalSession();
		session.setJobQueue(new JobQueue("save-dirty-test", false));

		Path destination = tempDirectory.resolve("dirty-cleared.pod");
		saveAndAwait(session, project, destination, false);

		// The dirty notification must reach listeners so the UI title can drop the "*".
		assertTrue(notified.await(15, TimeUnit.SECONDS), "groupDirtyChanged was not fired after save");
		assertTrue(notifiedOnEdt.get(), "groupDirtyChanged must be delivered on the Swing event dispatch thread");
		assertTrue(notifiedClean.get(), "groupDirtyChanged must report a clean (non-dirty) project");
		// Model-level post-conditions, independent of the listener path above.
		assertFalse(project.isGroupDirty(), "groupDirty must be cleared after save");
		assertFalse(project.needsSaving(), "needsSaving() must be false after save");
		assertTrue(Files.size(destination) > 0L, "saved file must exist");
	}

	@Test
	void recoverySnapshotDoesNotRenameOrMarkProjectClean() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("recovery-save", undoController), undoController);
		project.setGroupDirty(true);
		LocalSession session = new LocalSession(); session.setJobQueue(new JobQueue("recovery-save", false));
		Path destination = tempDirectory.resolve("snapshot.recovery.pod"); CountDownLatch completed = new CountDownLatch(1);
		SaveOptions options = new SaveOptions(); options.setLocal(true); options.setFileName(destination.toString());
		options.setImporter(LocalSession.LOCAL_PROJECT_IMPORTER); options.setRecoverySnapshot(true); options.setPostSaving(value -> completed.countDown());

		session.getSaveProjectJob(Collections.singletonList(project), options).execute();

		assertTrue(completed.await(15, TimeUnit.SECONDS)); assertTrue(Files.isRegularFile(destination));
		assertTrue(project.isGroupDirty()); assertEquals(null, project.getFileName());
	}

	private static void saveAndAwait(LocalSession session, Project project, Path destination, boolean saveAs)
			throws Exception {
		CountDownLatch completed = new CountDownLatch(1);
		AtomicBoolean callbackOnEdt = new AtomicBoolean();
		AtomicBoolean fileVisibleToCallback = new AtomicBoolean();

		SaveOptions options = new SaveOptions();
		options.setLocal(true);
		options.setSaveAs(saveAs);
		options.setFileName(destination.toString());
		options.setImporter(LocalSession.LOCAL_PROJECT_IMPORTER);
		options.setPostSaving(value -> {
			callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
			fileVisibleToCallback.set(Files.isRegularFile(destination));
			completed.countDown();
		});

		Job job = session.getSaveProjectJob(Collections.singletonList(project), options);
		job.execute();

		assertTrue(completed.await(15, TimeUnit.SECONDS), "save completion callback did not run");
		assertTrue(callbackOnEdt.get(), "save completion must run on the Swing event dispatch thread");
		assertTrue(fileVisibleToCallback.get(), "saved file must exist before completion is reported");
	}
}
