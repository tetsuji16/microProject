/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.microproject.application.AutoRecoveryStore;
import com.microproject.pm.task.Project;
import com.microproject.session.LoadOptions;

class RecoveryLoadCallbackTest {
	@Test
	void asynchronousSuccessRestoresIdentityAndCompletesOnce() {
		var entry = new AutoRecoveryStore.Entry(42, "Plan", "original.pod", Instant.now(),
			Path.of("snapshot.pod"), Path.of("metadata"), false);
		var options = new AtomicReference<LoadOptions>();
		var outcomes = new ArrayList<Boolean>();
		var initialized = new ArrayList<Project>();
		GraphicManager.loadRecoveryDocument(entry, "user", options::set, initialized::add, outcomes::add);
		assertFalse(options.get().isSync());
		assertTrue(outcomes.isEmpty());
		Project project = Project.getDummy();
		String oldFileName = project.getFileName();
		boolean oldDirty = project.isGroupDirty();
		try {
			options.get().getEndSwingClosure().accept(project);
			options.get().getEndSwingClosure().accept(null);
			assertEquals("original.pod", project.getFileName());
			assertTrue(project.isGroupDirty());
			assertEquals(java.util.List.of(project), initialized);
			assertEquals(java.util.List.of(true), outcomes);
		} finally {
			project.setFileName(oldFileName);
			project.setGroupDirty(oldDirty);
		}
	}

	@Test
	void failedLoadCompletesOnceWithoutInitializing() {
		var entry = new AutoRecoveryStore.Entry(42, "Plan", null, Instant.now(),
			Path.of("snapshot.pod"), Path.of("metadata"), false);
		var options = new AtomicReference<LoadOptions>();
		var outcomes = new ArrayList<Boolean>();
		GraphicManager.loadRecoveryDocument(entry, "user", options::set, project -> fail("unexpected project"), outcomes::add);
		options.get().getEndSwingClosure().accept(null);
		options.get().getEndSwingClosure().accept(null);
		assertEquals(java.util.List.of(false), outcomes);
	}
	@Test
	void initializationFailureCompletesOnceAsFailure() {
		var entry = new AutoRecoveryStore.Entry(42, "Plan", "original.pod", Instant.now(),
			Path.of("snapshot.pod"), Path.of("metadata"), false);
		var options = new AtomicReference<LoadOptions>();
		var outcomes = new ArrayList<Boolean>();
		GraphicManager.loadRecoveryDocument(entry, "user", options::set,
			project -> { throw new IllegalStateException("injected"); }, outcomes::add);
		Project project = Project.getDummy();
		String oldFileName = project.getFileName();
		boolean oldDirty = project.isGroupDirty();
		try {
			options.get().getEndSwingClosure().accept(project);
			options.get().getEndSwingClosure().accept(null);
			assertEquals(java.util.List.of(false), outcomes);
		} finally { project.setFileName(oldFileName); project.setGroupDirty(oldDirty); }
	}

}
