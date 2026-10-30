/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class CollaborationSessionLockFailureTest {
	@Test
	void taskLocksAcquiredBeforeMetadataTimeoutAreForgottenAndNotRenewed() throws Exception {
		Path projectFile = Files.createTempFile("mpo-collaboration-rollback", ".mpo");
		Project project = project();
		NormalTask first = task(project);
		NormalTask second = task(project);
		CollaborationSession session = new CollaborationSession(project, projectFile.toString(), "alice");
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile());
		Path lockPath = CollaborationMetadataStore.buildLockFile(store.getSidecarFile()).toPath();
		AtomicReference<FileChannel> heldChannel = new AtomicReference<>();
		AtomicReference<FileLock> heldLock = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();

		EventQueue.invokeAndWait(() -> {
			Iterable<Task> tasks = () -> new Iterator<>() {
				private int index;

				@Override public boolean hasNext() { return index < 2; }

				@Override public Task next() {
					if (index++ == 0) return first;
					try {
						FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
						heldChannel.set(channel);
						heldLock.set(channel.lock());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return second;
				}
			};
			try {
				session.tryAcquireTasks(tasks);
			} catch (Throwable error) {
				failure.set(error);
			} finally {
				try {
					if (heldLock.get() != null) heldLock.get().release();
					if (heldChannel.get() != null) heldChannel.get().close();
				} catch (Exception e) {
					failure.compareAndSet(null, e);
				}
			}
		});

		assertInstanceOf(CollaborationLockUnavailableException.class, failure.get());
		assertTrue(session.getLocalLocks().isEmpty(), "new locks must be removed from heartbeat renewal after rollback");
		session.stop();
	}

	@Test
	void failedAfterSaveMetadataRefreshIsRetriedByLaterPoll() throws Exception {
		Path projectFile = Files.createTempFile("mpo-collaboration-after-save", ".mpo");
		AtomicBoolean failNextMutation = new AtomicBoolean(true);
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile.toFile()) {
			@Override
			public void mutate(MetadataMutation mutation) {
				if (failNextMutation.compareAndSet(true, false)) {
					throw new CollaborationLockUnavailableException("injected temporary metadata lock failure");
				}
				super.mutate(mutation);
			}
		};
		CollaborationSession session = new CollaborationSession(null, projectFile.toString(), "alice", store);
		Files.writeString(projectFile, "saved project data");
		AtomicReference<Throwable> failure = new AtomicReference<>();

		EventQueue.invokeAndWait(session::afterSave);
		EventQueue.invokeAndWait(() -> {
			try {
				session.pollAt(System.currentTimeMillis());
			} catch (Throwable error) {
				failure.set(error);
			}
		});

		assertEquals(null, failure.get());
		assertEquals(Files.size(projectFile), store.load().getProjectFileLength());
		session.stop();
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
