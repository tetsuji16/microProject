/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void failedMergedReplacementKeepsArchiveModelAndReservedOperationIds() throws Exception {
		Project project = loadSample();
		Path target = Files.createTempFile("mpof-merge-failure-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString());
			initial.setProject(project);
			initial.exportFile();
			byte[] archiveBefore = Files.readAllBytes(target);
			Object state = operationState(project);
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
			List<String> reserved = operationIds(state);
			assertEquals(operationCountBefore + 1, reserved.size(), "the edit keeps one reserved operation");
			assertThrows(IOException.class, failing::exportFile);
			assertEquals(reserved, operationIds(state), "a retry must not issue new operation IDs");

			MpoFileImporter retry = new MpoFileImporter();
			retry.setFileName(target.toString());
			retry.setProject(project);
			retry.exportFile();
			assertEquals(reserved, operationIds(state), "the successful retry must write the reserved IDs");
			assertEquals(reserved, archivedOperationIds(target));
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
						java.util.List<OperationLog.Operation> operations,
						java.util.Set<String> alreadyAppliedOperationIds) throws IOException {
					if (project == original && failOnce.getAndSet(false))
						throw new IOException("injected apply failure");
					super.applyMergedOperationsOnEdt(project, operations, alreadyAppliedOperationIds);
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

	@Test
	void laterEditMustSurviveRemoteMergeCommit() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-later-edit-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString()); initial.setProject(local); initial.exportFile();
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project remote = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			Task remoteTask = (Task) remote.getTaskOutlineIterator().next();
			remoteTask.setName("remote edit");
			MpoFileImporter remoteWriter = new MpoFileImporter();
			remoteWriter.setFileName(target.toString()); remoteWriter.setProject(remote); remoteWriter.exportFile();
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected void moveTemporary(Path temporary, Path destination) throws IOException {
					super.moveTemporary(temporary, destination);
					localTask.setName("later local edit");
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile);
			assertEquals("later local edit", localTask.getName());
			assertThrows(MpoConflictRecoveryException.class, writer::exportFile);
			assertEquals("later local edit", localTask.getName());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void laterDifferentFieldSurvivesMergeAndRetry() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-later-notes-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString()); initial.setProject(local); initial.exportFile();
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project remote = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			((Task) remote.getTaskOutlineIterator().next()).setName("remote edit");
			MpoFileImporter remoteWriter = new MpoFileImporter();
			remoteWriter.setFileName(target.toString()); remoteWriter.setProject(remote); remoteWriter.exportFile();
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			AtomicBoolean editOnce = new AtomicBoolean(true);
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected void moveTemporary(Path temporary, Path destination) throws IOException {
					super.moveTemporary(temporary, destination);
					if (editOnce.getAndSet(false)) localTask.setNotes("later notes");
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile,
				"the later notes are not in the archive, so this save must not report success");
			assertEquals("later notes", localTask.getNotes());
			writer.exportFile();
			assertTrue(writer.confirmSavedState(), "a merged save must leave the project confirmed as saved");
			assertEquals("remote edit", localTask.getName());
			assertEquals("later notes", localTask.getNotes());
			Project reopened = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			Task reopenedTask = (Task) reopened.getTaskOutlineIterator().next();
			assertEquals("remote edit", reopenedTask.getName());
			assertEquals("later notes", reopenedTask.getNotes());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void laterDurationEditMustNotBeReportedAsSaved() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-later-duration-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString()); initial.setProject(local); initial.exportFile();
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project remote = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			((Task) remote.getTaskOutlineIterator().next()).setName("remote edit");
			MpoFileImporter remoteWriter = new MpoFileImporter();
			remoteWriter.setFileName(target.toString()); remoteWriter.setProject(remote); remoteWriter.exportFile();
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			long laterDuration = localTask.getDuration() + 8L * 60 * 60 * 1000;
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected void moveTemporary(Path temporary, Path destination) throws IOException {
					super.moveTemporary(temporary, destination);
					localTask.setDuration(laterDuration);
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile);
			assertEquals(laterDuration, localTask.getDuration());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}
	@Test
	void failedMergePreflightRemovesStagedArchive() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-preflight-cleanup-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString()); initial.setProject(local); initial.exportFile();
			byte[] before = Files.readAllBytes(target);
			Path[] stage = new Path[1];
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected java.io.File createTemporaryFile(java.io.File destination) throws IOException {
					java.io.File file = super.createTemporaryFile(destination);
					stage[0] = file.toPath();
					return file;
				}
				@Override protected void verifyMergeApplicability(Project source,
						List<OperationLog.Operation> operations, java.util.Set<String> applied) throws IOException {
					throw new IOException("injected preflight failure");
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(IOException.class, writer::exportFile);
			assertTrue(stage[0] != null && !Files.exists(stage[0]));
			assertArrayEquals(before, Files.readAllBytes(target));
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void laterNotesOnSameFieldMustSurviveRemoteMerge() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-later-same-notes-", ".mpo");
		try {
			MpoFileImporter initial = new MpoFileImporter();
			initial.setFileName(target.toString()); initial.setProject(local); initial.exportFile();
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project remote = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			((Task) remote.getTaskOutlineIterator().next()).setNotes("remote notes");
			MpoFileImporter remoteWriter = new MpoFileImporter();
			remoteWriter.setFileName(target.toString()); remoteWriter.setProject(remote); remoteWriter.exportFile();
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected void moveTemporary(Path temporary, Path destination) throws IOException {
					super.moveTemporary(temporary, destination);
					localTask.setNotes("later local notes");
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile);
			assertEquals("later local notes", localTask.getNotes());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void editDuringInitialSaveMustNotBeReportedAsSaved() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-initial-later-edit-", ".mpo");
		try {
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected void moveTemporary(Path temporary, Path destination) throws IOException {
					super.moveTemporary(temporary, destination);
					localTask.setName("later local edit");
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile);
			assertEquals("later local edit", localTask.getName());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void editBeforeReplacementIsNotReportedAsSaved() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-edit-before-replace-", ".mpo");
		try {
			Task localTask = (Task) local.getTaskOutlineIterator().next();
			MpoFileImporter writer = new MpoFileImporter() {
				@Override protected java.io.File createTemporaryFile(java.io.File destination) throws IOException {
					localTask.setName("edit while staging");
					return super.createTemporaryFile(destination);
				}
			};
			writer.setFileName(target.toString()); writer.setProject(local);
			assertThrows(MpoPartialApplyException.class, writer::exportFile);
			assertEquals("edit while staging", localTask.getName());
			assertFalse(writer.confirmSavedState());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void linkedChildSaveMarksSavedOnlyWhenNoEditRacedTheExport() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-child-save-", ".mpo");
		try {
			local.setGroupDirty(true);
			var clean = new FileImporterPortAdapter("mpo", MpoFileImporter::new);
			assertTrue(clean.exportProject(local, new com.microproject.port.ExportPort.ExportRequest(target, true)).succeeded());
			assertFalse(local.needsSaving(), "an unraced save marks the project saved");

			Task task = (Task) local.getTaskOutlineIterator().next();
			var racing = new FileImporterPortAdapter("mpo", () -> new MpoFileImporter() {
				@Override public void exportFile() throws Exception {
					super.exportFile();
					task.setNotes("edited after the commit");
					local.setDirty(true); // the edit route marks the project modified
				}
			});
			var result = racing.exportProject(local, new com.microproject.port.ExportPort.ExportRequest(target, true));
			assertFalse(result.succeeded());
			assertTrue(local.needsSaving(), "an edit made after the save must stay unsaved");
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@Test
	void editAfterCommitLeavesProjectUnconfirmed() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-edit-after-commit-", ".mpo");
		try {
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.toString()); writer.setProject(local); writer.exportFile();
			assertTrue(writer.confirmSavedState());
			((Task) local.getTaskOutlineIterator().next()).setNotes("after the commit");
			assertFalse(writer.confirmSavedState());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	/** Every completed save must hold the name the project had when it reported success. */
	@Test
	void concurrentEditsAreSavedOrReported() throws Exception {
		Project local = loadSample();
		Path target = Files.createTempFile("mpof-concurrent-edit-", ".mpo");
		Task localTask = (Task) local.getTaskOutlineIterator().next();
		AtomicBoolean stop = new AtomicBoolean();
		java.util.concurrent.atomic.AtomicInteger edits = new java.util.concurrent.atomic.AtomicInteger();
		Thread editor = new Thread(() -> {
			while (!stop.get()) {
				localTask.setName("edit " + edits.incrementAndGet());
				try { Thread.sleep(5L); } catch (InterruptedException interrupted) { return; }
			}
		});
		editor.start();
		try {
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.toString()); writer.setProject(local);
			for (int attempt = 0; attempt < 5; attempt++) {
				try {
					writer.exportFile();
				} catch (IOException reported) {
					assertTrue(reported instanceof MpoPartialApplyException
						|| reported.getMessage().contains("kept changing"), reported.toString());
				}
			}
		} finally {
			stop.set(true);
			editor.join();
		}
		try {
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.toString()); writer.setProject(local); writer.exportFile();
			MpoFileImporter reader = new MpoFileImporter();
			reader.setProjectFactory(ProjectFactory.getInstance());
			Project reopened = reader.loadProject(new ByteArrayInputStream(Files.readAllBytes(target)));
			assertEquals(localTask.getName(), ((Task) reopened.getTaskOutlineIterator().next()).getName());
		} finally {
			Files.deleteIfExists(target);
			Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".lock"));
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> operationIds(Object state) throws Exception {
		return ((List<OperationLog.Operation>) field(state, "operations")).stream().map(OperationLog.Operation::id).toList();
	}

	private static List<String> archivedOperationIds(Path target) throws IOException {
		try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(target.toFile())) {
			java.util.zip.ZipEntry entry = zip.getEntry(MpoFileImporter.OPERATIONS_ENTRY);
			try (java.io.InputStream in = zip.getInputStream(entry)) {
				return new OperationLog().readJsonl(in.readAllBytes()).operations().stream()
					.map(OperationLog.Operation::id).toList();
			}
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
