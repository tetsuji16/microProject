/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.collaboration.OperationLog;

class MpoTransactionBoundaryTest {
	@Test
	void validatedSnapshotPlanProducesReloadableArchive() throws Exception {
		Project project = loadSample();
		Path target = Files.createTempFile("mpof-plan-", ".mpo");
		try {
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.toString());
			writer.setProject(project);
			writer.exportFile();
			assertTrue(Files.size(target) > 0L);
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project reopened = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			assertTrue(reopened.getTaskList().size() > 0, "planned archive must retain the MSP task snapshot");
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void failedReplacementLeavesExistingArchiveUntouched() throws Exception {
		Project project = loadSample();
		Path target = Files.createTempFile("mpof-plan-failure-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString());
			initial.setProject(project);
			initial.exportFile();
			byte[] before = Files.readAllBytes(target);
			MpoFileImporter failing = new MpoFileImporter() {
				@Override
				protected void moveTemporary(Path temporary, Path destination) throws IOException {
					throw new IOException("injected replacement failure");
				}
			};
			failing.setFileName(target.toString());
			failing.setProject(project);
			assertThrows(IOException.class, failing::exportFile);
			assertArrayEquals(before, Files.readAllBytes(target));
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void failedMergedReplacementLeavesProjectAndOperationStateUntouched() throws Exception {
		Project project = loadSample();
		Path target = Files.createTempFile("mpof-merge-failure-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString());
			initial.setProject(project);
			initial.exportFile();
			byte[] archiveBefore = Files.readAllBytes(target);
			Object state = operationState(project);
			byte[] operationJsonBefore = ((byte[]) field(state, "json")).clone();
			int operationCountBefore = ((List<?>) field(state, "operations")).size();
			Task task = (Task) project.getTaskOutlineIterator().next();
			String changedName = task.getName() + " (local edit)";
			task.setName(changedName);

			MpoFileImporter failing = new MpoFileImporter() {
				@Override
				protected void moveTemporary(Path temporary, Path destination) throws IOException {
					throw new IOException("injected replacement failure");
				}
			};
			failing.setFileName(target.toString());
			failing.setProject(project);
			assertThrows(IOException.class, failing::exportFile);
			assertArrayEquals(archiveBefore, Files.readAllBytes(target));
			assertEquals(changedName, task.getName(), "a failed merge must not replace the live project");
			assertArrayEquals(operationJsonBefore, (byte[]) field(state, "json"),
				"a failed merge must not commit its operation plan");
			assertEquals(operationCountBefore, ((List<?>) field(state, "operations")).size());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void partialApplyIsExplicitAndRetryableAfterArchiveReplacement() throws Exception {
		Project original = loadSample();
		Path target = Files.createTempFile("mpof-partial-apply-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString());
			initial.setProject(original);
			initial.exportFile();

			// A second editor produces a committed operation in the archive.
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project concurrent = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			Task concurrentTask = (Task) concurrent.getTaskOutlineIterator().next();
			concurrentTask.setName(concurrentTask.getName() + " (concurrent)");
			MpoFileImporter concurrentWriter = new MpoFileImporter();
			concurrentWriter.setFileName(target.toString());
			concurrentWriter.setProject(concurrent);
			concurrentWriter.exportFile();

			AtomicBoolean failOnce = new AtomicBoolean(true);
			MpoFileImporter retrying = new MpoFileImporter() {
				@Override
				protected void applyMergedOperationsOnEdt(Project project,
						java.util.List<OperationLog.Operation> operations) throws IOException {
					if (project == original && failOnce.getAndSet(false))
						throw new IOException("injected apply failure");
					super.applyMergedOperationsOnEdt(project, operations);
				}
			};
			retrying.setFileName(target.toString());
			retrying.setProject(original);
			assertThrows(MpoPartialApplyException.class, retrying::exportFile,
				"a post-replacement apply failure must be explicit and retryable");
			retrying.exportFile();
			assertTrue(Files.size(target) > 0L, "retry must leave a readable archive");
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object operationState(Project project) throws Exception {
		Class<?> stateType = Class.forName("com.microproject.exchange.MpoFileImporter$MpoOperationState");
		return project.findTransientDocumentState((Class) stateType);
	}

	private static Object field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static Project loadSample() throws Exception {
		Path sample = Path.of("..", "..", "samples", "CCPM path comparison English.mpo");
		if (!Files.isRegularFile(sample)) sample = Path.of("samples", "CCPM path comparison English.mpo");
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		try (var input = Files.newInputStream(sample)) {
			return reader.loadProject(input);
		}
	}
}
