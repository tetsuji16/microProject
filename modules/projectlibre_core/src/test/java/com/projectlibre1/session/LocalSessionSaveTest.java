package com.projectlibre1.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.projectlibre1.job.Job;
import com.projectlibre1.job.JobQueue;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

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
