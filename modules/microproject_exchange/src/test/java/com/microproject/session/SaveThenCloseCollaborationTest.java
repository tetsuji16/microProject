/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;
import com.microproject.util.Environment;
import com.microproject.util.UiDispatch;
import com.microproject.util.UiDispatcher;

/** Issue #967: the route the reporter uses, a real save from the close prompt, leaves only the project file. */
class SaveThenCloseCollaborationTest {
	private final boolean originalClientSide = Environment.isClientSide();
	private final boolean originalBatchMode = Environment.isBatchMode();
	private final JobQueue originalQueue = SessionFactory.getInstance().getJobQueue();

	@TempDir
	Path folder;

	@BeforeEach
	void useWindowThreadAndTestQueue() {
		UiDispatch.setDispatcher(new UiDispatcher() {
			@Override public void invokeLater(Runnable task) { EventQueue.invokeLater(task); }
			@Override public boolean isDispatchThread() { return EventQueue.isDispatchThread(); }
		});
		SessionFactory.getInstance().setJobQueue(new JobQueue("save-close-test", false));
	}

	@AfterEach
	void restoreEnvironment() {
		SessionFactory.getInstance().setJobQueue(originalQueue);
		UiDispatch.setDispatcher(null);
		Alert.setPresenter(null);
		Environment.setClientSide(originalClientSide);
		Environment.setBatchMode(originalBatchMode);
	}

	@Test
	void answeringYesSavesTheProjectThenLeavesOnlyItsFile() throws Exception {
		ProjectFactory factory = ProjectFactory.createInstance();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("save close test", undo), undo);
		project.setLocal(true);
		Path projectFile = folder.resolve("plan.mpo");
		save(project, projectFile);
		byte[] before = Files.readAllBytes(projectFile);
		factory.addProject(project, false, false);
		CollaborationSession session = CollaborationSession.create(project, project.getFileName(), "alice");
		project.setCollaborationSession(session);
		session.start();
		project.createScriptedTask().setManuallyScheduled(true);
		project.setGroupDirty(true);
		assertTrue(project.needsSaving(), "fixture must prompt before closing");
		assertTrue(fileNames().contains("plan.microproject-sync.json"), "fixture must collaborate");

		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		AtomicInteger prompts = new AtomicInteger();
		Alert.setPresenter(new AnsweringYes(prompts));
		CountDownLatch exited = new CountDownLatch(1);
		AtomicReference<List<String>> filesAtExit = new AtomicReference<>();
		Job job = factory.getPortfolio().getRemoveAllProjectsJob(new JobRunnable("exit") {
			@Override public Object run() throws Exception {
				filesAtExit.set(fileNames());
				exited.countDown();
				return null;
			}
		}, true, null);
		SessionFactory.getInstance().getLocalSession().schedule(job);

		assertTrue(exited.await(30, TimeUnit.SECONDS), "the quit job did not reach the exit step");
		assertEquals(1, prompts.get(), "the user must have been asked");
		assertFalse(Arrays.equals(before, Files.readAllBytes(projectFile)), "Yes must save the change");
		assertEquals(List.of("plan.mpo"), filesAtExit.get(), "the save must not bring the files back");
		assertNull(project.getCollaborationSession());
	}

	private static void save(Project project, Path destination) throws Exception {
		CountDownLatch saved = new CountDownLatch(1);
		SaveOptions options = new SaveOptions();
		options.setLocal(true);
		options.setFileName(destination.toString());
		options.setImporter(LocalSession.MPO_PROJECT_IMPORTER);
		options.setPostSaving(value -> saved.countDown());
		LocalSession session = new LocalSession();
		session.setJobQueue(new JobQueue("save-close-fixture", false));
		session.getSaveProjectJob(Collections.singletonList(project), options).execute();
		assertTrue(saved.await(15, TimeUnit.SECONDS), "the fixture project was not saved");
	}

	private List<String> fileNames() throws Exception {
		try (var files = Files.list(folder)) {
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
	}

	private static final class AnsweringYes implements AlertPresenter {
		private final AtomicInteger prompts;

		AnsweringYes(AtomicInteger prompts) {
			this.prompts = prompts;
		}

		@Override public void warn(Object message, Object parent) { }
		@Override public void error(Object message, Object parent) { }
		@Override public int confirmYesNo(Object message) { return Alert.YES_OPTION; }
		@Override public int confirm(Object message) {
			prompts.incrementAndGet();
			return Alert.YES_OPTION;
		}
		@Override public boolean okCancel(Object message) { return true; }
		@Override public String renameProject(String name, Set<String> projectNames, boolean saveAs) { return name; }
		@Override public void warnWithOnceOption(Object message, String preference, Object parent) { }
	}
}
