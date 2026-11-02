/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #967: after a prior process leaves a live same-user task lease under a
 * different clientInstanceId (crash, force-quit, or a second local instance),
 * the next session for that user must reclaim the lease so selection and edits
 * work without waiting for the 15s expiry.
 */
class SameUserStaleLeaseReclaimTest {
	@TempDir
	Path tempDir;

	@Test
	void reclaimsLiveLeaseLeftByPriorInstanceOfSameUser() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("reclaim-same-user.mpo"));
		Project project = project();
		NormalTask task = task(project);
		long taskId = task.getUniqueId();

		CollaborationSession first = new CollaborationSession(project, projectFile.toString(), "alice");
		assertTrue(first.tryAcquireTaskLock(task), "first instance acquires the task");
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile());
		CollaborationMetadataStore.LockRecord planted = store.load().getLocks().get(String.valueOf(taskId));
		assertEquals("alice", planted.getUserKey());
		String firstOwnerKey = planted.getOwnerKey();

		// Simulate crash: abandon the first session without stop()/release.
		CollaborationSession second = new CollaborationSession(project, projectFile.toString(), "alice");
		assertTrue(second.tryAcquireTaskLock(task),
				"same user must reclaim a live lease left by a prior instance");

		CollaborationMetadataStore.LockRecord reclaimed = store.load().getLocks().get(String.valueOf(taskId));
		assertEquals("alice", reclaimed.getUserKey());
		assertFalse(firstOwnerKey.equals(reclaimed.getOwnerKey()),
				"ownerKey must move to the new client instance");
		assertTrue(second.tryAcquireTaskLock(task), "already held locally still succeeds");
		second.stop();
	}

	@Test
	void differentUserStillBlockedWhileLeaseIsLive() throws Exception {
		Path projectFile = Files.createFile(tempDir.resolve("other-user-blocked.mpo"));
		Project project = project();
		NormalTask task = task(project);

		CollaborationSession alice = new CollaborationSession(project, projectFile.toString(), "alice");
		assertTrue(alice.tryAcquireTaskLock(task));

		CollaborationSession bob = new CollaborationSession(project, projectFile.toString(), "bob");
		assertFalse(bob.tryAcquireTaskLock(task),
				"a different user must not take a live lease");
		alice.stop();
		bob.stop();
	}

	private static Project project() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask task(Project project) {
		return project.createScriptedTask();
	}
}
