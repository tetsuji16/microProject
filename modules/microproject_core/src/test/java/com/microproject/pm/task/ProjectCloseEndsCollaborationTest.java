/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.collaboration.CollaborationMetadataStore;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.session.SessionFactory;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;
import com.microproject.util.Environment;

/** Issue #967: every route that closes a project ends its collaboration session once the close is certain. */
class ProjectCloseEndsCollaborationTest {
	private final boolean originalClientSide = Environment.isClientSide();
	private final boolean originalBatchMode = Environment.isBatchMode();
	private final JobQueue originalQueue = SessionFactory.getInstance().getJobQueue();

	@TempDir
	Path folder;

	@BeforeEach
	void useTestQueue() {
		SessionFactory.getInstance().setJobQueue(new JobQueue("close-test", false));
	}

	@AfterEach
	void restoreEnvironment() {
		SessionFactory.getInstance().setJobQueue(originalQueue);
		Alert.setPresenter(null);
		Environment.setClientSide(originalClientSide);
		Environment.setBatchMode(originalBatchMode);
	}

	@Test
	void quittingFromTheWindowLeavesOnlyTheProjectFile() throws Exception {
		assertQuitRemovesCollaborationFiles(true);
	}

	@Test
	void quittingFromTheDesktopLeavesOnlyTheProjectFile() throws Exception {
		assertQuitRemovesCollaborationFiles(false);
	}

	private void assertQuitRemovesCollaborationFiles(boolean calledFromSwing) throws Exception {
		ProjectFactory factory = ProjectFactory.createInstance();
		Project project = openProject(factory);
		CollaborationSession session = startSession(project);
		assertEquals(List.of("plan.microproject-sync.json", "plan.microproject-sync.lock", "plan.mpo"), fileNames());

		CountDownLatch exited = new CountDownLatch(1);
		AtomicReference<List<String>> filesAtExit = new AtomicReference<>();
		JobRunnable exit = new JobRunnable("exit") {
			@Override public Object run() throws Exception {
				filesAtExit.set(fileNames());
				exited.countDown();
				return null;
			}
		};
		Job job = factory.getPortfolio().getRemoveAllProjectsJob(exit, calledFromSwing, null);
		SessionFactory.getInstance().getLocalSession().schedule(job);

		assertTrue(exited.await(30, TimeUnit.SECONDS), "the quit job did not reach the exit step");
		assertEquals(List.of("plan.mpo"), filesAtExit.get(), "collaboration files must be gone before the process exits");
		assertNull(project.getCollaborationSession());
		session.stop(); // a second stop has nothing left to do
		assertEquals(List.of("plan.mpo"), fileNames());
	}

	@Test
	void cancellingTheSavePromptKeepsTheSessionWorking() throws Exception {
		ProjectFactory factory = ProjectFactory.createInstance();
		Project project = openProject(factory);
		project.createScriptedTask().setManuallyScheduled(true);
		project.setGroupDirty(true);
		assertTrue(project.needsSaving(), "fixture must prompt before closing");
		CollaborationSession session = startSession(project);
		List<Integer> prompts = new ArrayList<>();
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		Alert.setPresenter(new CancellingPresenter(prompts));
		boolean[] closeStatus = {true};

		try {
			factory.getPortfolio().getRemoveAllProjectsJob(new JobRunnable("exit") {
				@Override public Object run() {
					throw new AssertionError("a cancelled close must not exit");
				}
			}, true, closeStatus);

			assertEquals(1, prompts.size(), "the user must have been asked");
			assertFalse(closeStatus[0]);
			assertSame(session, project.getCollaborationSession(), "the open document must keep collaborating");
			assertEquals(List.of("plan.microproject-sync.json", "plan.microproject-sync.lock", "plan.mpo"), fileNames());
		} finally {
			session.stop();
		}
	}

	@Test
	void aSessionThatNeverStartedStillRemovesWhatItCreated() throws Exception {
		Path projectFile = Files.writeString(folder.resolve("plan.mpo"), "saved project data");
		Project project = newProject(projectFile);
		CollaborationSession session = CollaborationSession.create(project, projectFile.toString(), "alice");
		session.loadWorkspace();
		assertTrue(Files.exists(CollaborationMetadataStore.buildSidecarFile(new File(projectFile.toString())).toPath()),
			"loading the workspace is what creates the sidecar");

		session.stop();

		assertEquals(List.of("plan.mpo"), fileNames());
	}

	private Project openProject(ProjectFactory factory) throws Exception {
		Path projectFile = Files.writeString(folder.resolve("plan.mpo"), "saved project data");
		Project project = newProject(projectFile);
		factory.addProject(project, false, false);
		return project;
	}

	private static Project newProject(Path projectFile) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("close test", undo), undo);
		project.setLocal(true);
		project.setFileName(projectFile.toString());
		return project;
	}

	private static CollaborationSession startSession(Project project) {
		CollaborationSession session = CollaborationSession.create(project, project.getFileName(), "alice");
		project.setCollaborationSession(session);
		session.start();
		return session;
	}

	private List<String> fileNames() throws Exception {
		try (var files = Files.list(folder)) {
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
	}

	private static final class CancellingPresenter implements AlertPresenter {
		private final List<Integer> prompts;

		CancellingPresenter(List<Integer> prompts) {
			this.prompts = prompts;
		}

		@Override public void warn(Object message, Object parent) { }
		@Override public void error(Object message, Object parent) { }
		@Override public int confirmYesNo(Object message) { return Alert.NO_OPTION; }
		@Override public int confirm(Object message) {
			prompts.add(Alert.CANCEL_OPTION);
			return Alert.CANCEL_OPTION;
		}
		@Override public boolean okCancel(Object message) { return false; }
		@Override public String renameProject(String name, Set<String> projectNames, boolean saveAs) { return name; }
		@Override public void warnWithOnceOption(Object message, String preference, Object parent) { }
	}
}
