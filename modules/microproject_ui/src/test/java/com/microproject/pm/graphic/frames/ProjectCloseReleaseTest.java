/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.job.JobQueue;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.session.SessionFactory;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;
import com.microproject.util.Environment;

/** A document keeps what it still needs until its close is certain, which is after the save prompt. */
class ProjectCloseReleaseTest {
	private final boolean originalClientSide = Environment.isClientSide();
	private final boolean originalBatchMode = Environment.isBatchMode();
	private final JobQueue originalQueue = SessionFactory.getInstance().getJobQueue();

	@TempDir
	Path folder;

	@AfterEach
	void restoreEnvironment() {
		SessionFactory.getInstance().setJobQueue(originalQueue);
		Alert.setPresenter(null);
		Environment.setClientSide(originalClientSide);
		Environment.setBatchMode(originalBatchMode);
	}

	@Test
	void extractedFilesStayUntilTheSavePromptIsAnswered() throws Exception {
		List<Project> released = new CopyOnWriteArrayList<>();
		CountDownLatch closed = new CountDownLatch(1);
		GraphicManager manager = new GraphicManager(new JPanel()) {
			@Override public DocumentFrame addProjectFrame(Project project) { return null; }
			@Override void closeProjectArtifacts(Project project) {
				released.add(project);
				closed.countDown();
			}
		};
		manager.setFrameManager((FrameManager) Proxy.newProxyInstance(FrameManager.class.getClassLoader(),
			new Class<?>[] { FrameManager.class },
			(proxy, method, args) -> method.getReturnType() == boolean.class ? Boolean.TRUE : null));
		// Other tests leave managers listening to the shared factory, so this one gets its own.
		manager.getProjectFactory().getPortfolio().removeObjectListener(manager);
		ProjectFactory factory = ProjectFactory.createInstance();
		manager.projectFactory = factory;
		factory.getPortfolio().addObjectListener(manager);
		SessionFactory.getInstance().setJobQueue(manager.getJobQueue());
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("release test", undo), undo);
		project.setLocal(true);
		project.setFileName(folder.resolve("plan.mpo").toString());
		factory.addProject(project, false, false);
		project.createScriptedTask().setManuallyScheduled(true);
		project.setGroupDirty(true);
		assertTrue(project.needsSaving(), "fixture must prompt before closing");
		Environment.setClientSide(true);
		Environment.setBatchMode(false);

		Alert.setPresenter(new Answering(Alert.CANCEL_OPTION));
		manager.closeProject(project);
		assertEquals(List.of(), released, "Cancel keeps the document open, so it still needs its files");

		Alert.setPresenter(new Answering(Alert.NO_OPTION));
		manager.closeProject(project);
		assertTrue(closed.await(30, TimeUnit.SECONDS), "the closed project's files were not released");
		assertEquals(List.of(project), released);
	}

	private record Answering(int answer) implements AlertPresenter {
		@Override public void warn(Object message, Object parent) { }
		@Override public void error(Object message, Object parent) { }
		@Override public int confirmYesNo(Object message) { return answer; }
		@Override public int confirm(Object message) { return answer; }
		@Override public boolean okCancel(Object message) { return false; }
		@Override public String renameProject(String name, Set<String> projectNames, boolean saveAs) { return name; }
		@Override public void warnWithOnceOption(Object message, String preference, Object parent) { }
	}
}
