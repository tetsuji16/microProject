/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;
import com.microproject.util.Environment;

class CollaborationHelperTest {
	private final boolean previousClientSide = Environment.isClientSide();
	private final boolean previousBatchMode = Environment.isBatchMode();

	@AfterEach
	void restoreAlertEnvironment() {
		Alert.setPresenter(null);
		Environment.setClientSide(previousClientSide);
		Environment.setBatchMode(previousBatchMode);
	}

	@Test
	void rejectedTaskLockSchedulesOneWarningOnEdtWithOwnerAndMessage() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("collaboration-lock-warning", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Locked task");
		project.setCollaborationSession(new RejectingCollaborationSession(project));
		JPanel parent = new JPanel();
		CountDownLatch warningShown = new CountDownLatch(1);
		AtomicInteger warningCount = new AtomicInteger();
		AtomicReference<Object> actualMessage = new AtomicReference<>();
		AtomicReference<Object> actualParent = new AtomicReference<>();
		AtomicReference<Boolean> onEdt = new AtomicReference<>(false);
		Alert.setPresenter(new AlertPresenter() {
			@Override
			public void warn(Object message, Object warningParent) {
				actualMessage.set(message);
				actualParent.set(warningParent);
				onEdt.set(SwingUtilities.isEventDispatchThread());
				warningCount.incrementAndGet();
				warningShown.countDown();
			}

			@Override public void error(Object message, Object warningParent) { }
			@Override public int confirmYesNo(Object message) { return Alert.NO_OPTION; }
			@Override public int confirm(Object message) { return Alert.NO_OPTION; }
			@Override public boolean okCancel(Object message) { return true; }
			@Override public String renameProject(String name, Set<String> projectNames, boolean saveAs) { return name; }
			@Override public void warnWithOnceOption(Object message, String preference, Object warningParent) { }
		});
		Environment.setClientSide(true);
		Environment.setBatchMode(false);
		assertTrue(Alert.allowPopups());

		assertFalse(CollaborationHelper.tryLockNodes(project, List.of(task), parent, "assign"));
		assertTrue(warningShown.await(2, TimeUnit.SECONDS), "the deferred warning presenter was not invoked");

		assertEquals(1, warningCount.get(), "one rejected command should queue one warning");
		assertTrue(onEdt.get(), "the alert presenter must be invoked on the EDT");
		assertEquals("Cannot assign task \"Locked task\" because it is locked by another user.", actualMessage.get());
		assertSame(parent, actualParent.get());
	}

	private static final class RejectingCollaborationSession extends CollaborationSession {
		private RejectingCollaborationSession(Project project) {
			super(project, new File(System.getProperty("java.io.tmpdir"), "microproject-collaboration-warning.mpo").getAbsolutePath(),
				"test-user");
		}

		@Override
		public Task tryAcquireTasks(Iterable<Task> tasks) {
			return tasks.iterator().next();
		}

		@Override
		public String describeLockOwner(Task task) {
			return "another user";
		}
	}
}
