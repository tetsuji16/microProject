/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSplitInterval;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.task.UpdateProjectRequest;
import com.microproject.command.UpdateProjectCommand;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceType;
import com.microproject.pm.resource.SharedResourcePoolService;
import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MpoFileImporterTest {
	@Test
	void saveFailureFeedbackDistinguishesConflictFromLockFailureAndNamesRecoveryCopy() {
		Path recoveryCopy = Path.of("C:/projects/recovery.mpo");
		MpoConflictRecoveryException conflict = new MpoConflictRecoveryException(recoveryCopy, List.of());
		MpoConflictRecoveryException lockFailure = MpoConflictRecoveryException.lockUnavailable(
			recoveryCopy, new IOException("locked"));

		assertTrue(conflict.userFacingMessage().contains(recoveryCopy.toString()));
		assertTrue(lockFailure.userFacingMessage().contains(recoveryCopy.toString()));
		assertNotEquals(conflict.userFacingMessage(), lockFailure.userFacingMessage());
	}

	@AfterEach
	void closeOwnedExtractionSessions() {
		MpoExtractionOwnershipRegistry.closeAll();
		MpoFileImporter.setExtractionWorkspaceRoot(null);
	}

	@Test
	void loadProjectDoesNotCloseCallerOwnedInputStream() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		class CloseTrackingInputStream extends ByteArrayInputStream {
			boolean closed;
			CloseTrackingInputStream(byte[] data) { super(data); }
			@Override public void close() { closed = true; }
		}
		CloseTrackingInputStream input = new CloseTrackingInputStream(archive.toByteArray());
		assertTrue(new MpoFileImporter().loadProject(input) != null);
		assertFalse(input.closed, "MPO loader must not close a caller-owned stream");
	}

	@Test
	void mpoSnapshotSerializationRetainsTheUnderlyingFailureCause() {
		IOException failure = assertThrows(IOException.class,
				() -> new MpoFileImporter().saveProject(null, new ByteArrayOutputStream()));
		assertTrue(failure.getCause() != null,
				"MPO snapshot serialization must retain the MSPDI failure cause");
	}

	@Test
	void operationLogSemanticLimitsAreRejectedBeforeProjectImport() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String documentId = manifestDocumentId(entries);
		String parent = "00000000-0000-0000-0000-000000000001";
		StringBuilder parents = new StringBuilder("[");
		for (int i = 0; i < MpoFileImporter.MAX_OPERATION_PARENTS + 1; i++) {
			if (i > 0) parents.append(',');
			parents.append('"').append(parent).append('"');
		}
		parents.append(']');
		String operation = "{\"id\":\"00000000-0000-0000-0000-000000000002\","
			+ "\"actorId\":\"00000000-0000-0000-0000-000000000003\",\"sequence\":1,"
			+ "\"parents\":" + parents + ",\"kind\":\"task.update\","
			+ "\"entityId\":\"00000000-0000-0000-0000-000000000004\",\"payload\":{}}\n";
		entries.put(MpoFileImporter.OPERATIONS_ENTRY,
			("{\"type\":\"header\",\"schemaVersion\":1,\"documentId\":\"" + documentId + "\"}\n" + operation)
				.getBytes(StandardCharsets.UTF_8));
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, entries.get(MpoFileImporter.OPERATIONS_ENTRY));
		assertThrows(IOException.class, () -> new MpoFileImporter().loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void ccpmHistorySemanticLimitIsRejectedBeforeProjectImport() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, archive));
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		StringBuilder history = new StringBuilder();
		for (int i = 0; i < MpoFileImporter.MAX_CCPM_HISTORY_ENTRIES + 1; i++) history.append("{}\n");
		byte[] historyBytes = history.toString().getBytes(StandardCharsets.UTF_8);
		entries.put(MpoFileImporter.CCPM_HISTORY_ENTRY, historyBytes);
		updateManifestChecksum(entries, MpoFileImporter.CCPM_HISTORY_ENTRY, historyBytes);
		assertThrows(IOException.class, () -> new MpoFileImporter().loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void mpoSaveCreatesANewDestinationDirectory() throws Exception {
		Project project = projectForRoundTrip();
		File root = java.nio.file.Files.createTempDirectory("mpo-new-folder-").toFile();
		root.deleteOnExit();
		File destination = new File(root, "nested" + File.separator + "plan.mpo");
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(destination.getAbsolutePath());
		writer.setProject(project);

		writer.exportFile();

		assertTrue(destination.isFile(), "MPO save must create a missing destination directory");
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(destination.getAbsolutePath());
		try (java.io.FileInputStream input = new java.io.FileInputStream(destination)) {
			assertTrue(reader.loadProject(input) != null, "newly saved MPO must be readable");
		}
	}

	@Test
	void hiddenTaskVisibilitySurvivesMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		Task task = firstTask(original);
		task.setHiddenTask(true);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());

		Task restored = firstTask(reopened);
		assertTrue(restored.isHiddenTask(), "MPO reload must preserve hidden task visibility");
	}

	@Test
	void taskColumnLayoutSurvivesMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		FieldDictionary dictionary = FieldDictionary.getInstance();
		SpreadSheetFieldArray layout = new SpreadSheetFieldArray();
		layout.add(dictionary.getFieldFromId("Field.name"));
		layout.add(dictionary.getFieldFromId("Field.duration"));
		layout.add(dictionary.getFieldFromId("Field.percentComplete"));
		layout.setWidths(new ArrayList<>(List.of(240, 88, 72)));
		layout.setManualWidths(new ArrayList<>(List.of(true, false, true)));
		original.setFieldArray(layout);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		SpreadSheetFieldArray restored = reopened.getFieldArray();

		assertEquals(List.of("Field.name", "Field.duration", "Field.percentComplete"),
			restored.stream().map(Field::getId).toList());
		assertEquals(240, restored.getWidth(0));
		assertEquals(88, restored.getWidth(1));
		assertEquals(72, restored.getWidth(2));
		assertTrue(restored.isManualWidth(0));
		assertFalse(restored.isManualWidth(1));
		assertTrue(restored.isManualWidth(2));
	}

	@Test
	void mpoPreservesUnknownExtensionBytesAcrossLoadAndSave() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String extensionPath = "future/vendor-state.bin";
		byte[] extension = new byte[] { 0, 1, 2, (byte) 0xFE, (byte) 0xFF };
		entries.put(extensionPath, extension);
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		manifest = manifest.replace("</manifest>", "<entry path=\"" + extensionPath + "\" sha256=\""
			+ sha256(extension) + "\"/></manifest>");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, resaved);

		assertArrayEquals(extension, readEntries(resaved.toByteArray()).get(extensionPath));
	}

	@Test
	void taskOwnedSplitIntervalsSurviveMpoSaveAndReload() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		long splitFrom = task.getStart() + 4L * 60L * 60L * 1000L;
		long splitTo = splitFrom + 60L * 60L * 1000L;
		List<TaskSplitInterval> expected = List.of(new TaskSplitInterval(
			splitFrom - task.getStart(), splitTo - task.getStart()));
		task.restoreTaskSplitIntervals(this, expected);

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project reopened = loadFromBytes(archive.toByteArray());
		NormalTask restored = (NormalTask) firstTask(reopened);

		assertEquals(expected, restored.getTaskSplitIntervals());
		List<String> projected = new ArrayList<>();
		restored.consumeIntervals(interval -> projected.add(interval.getStart() + ":" + interval.getEnd()));
		assertTrue(projected.size() > 1, "reloaded task projection must render the split gap: " + projected
			+ " / " + restored.getStart() + ".." + restored.getEnd() + " / " + expected);
	}

	@Test
	void nativeMpoPreservesExactTaskScheduleSnapshot() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(original);
		NormalTask secondTask = (NormalTask) original.createLocalTaskNode(null).getImpl();
		secondTask.setName("Materialized after blank row");
		secondTask.setUniqueId(9_823_471L);
		com.microproject.grouping.core.model.NodeModel taskModel = original.getTaskModel();
		com.microproject.grouping.core.Node secondNode = taskModel.search(secondTask);
		com.microproject.grouping.core.Node blankRow = NodeFactory.getInstance().createVoidNode();
		Object outlineRoot = taskModel.getHierarchy().getRoot();
		taskModel.add((com.microproject.grouping.core.Node) outlineRoot, blankRow,
			taskModel.getIndexOfChild(outlineRoot, secondNode),
			com.microproject.grouping.core.model.NodeModel.SILENT);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(3L * day);
		secondTask.setDuration(2L * day);
		Resource resource = original.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L,
			MpoFileImporterTest.class);
		assignment.setWork(3L * day, null);
		long start = task.getStart() - day;
		long finish = start + 4L * day;
		long secondStart = secondTask.getStart() - 2L * day;
		long secondFinish = secondStart + 5L * day;
		long constraintDate = start + 30L * 60L * 1000L;
		task.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, constraintDate);
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setFinish(finish);
		task.setActualStartNoEvent(start);
		secondTask.getCurrentSchedule().setStart(secondStart);
		secondTask.getCurrentSchedule().setFinish(secondFinish);

		byte[] archive = saveProjectBytes(original);
		String projectXml = new String(readEntries(archive).get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		assertTrue(projectXml.contains("<IsNull>1</IsNull>"), "the test fixture must export its interleaved blank row");
		Project reopened = loadFromBytes(archive);
		NormalTask restored = (NormalTask) firstTask(reopened);
		NormalTask restoredSecond = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Task candidate = (Task) tasks.next();
			if ("Materialized after blank row".equals(candidate.getName())) restoredSecond = (NormalTask) candidate;
		}
		assertEquals(start, restored.getStart());
		assertEquals(finish, restored.getEnd());
		assertEquals(start, restored.getActualStart());
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, restored.getConstraintTypeKind());
		assertEquals(constraintDate, restored.getConstraintDate());
		assertNotNull(restoredSecond, "the high-runtime-UID task after a blank row must be restored");
		assertEquals(secondStart, restoredSecond.getStart());
		assertEquals(secondFinish, restoredSecond.getEnd());
	}

	@Test
	void mpoTaskCreateOperationCanBeReplayedWithoutChangingTheResult() throws Exception {
		Project project = projectForRoundTrip();
		String operationId = java.util.UUID.randomUUID().toString();
		OperationLog.Operation operation = new OperationLog.Operation(operationId,
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 987654321L,
				"name", "Idempotent task", "notes", "", "percentComplete", 0D));
		com.microproject.collaboration.MpoTaskOperationService service = new com.microproject.collaboration.MpoTaskOperationService();
		int taskCountBefore = project.getTaskList().size();

		service.apply(project, List.of(operation));
		Task created = project.findByUniqueId(987654321L);
		service.apply(project, List.of(operation));

		assertEquals(taskCountBefore + 1, project.getTaskList().size());
		assertSame(created, project.findByUniqueId(987654321L));
		assertEquals("Idempotent task", created.getName());
	}

	@Test
	void mpoDocumentIdentitySurvivesSaveReloadSave() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream first = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, first);
		String firstManifest = new String(readEntries(first.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String firstDocumentId = firstManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		org.junit.jupiter.api.Assertions.assertNotEquals(firstManifest, firstDocumentId, "MPO must contain a UUID document identity");

		Project reopened = loadFromBytes(first.toByteArray());
		org.junit.jupiter.api.Assertions.assertEquals(firstDocumentId, reopened.getDocumentId());
		ByteArrayOutputStream second = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, second);
		String secondManifest = new String(readEntries(second.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String secondDocumentId = secondManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		org.junit.jupiter.api.Assertions.assertEquals(firstDocumentId, secondDocumentId);
	}

	@Test
	void repeatedMpoSaveUsesDeterministicArchiveEntryOrder() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream first = new ByteArrayOutputStream();
		ByteArrayOutputStream second = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, first);
		new MpoFileImporter().saveProject(project, second);
		Map<String, byte[]> firstEntries = readEntries(first.toByteArray());
		Map<String, byte[]> secondEntries = readEntries(second.toByteArray());
		assertEquals(firstEntries.keySet(), secondEntries.keySet());
	}

	@Test
	void masterMpoEmbedsLinkedProjectAndRestoresAUsableReference() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-linked-child-", ".mpo");
		childFile.deleteOnExit();
		child.setFileName(childFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}

		Project master = projectForRoundTrip();
		master.setMaster(true);
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setReferenceId("00000000-0000-0000-0000-000000000081");
		reference.setName("Embedded child");
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(childFile.toPath()), entries.get(embeddedEntry));

		Project reopened = loadFromBytes(archive.toByteArray());
		DefaultSubProj restored = null;
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj value) { restored = value; break; }
		}
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		org.junit.jupiter.api.Assertions.assertEquals(reference.getReferenceId(), restored.getReferenceId());
		org.junit.jupiter.api.Assertions.assertTrue(new File(restored.getSubprojectFile()).isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(childFile.toPath()),
				java.nio.file.Files.readAllBytes(new File(restored.getSubprojectFile()).toPath()));
	}

	@Test
	void fileImportOwnsExtractionUntilTheLoadedProjectIsClosed() throws Exception {
		File childFile = File.createTempFile("mpo-lifecycle-child-", ".mpo");
		File masterFile = File.createTempFile("mpo-lifecycle-master-", ".mpo");
		java.nio.file.Path extractionRoot = java.nio.file.Files.createTempDirectory("mpo-lifecycle-workspace-");
		try {
			Project child = projectForRoundTrip();
			child.setFileName(childFile.getAbsolutePath());
			MpoFileImporter childWriter = new MpoFileImporter();
			childWriter.setFileName(childFile.getAbsolutePath());
			childWriter.setProject(child);
			childWriter.exportFile();

			Project master = projectForRoundTrip();
			master.setMaster(true);
			addEmbeddedReference(master, child, childFile);
			MpoFileImporter masterWriter = new MpoFileImporter();
			masterWriter.setFileName(masterFile.getAbsolutePath());
			masterWriter.setProject(master);
			masterWriter.exportFile();

			MpoFileImporter.setExtractionWorkspaceRoot(extractionRoot);
			MpoFileImporter reader = new MpoFileImporter();
			reader.setFileName(masterFile.getAbsolutePath());
			reader.setProjectFactory(ProjectFactory.getInstance());
			reader.importFile();
			Project loaded = reader.getProject();
			DefaultSubProj restored = findSubproject(loaded);
			File extracted = new File(restored.getSubprojectFile());
			assertTrue(extracted.isFile(), "real file import must materialize the embedded child");
			assertEquals(1, MpoExtractionOwnershipRegistry.size());

			// This is the same ownership boundary used by DocumentFrame/GraphicManager
			// when the loaded project document is closed.
			assertTrue(MpoExtractionOwnershipRegistry.close(loaded));
			assertTrue(!extracted.exists(), "closing the project must remove extracted children");
			assertEquals(0, MpoExtractionOwnershipRegistry.size());
		} finally {
			MpoExtractionOwnershipRegistry.closeAll();
			MpoFileImporter.setExtractionWorkspaceRoot(null);
			try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(extractionRoot)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
					try { java.nio.file.Files.deleteIfExists(path); } catch (IOException ignored) { }
				});
			}
			java.nio.file.Files.deleteIfExists(childFile.toPath());
			java.nio.file.Files.deleteIfExists(masterFile.toPath());
		}
	}

	@Test
	void masterMpoKeepsTheMasterOpenWhenAnEmbeddedProjectIsTampered() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-linked-child-", ".mpo");
		childFile.deleteOnExit();
		child.setFileName(childFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		entries.put(embeddedEntry, new byte[] { 1, 2, 3 });
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		DefaultSubProj restored = findSubproject(reopened);
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, restored.getLoadStatus());
	}

	@Test
	void masterMpoKeepsTheMasterOpenWhenAnEmbeddedProjectEntryIsMissing() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-missing-child-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		entries.keySet().removeIf(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX));
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.MISSING, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void masterMpoRestoresAValidChildWhenAnotherEmbeddedChildIsTampered() throws Exception {
		Project validChild = projectForRoundTrip();
		File validChildFile = File.createTempFile("mpo-valid-child-", ".mpo");
		validChildFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(validChildFile)) {
			new MpoFileImporter().saveProject(validChild, output);
		}
		Project invalidChild = projectForRoundTrip();
		File invalidChildFile = File.createTempFile("mpo-invalid-child-", ".mpo");
		invalidChildFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(invalidChildFile)) {
			new MpoFileImporter().saveProject(invalidChild, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, validChild, validChildFile);
		addEmbeddedReference(master, invalidChild, invalidChildFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String invalidEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX + "2-")).findFirst().orElseThrow();
		entries.put(invalidEntry, new byte[] { 1, 2, 3 });
		Project reopened = loadFromBytes(zip(entries).toByteArray());
		java.util.List<DefaultSubProj> references = new java.util.ArrayList<>();
		for (java.util.Iterator<?> tasks = reopened.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) references.add(reference);
		}
		assertEquals(2, references.size());
		org.junit.jupiter.api.Assertions.assertTrue(references.stream().anyMatch(reference -> reference.getLoadStatus() == com.microproject.pm.task.SubProj.LoadStatus.INVALID));
		org.junit.jupiter.api.Assertions.assertTrue(references.stream().anyMatch(reference -> reference.getLoadStatus() == com.microproject.pm.task.SubProj.LoadStatus.NOT_LOADED
				&& new File(reference.getSubprojectFile()).isFile()));
	}

	@Test
	void masterMpoMarksAWellChecksummedButMalformedChildInvalid() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-malformed-child-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		Map<String, byte[]> malformed = readEntries(entries.get(embeddedEntry));
		byte[] malformedXml = "<Project>".getBytes(StandardCharsets.UTF_8);
		malformed.put(MpoFileImporter.PROJECT_ENTRY, malformedXml);
		malformed.put(MpoFileImporter.MANIFEST_ENTRY,
				MpoFileImporter.manifestFor(malformedXml).getBytes(StandardCharsets.UTF_8));
		byte[] malformedArchive = zip(malformed).toByteArray();
		String outerManifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		outerManifest = outerManifest.replace(sha256(childFile), sha256(malformedArchive));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, outerManifest.getBytes(StandardCharsets.UTF_8));
		entries.put(embeddedEntry, malformedArchive);

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void masterMpoMarksAChildWithTamperedStandardMetadataInvalid() throws Exception {
		Project child = projectForRoundTrip();
		File childFile = File.createTempFile("mpo-child-metadata-", ".mpo");
		childFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(childFile)) {
			new MpoFileImporter().saveProject(child, output);
		}
		Project master = projectForRoundTrip();
		master.setMaster(true);
		addEmbeddedReference(master, child, childFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Map<String, byte[]> entries = readEntries(archive.toByteArray());
		String embeddedEntry = entries.keySet().stream()
				.filter(name -> name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)).findFirst().orElseThrow();
		Map<String, byte[]> altered = readEntries(entries.get(embeddedEntry));
		altered.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		byte[] alteredArchive = zip(altered).toByteArray();
		String outerManifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
				.replace(sha256(childFile), sha256(alteredArchive));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, outerManifest.getBytes(StandardCharsets.UTF_8));
		entries.put(embeddedEntry, alteredArchive);

		Project reopened = loadFromBytes(zip(entries).toByteArray());
		assertEquals(com.microproject.pm.task.SubProj.LoadStatus.INVALID, findSubproject(reopened).getLoadStatus());
	}

	@Test
	void mpoEmbedsAndRestoresTheReferencedSharedResourcePoolFile() throws Exception {
		Project pool = projectForRoundTrip();
		File poolFile = File.createTempFile("mpo-resource-pool-", ".mpo");
		poolFile.deleteOnExit();
		pool.setFileName(poolFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(poolFile)) {
			new MpoFileImporter().saveProject(pool, output);
		}
		Project sharer = projectForRoundTrip();
		sharer.setSharedResourcePoolFile(poolFile.getAbsolutePath());
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(sharer, archive);

		Project reopened = loadFromBytes(archive.toByteArray());
		org.junit.jupiter.api.Assertions.assertTrue(new File(reopened.getSharedResourcePoolFile()).isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(poolFile.toPath()),
				java.nio.file.Files.readAllBytes(new File(reopened.getSharedResourcePoolFile()).toPath()));
	}

	@Test
	void sharedPoolSharerRoundTripPreservesResourceIdentityAndAssignmentUnits() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("shared-pool", undo);
		Resource resource = pool.newResourceInstance();
		resource.setResourceTypeKind(ResourceType.Kind.WORK);
		((com.microproject.pm.resource.ResourceImpl) resource).setUniqueId(88001L);
		Project poolProject = Project.createProject(pool, undo);
		File poolFile = File.createTempFile("mpo-shared-pool-", ".mpo");
		poolFile.deleteOnExit();
		poolProject.setFileName(poolFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(poolFile)) {
			new MpoFileImporter().saveProject(poolProject, output);
		}
		Project first = sharedSharer(pool, poolFile, resource, "First sharer task");
		Project second = sharedSharer(pool, poolFile, resource, "Second sharer task");
		SharedResourcePoolService.getInstance().share(first, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		SharedResourcePoolService.getInstance().share(second, poolProject,
				SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		File firstFile = File.createTempFile("mpo-shared-first-", ".mpo");
		File secondFile = File.createTempFile("mpo-shared-second-", ".mpo");
		firstFile.deleteOnExit();
		secondFile.deleteOnExit();
		first.setFileName(firstFile.getAbsolutePath());
		second.setFileName(secondFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(firstFile)) {
			new MpoFileImporter().saveProject(first, output);
		}
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(secondFile)) {
			new MpoFileImporter().saveProject(second, output);
		}
		Project reopenedPool = load(poolFile);
		reopenedPool.setFileName(poolFile.getAbsolutePath());
		Project reopenedFirst = load(firstFile);
		Project reopenedSecond = load(secondFile);
		Assignment firstAssignment = firstRealAssignment(reopenedFirst);
		Assignment secondAssignment = firstRealAssignment(reopenedSecond);
		assertEquals(88001L, firstAssignment.getResource().getUniqueId());
		assertEquals(firstAssignment.getResource().getUniqueId(), secondAssignment.getResource().getUniqueId());
		assertEquals(1D, firstAssignment.getUnits(), 0.0001D);
		assertEquals(1D, secondAssignment.getUnits(), 0.0001D);
		assertTrue(new File(reopenedFirst.getSharedResourcePoolFile()).isFile());
		assertTrue(new File(reopenedSecond.getSharedResourcePoolFile()).isFile());
		assertEquals(reopenedPool.getUniqueId(), reopenedFirst.getSharedResourcePoolProjectId());
		assertEquals(reopenedPool.getUniqueId(), reopenedSecond.getSharedResourcePoolProjectId());
		assertTrue(SharedResourcePoolService.getInstance().resolve(reopenedFirst,
				List.of(reopenedPool, reopenedSecond)));
		assertTrue(SharedResourcePoolService.getInstance().resolve(reopenedSecond,
				List.of(reopenedPool, reopenedFirst)));
		List<TeamPlannerService.Slot> slots = new TeamPlannerService().slots(reopenedPool);
		assertEquals(2, slots.size(), "restarted pool must aggregate both sharer assignments");
	}

	private static Project sharedSharer(ResourcePool pool, File poolFile, Resource resource, String taskName) {
		Project project = Project.createProject(pool, new DataFactoryUndoController());
		project.initialize(false, false);
		project.setSharedResourcePoolFile(poolFile.getAbsolutePath());
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(taskName);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, MpoFileImporterTest.class);
		return project;
	}

	private static byte[] saveProjectBytes(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, output);
		return output.toByteArray();
	}

	private static Assignment firstRealAssignment(Project project) {
		for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Object value = iterator.next();
			if (value instanceof NormalTask task)
				for (Object candidate : task.getAssignments()) {
					Assignment assignment = (Assignment) candidate;
					if (!assignment.isDefault()) return assignment;
				}
		}
		throw new AssertionError("No real assignment was restored");
	}

	@Test
	void portableMasterPreservesChildCrossProjectAndSharedPoolReferencesAfterReopen() throws Exception {
		Project target = projectForRoundTrip();
		target.setUniqueId(9102L);
		File targetFile = File.createTempFile("mpo-portable-target-", ".mpo");
		targetFile.deleteOnExit();
		target.setFileName(targetFile.getAbsolutePath());
		NormalTask targetTask = (NormalTask) target.createLocalTaskNode(null).getImpl();
		targetTask.setName("External dependency endpoint");
		targetTask.setExternalProjectFile(targetFile.getAbsolutePath());
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(targetFile)) {
			new MpoFileImporter().saveProject(target, output);
		}

		Project source = projectForRoundTrip();
		source.setUniqueId(9101L);
		NormalTask sourceTask = (NormalTask) source.createLocalTaskNode(null).getImpl();
		sourceTask.setName("Local cross-project successor");
		DependencyService.getInstance().newDependency(targetTask, sourceTask,
				com.microproject.pm.dependency.DependencyType.Kind.FS.code(), 0L, this);
		source.setSharedResourcePoolFile(targetFile.getAbsolutePath());
		File sourceFile = File.createTempFile("mpo-portable-source-", ".mpo");
		sourceFile.deleteOnExit();
		try (java.io.FileOutputStream output = new java.io.FileOutputStream(sourceFile)) {
			new MpoFileImporter().saveProject(source, output);
		}

		Project master = projectForRoundTrip();
		master.setUniqueId(9100L);
		master.setMaster(true);
		addEmbeddedReference(master, source, sourceFile);
		addEmbeddedReference(master, target, targetFile);
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(master, archive);
		Project reopened = loadFromBytes(archive.toByteArray());
		DefaultSubProj extractedSource = findSubprojectForId(reopened, source.getUniqueId());
		DefaultSubProj extractedTarget = findSubprojectForId(reopened, target.getUniqueId());
		Project reopenedSource;
		try (java.io.FileInputStream input = new java.io.FileInputStream(extractedSource.getSubprojectFile())) {
			reopenedSource = new MpoFileImporter().loadProject(input);
		}
		NormalTask restoredTask = null;
		for (java.util.Iterator<?> tasks = reopenedSource.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (value instanceof NormalTask task && "Local cross-project successor".equals(task.getName())) {
				restoredTask = task;
				break;
			}
		}
		org.junit.jupiter.api.Assertions.assertNotNull(restoredTask);
		org.junit.jupiter.api.Assertions.assertEquals(1, restoredTask.getPredecessorList().size());
		Task restoredExternal = (Task) ((com.microproject.pm.dependency.Dependency)
				restoredTask.getPredecessorList().iterator().next()).getPredecessor();
		org.junit.jupiter.api.Assertions.assertNotNull(restoredExternal.getExternalProjectFile(),
				() -> "restored external task=" + restoredExternal.getName() + ", external="
						+ restoredExternal.isExternal() + ", projectId=" + restoredExternal.getProjectId());
		File restoredExternalProject = new File(restoredExternal.getExternalProjectFile());
		org.junit.jupiter.api.Assertions.assertTrue(restoredExternalProject.isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(targetFile.toPath()),
				java.nio.file.Files.readAllBytes(restoredExternalProject.toPath()));
		File restoredPool = new File(reopenedSource.getSharedResourcePoolFile());
		org.junit.jupiter.api.Assertions.assertTrue(restoredPool.isFile());
		org.junit.jupiter.api.Assertions.assertArrayEquals(java.nio.file.Files.readAllBytes(targetFile.toPath()),
				java.nio.file.Files.readAllBytes(restoredPool.toPath()));
	}
	@Test
	void manifestValidatesTheExactProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> MpoFileImporter.validateManifest(MpoFileImporter.manifestFor(projectXml), projectXml));
	}

	@Test
	void manifestRejectsChangedProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		byte[] changedXml = "<Project><Name>changed</Name></Project>".getBytes(StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(MpoFileImporter.manifestFor(projectXml), changedXml));
	}

	@Test
	void mpoRejectsTamperedStandardArchiveEntry() throws Exception {
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(projectForRoundTrip(), generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		entries.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void manifestRejectsUnsupportedVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String unsupported = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"2.0\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(unsupported, projectXml));
	}

	@Test
	void manifestReportsAnUnsupportedMajorVersionExplicitly() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String newerMajor = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"2.3\"");
		IOException failure = assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(newerMajor, projectXml));
		org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("Unsupported MPOF format version 2.3"), failure.getMessage());
	}

	@Test
	void manifestRejectsMissingOrMalformedFormatVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String missing = MpoFileImporter.manifestFor(projectXml).replace(" formatVersion=\"1.0\"", "");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(missing, projectXml));
		String malformed = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"1\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(malformed, projectXml));
	}

	/** Issue #356: an mpo written by a different minor revision of the same major still opens. */
	@Test
	void mpoOpensAnOtherMinorRevisionAndUpgradesItToTheCurrentVersionOnSave() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("formatVersion=\"1.0\""), manifest);
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.replace("formatVersion=\"1.0\"", "formatVersion=\"1.7\"").getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNotNull(reopened);

		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(reopened, resaved);
		String upgraded = new String(readEntries(resaved.toByteArray()).get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(upgraded.contains("formatVersion=\"1.0\""), upgraded);
		org.junit.jupiter.api.Assertions.assertFalse(upgraded.contains("formatVersion=\"1.7\""), upgraded);
	}

	@Test
	void mpoOpensAnOtherMinorMetaRevisionButRejectsAnotherMajor() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String meta = new String(entries.get("meta.xml"), StandardCharsets.UTF_8);
		byte[] minorMeta = meta.replace("formatVersion=\"1.0\"", "formatVersion=\"1.4\"").getBytes(StandardCharsets.UTF_8);
		entries.put("meta.xml", minorMeta);
		updateManifestChecksum(entries, "meta.xml", minorMeta);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		org.junit.jupiter.api.Assertions.assertNotNull(reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));

		byte[] majorMeta = meta.replace("formatVersion=\"1.0\"", "formatVersion=\"9.0\"").getBytes(StandardCharsets.UTF_8);
		entries.put("meta.xml", majorMeta);
		updateManifestChecksum(entries, "meta.xml", majorMeta);
		MpoFileImporter rejecting = new MpoFileImporter();
		rejecting.setProjectFactory(ProjectFactory.getInstance());
		assertThrows(IOException.class, () -> rejecting.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void legacyMicroprojectMpoMigratesMillisecondLevelingDelayBeforeScheduling() throws Exception {
		Project project = projectForRoundTrip();
		long delay = 3L * 60L * 60L * 1000L;
		((NormalTask) firstTask(project)).setLevelingDelay(delay);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String currentXml = new String(entries.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(currentXml.contains("<LevelingDelay>"), currentXml);
		String legacyXml = currentXml.replaceFirst("<LevelingDelay>\\d+</LevelingDelay>", "<LevelingDelay>10800000</LevelingDelay>");
		entries.put(MpoFileImporter.PROJECT_ENTRY, legacyXml.getBytes(StandardCharsets.UTF_8));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, MpoFileImporter.manifestFor(entries.get(MpoFileImporter.PROJECT_ENTRY)).getBytes(StandardCharsets.UTF_8));
		String legacyMeta = new String(entries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8)
				.replace(" levelingDelayUnit=\"minutes\"", "");
		entries.put(MpoFileImporter.META_ENTRY, legacyMeta.getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(delay, firstTask(loaded).getLevelingDelay());

		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(loaded, resaved);
		String migratedMeta = new String(readEntries(resaved.toByteArray()).get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(migratedMeta.contains("levelingDelayUnit=\"minutes\""), migratedMeta);
	}

	@Test
	void legacyMicroprojectMpoMigratesAssignmentLevelingDelay() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("Legacy assignment resource");
		com.microproject.pm.assignment.Assignment assignment =
			AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, null, false);
		long delay = 2L * 60L * 60L * 1000L;
		assignment.setLevelingDelay(delay);

		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		String currentXml = new String(entries.get(MpoFileImporter.PROJECT_ENTRY), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(currentXml.contains("<LevelingDelay>1200</LevelingDelay>"), currentXml);
		String legacyXml = currentXml.replace("<LevelingDelay>1200</LevelingDelay>", "<LevelingDelay>7200000</LevelingDelay>");
		entries.put(MpoFileImporter.PROJECT_ENTRY, legacyXml.getBytes(StandardCharsets.UTF_8));
		entries.put(MpoFileImporter.MANIFEST_ENTRY, MpoFileImporter.manifestFor(entries.get(MpoFileImporter.PROJECT_ENTRY)).getBytes(StandardCharsets.UTF_8));
		String legacyMeta = new String(entries.get(MpoFileImporter.META_ENTRY), StandardCharsets.UTF_8)
			.replace(" levelingDelayUnit=\"minutes\"", "");
		entries.put(MpoFileImporter.META_ENTRY, legacyMeta.getBytes(StandardCharsets.UTF_8));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		NormalTask loadedTask = (NormalTask) firstTask(loaded);
		com.microproject.pm.assignment.Assignment loadedAssignment =
			(com.microproject.pm.assignment.Assignment) loadedTask.getAssignments().iterator().next();
		org.junit.jupiter.api.Assertions.assertEquals("Legacy assignment resource", loadedAssignment.getResource().getName());
		org.junit.jupiter.api.Assertions.assertEquals(delay, loadedAssignment.getLevelingDelay());
	}

	@Test
	void manifestRejectsDuplicateRequiredFields() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String duplicate = MpoFileImporter.manifestFor(projectXml).replace("format=\"mpof\"", "format=\"mpof\" format=\"mpof\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(duplicate, projectXml));
	}

	@Test
	void mpoRejectsMalformedOperationLogsBeforeLoadingTheSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		entries.put("operations/log.jsonl", "{\"type\":\"header\",\"schemaVersion\":1,\"documentId\":\"not-a-uuid\"}\n".getBytes(StandardCharsets.UTF_8));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		assertThrows(IOException.class, () -> reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void mpoDoesNotReplayTaskUpdatesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation update = new OperationLog.Operation("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000012", 1, java.util.Set.of(), "task.update", "00000000-0000-0000-0000-000000000013", Map.of("legacyUniqueId", Long.valueOf(task.getUniqueId()), "name", "Merged task"));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(update));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(task.getName(), firstTask(loaded).getName());
		ByteArrayOutputStream resaved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(loaded, resaved);
		java.util.List<OperationLog.Operation> retained = new OperationLog().readJsonl(
				readEntries(resaved.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals(1, retained.size());
		org.junit.jupiter.api.Assertions.assertEquals("task.update", retained.get(0).kind());
	}

	@Test
	void mpoDoesNotReplayTaskCreatesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation create = new OperationLog.Operation("00000000-0000-0000-0000-000000000021", "00000000-0000-0000-0000-000000000022", 1, java.util.Set.of(), "task.create", "00000000-0000-0000-0000-000000000023", Map.of("legacyUniqueId", Long.valueOf(9001L), "name", "Created task"));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(create, create));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNull(loaded.findByUniqueId(9001L));
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(project), taskCount(loaded));
	}

	@Test
	void mpoDoesNotReplayTaskDeletesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		long taskId = firstTask(project).getUniqueId();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation delete = new OperationLog.Operation("00000000-0000-0000-0000-000000000031", "00000000-0000-0000-0000-000000000032", 1, java.util.Set.of(), "task.delete", "00000000-0000-0000-0000-000000000033", Map.of("legacyUniqueId", Long.valueOf(taskId)));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(delete, delete));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(project), taskCount(loaded));
	}

	@Test
	void mpoSaveAppendsTaskUpdatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) firstTask(project)).setName("Edited after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("operations/log.jsonl");
		org.junit.jupiter.api.Assertions.assertEquals(1, new OperationLog().readJsonl(operations).operations().size());
	}

	@Test
	void mpoSaveAppendsTaskCreatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) project.createLocalTaskNode(null).getImpl()).setName("Added after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("operations/log.jsonl");
		org.junit.jupiter.api.Assertions.assertEquals("task.create", new OperationLog().readJsonl(operations).operations().get(0).kind());
	}

	@Test
	void mpoDoesNotReplayTaskMovesOverItsMaterializedSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		assignPositiveUniqueIds(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream(); new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation move = new OperationLog.Operation("00000000-0000-0000-0000-000000000041", "00000000-0000-0000-0000-000000000042", 1, java.util.Set.of(), "task.move", "00000000-0000-0000-0000-000000000043", Map.of("legacyUniqueId", Long.valueOf(child.getUniqueId()), "parentLegacyUniqueId", Long.valueOf(parent.getUniqueId())));
		byte[] operations = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(move));
		entries.put("operations/log.jsonl", operations);
		updateManifestChecksum(entries, "operations/log.jsonl", operations);
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNotSame(loaded.findByUniqueId(parent.getUniqueId()),
				loaded.findByUniqueId(child.getUniqueId()).getWbsParentTask());
	}

	@Test
	void mpoSaveAppendsTaskMovesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		project.setLocalParent(child, parent);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("task.move", operations.get(0).kind());
		org.junit.jupiter.api.Assertions.assertEquals(parent.getUniqueId(), ((Number) operations.get(0).payload().get("parentLegacyUniqueId")).longValue());
	}

	@Test
	void mpoSavesAndReplaysDependencyOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(project);
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl(); successor.setName("Successor");
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("dependency.add", operations.get(0).kind());
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		org.junit.jupiter.api.Assertions.assertTrue(findByName(loaded, predecessor.getName()).getSuccessorList().iterator().hasNext());
	}

	@Test
	void mpoSavesAndReplaysAssignmentOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		Resource resource = project.getResourcePool().newResourceInstance(); resource.setName("Engineer");
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, null, false);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(readEntries(second.toByteArray()).get("operations/log.jsonl")).operations();
		org.junit.jupiter.api.Assertions.assertEquals("assignment.add", operations.get(0).kind());
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		org.junit.jupiter.api.Assertions.assertTrue(((NormalTask) loaded.findByUniqueId(task.getUniqueId())).getAssignments().iterator().hasNext());
	}

	@Test
	void mpoSequentialSharedFolderSavesMergeIndependentTaskEdits() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-shared", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(new File(shared.getAbsolutePath() + ".lock").isFile(), "shared saves use a stable transaction lock");
		Project firstEditor = load(shared); Project secondEditor = load(shared);
		firstTask(firstEditor).setName("First editor");
		secondEditor.findByUniqueId(second.getUniqueId()).setName("Second editor");
		MpoFileImporter firstWriter = new MpoFileImporter(); firstWriter.setFileName(shared.getAbsolutePath()); firstWriter.setProject(firstEditor); firstWriter.exportFile();
		MpoFileImporter secondWriter = new MpoFileImporter(); secondWriter.setFileName(shared.getAbsolutePath()); secondWriter.setProject(secondEditor); secondWriter.exportFile();
		Project merged = load(shared);
		org.junit.jupiter.api.Assertions.assertEquals("First editor", firstTask(merged).getName());
		org.junit.jupiter.api.Assertions.assertEquals("Second editor", merged.findByUniqueId(second.getUniqueId()).getName());
	}

	@Test
	void mpoSaveAfterHierarchyChangeAcceptsAnEquivalentArchiveOperationWithDifferentNumberTypes() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-number-normalization", ".mpo");
		shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter();
		initialWriter.setFileName(shared.getAbsolutePath());
		initialWriter.setProject(initial);
		initialWriter.exportFile();

		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		OperationLog.Operation archiveOperation = new OperationLog.Operation(
				"00000000-0000-0000-0000-000000000081", "00000000-0000-0000-0000-000000000082", 1,
				java.util.Set.of(), "assignment.delete", "00000000-0000-0000-0000-000000000083",
				Map.of("taskLegacyUniqueId", Integer.valueOf(1), "resourceUniqueId", Integer.valueOf(1)));
		byte[] archiveLog = new OperationLog().writeJsonl(manifestDocumentId(entries), java.util.List.of(archiveOperation));
		entries.put(MpoFileImporter.OPERATIONS_ENTRY, archiveLog);
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, archiveLog);
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());

		Project editor = load(shared);
		NormalTask child = (NormalTask) firstTask(editor);
		NormalTask parent = (NormalTask) editor.createLocalTaskNode(null).getImpl();
		parent.setName("New parent");
		parent.setUniqueId(-91001L);
		editor.setLocalParent(child, parent);
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(shared.getAbsolutePath());
		writer.setProject(editor);

		assertDoesNotThrow(writer::exportFile);
		Project reloaded = load(shared);
		Task reloadedChild = reloaded.findByUniqueId(child.getUniqueId());
		assertNotNull(reloadedChild, "persisted child UID must survive a hierarchy change");
		assertEquals("New parent", reloadedChild.getWbsParentTask().getName());
	}

	@Test
	void mpoConcurrentSharedFolderSavesSerializeAndMergeBothEditors() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-concurrent", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project firstEditor = load(shared); Project secondEditor = load(shared);
		firstTask(firstEditor).setName("Concurrent first");
		secondEditor.findByUniqueId(second.getUniqueId()).setName("Concurrent second");
		java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		try {
			java.util.concurrent.Future<?> first = executor.submit(() -> saveAfter(start, shared, firstEditor));
			java.util.concurrent.Future<?> secondSave = executor.submit(() -> saveAfter(start, shared, secondEditor));
			start.countDown();
			first.get(); secondSave.get();
		} finally {
			executor.shutdownNow();
		}
		Project merged = load(shared);
		org.junit.jupiter.api.Assertions.assertEquals("Concurrent first", firstTask(merged).getName());
		org.junit.jupiter.api.Assertions.assertEquals("Concurrent second", merged.findByUniqueId(second.getUniqueId()).getName());
	}

	@Test
	void mpoSameFieldConflictKeepsSharedFileAndWritesReloadableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-conflict-recovery", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Project firstEditor = load(shared);
		Project secondEditor = load(shared);
		firstTask(firstEditor).setName("First committed value");
		firstTask(secondEditor).setName("Second recoverable value");
		MpoFileImporter firstWriter = new MpoFileImporter();
		firstWriter.setFileName(shared.getAbsolutePath());
		firstWriter.setProject(firstEditor);
		firstWriter.exportFile();
		byte[] committedArchive = Files.readAllBytes(shared.toPath());

		MpoFileImporter secondWriter = new MpoFileImporter();
		secondWriter.setFileName(shared.getAbsolutePath());
		secondWriter.setProject(secondEditor);
		MpoConflictRecoveryException conflict = assertThrows(MpoConflictRecoveryException.class, secondWriter::exportFile);
		Path recoveryCopy = conflict.recoveryCopy();
		Path repeatedRecoveryCopy = null;
		try {
			assertEquals(1, conflict.conflicts().size());
			assertTrue(conflict.getMessage().contains(recoveryCopy.toString()));
			org.junit.jupiter.api.Assertions.assertArrayEquals(committedArchive, Files.readAllBytes(shared.toPath()),
				"the shared archive must remain unchanged when same-field edits conflict");
			assertEquals("Second recoverable value", firstTask(secondEditor).getName(),
				"the losing editor's in-memory value must remain available");
			assertTrue(Files.isRegularFile(recoveryCopy));
			Project recovered = load(recoveryCopy.toFile());
			assertEquals("Second recoverable value", firstTask(recovered).getName());
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(2, recoveryLog.operations().size(), "the recovery journal must retain both concurrent operations");
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
			assertEquals(1, recoveryLog.appliedOperationIds().size(),
				"the recovery snapshot may advertise only the losing editor's materialized branch");
			MpoConflictRecoveryException retryConflict = assertThrows(MpoConflictRecoveryException.class, secondWriter::exportFile);
			repeatedRecoveryCopy = retryConflict.recoveryCopy();
			OperationLog.DocumentLog retryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(repeatedRecoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(recoveryLog.operations().stream().map(OperationLog.Operation::id).collect(java.util.stream.Collectors.toSet()),
				retryLog.operations().stream().map(OperationLog.Operation::id).collect(java.util.stream.Collectors.toSet()),
				"a retry must reuse the same operation IDs instead of appending duplicate edits");
		} finally {
			Files.deleteIfExists(recoveryCopy);
			if (repeatedRecoveryCopy != null) Files.deleteIfExists(repeatedRecoveryCopy);
		}
	}

	@Test
	void concurrentDependencyLagChangesShareRelationshipIdentityAndConflict() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(initial);
		NormalTask successor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		successor.setName("Successor");
		assignPositiveUniqueIds(initial);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		File shared = File.createTempFile("mpo-dependency-conflict", ".mpo");
		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		try {
			MpoFileImporter seed = new MpoFileImporter();
			seed.setFileName(shared.getAbsolutePath());
			seed.setProject(initial);
			seed.exportFile();
			Project left = load(shared);
			Project right = load(shared);
			changeDependencyLag(left, predecessor.getUniqueId(), successor.getUniqueId(), 1000L);
			changeDependencyLag(right, predecessor.getUniqueId(), successor.getUniqueId(), 2000L);
			byte[] leftArchive = saveMpo(left);
			byte[] rightArchive = saveMpo(right);
			List<OperationLog.Operation> concurrent = new ArrayList<>(new OperationLog().readJsonl(
					readEntries(leftArchive).get(MpoFileImporter.OPERATIONS_ENTRY)).operations());
			concurrent.addAll(new OperationLog().readJsonl(
					readEntries(rightArchive).get(MpoFileImporter.OPERATIONS_ENTRY)).operations());
			OperationLog.MergeResult merged = new OperationLog().merge(concurrent);
			List<OperationLog.Operation> additions = concurrent.stream().filter(operation ->
					"dependency.add".equals(operation.kind())).toList();
			assertEquals(2, additions.size());
			assertEquals(additions.get(0).entityId(), additions.get(1).entityId(),
				"lag is mutable relationship data and must not split the entity identity");
			assertFalse(merged.conflicts().isEmpty(), "concurrent edits to one dependency must not both merge");
		} finally {
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void mpoLockOpenFailurePreservesLocalEditsInReloadableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		File shared = File.createTempFile("mpo-lock-recovery", ".mpo");
		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();
		byte[] sharedArchive = Files.readAllBytes(shared.toPath());
		Project editor = load(shared);
		firstTask(editor).setName("Recover after lock failure");
		Files.delete(lockPath);
		Files.createDirectory(lockPath);
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(shared.getAbsolutePath());
		writer.setProject(editor);
		try {
			MpoConflictRecoveryException failure = assertThrows(MpoConflictRecoveryException.class, writer::exportFile);
			assertTrue(failure.conflicts().isEmpty(), "a lock failure is not an operation conflict");
			assertTrue(failure.getMessage().contains("transaction lock"));
			Path recoveryCopy = failure.recoveryCopy();
			try {
				assertTrue(Files.isRegularFile(recoveryCopy));
				org.junit.jupiter.api.Assertions.assertArrayEquals(sharedArchive, Files.readAllBytes(shared.toPath()));
				assertEquals("Recover after lock failure", firstTask(load(recoveryCopy.toFile())).getName());
				OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
						readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
				assertEquals(1, recoveryLog.operations().size(), "the local edit must be recoverable as an operation");
				assertEquals(1, recoveryLog.appliedOperationIds().size());
			} finally {
				Files.deleteIfExists(recoveryCopy);
			}
		} finally {
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmSavesMergeIndependentTaskEdits() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		second.setName("Second");
		assignPositiveUniqueIds(initial);
		long firstId = firstTask(initial).getUniqueId();
		long secondId = second.getUniqueId();
		File shared = File.createTempFile("mpo-process-concurrent", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = release.resolveSibling("first-ready.flag");
		Path secondReady = release.resolveSibling("second-ready.flag");
		Path firstLog = release.resolveSibling("first-worker.log");
		Path secondLog = release.resolveSibling("second-worker.log");
		Process first = null;
		Process secondProcess = null;
		try {
			first = startMpoSaveWorker(shared, firstId, "Process first", firstReady, release, firstLog);
			secondProcess = startMpoSaveWorker(shared, secondId, "Process second", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(secondProcess, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(secondProcess, secondLog);
			Project merged = load(shared);
			assertEquals("Process first", merged.findByUniqueId(firstId).getName());
			assertEquals("Process second", merged.findByUniqueId(secondId).getName());
		} finally {
			stopWorker(first);
			stopWorker(secondProcess);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	@Test
	void mpoRoundTripPreservesReadyButUnappliedOperationGeneration() throws Exception {
		Project source = projectForRoundTrip();
		ByteArrayOutputStream initial = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(source, initial));
		Map<String, byte[]> entries = readEntries(initial.toByteArray());
		String documentId = source.getDocumentId();
		String actorId = java.util.UUID.randomUUID().toString();
		String operationId = java.util.UUID.randomUUID().toString();
		String entityId = java.util.UUID.randomUUID().toString();
		OperationLog.Operation unapplied = new OperationLog.Operation(operationId, actorId, 1L, java.util.Set.of(),
			"task.update", entityId, Map.of("legacyUniqueId", firstTask(source).getUniqueId(), "name", "Held conflict value"));
		byte[] operationLog = new OperationLog().writeJsonl(documentId, List.of(unapplied), java.util.Set.of());
		entries.put(MpoFileImporter.OPERATIONS_ENTRY, operationLog);
		updateManifestChecksum(entries, MpoFileImporter.OPERATIONS_ENTRY, operationLog);

		File target = File.createTempFile("mpo-unapplied-generation", ".mpo");
		target.deleteOnExit();
		Files.write(target.toPath(), zip(entries).toByteArray());
		Project loaded = load(target);
		assertEquals(java.util.Set.of(), new OperationLog().readJsonl(
			readEntries(Files.readAllBytes(target.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY)).appliedOperationIds());
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(target.getAbsolutePath());
		writer.setProject(loaded);
		writer.exportFile();

		OperationLog.DocumentLog persisted = new OperationLog().readJsonl(
			readEntries(Files.readAllBytes(target.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY));
		assertEquals(java.util.Set.of(operationId), persisted.operations().stream().map(OperationLog.Operation::id)
			.collect(java.util.stream.Collectors.toSet()));
		assertEquals(java.util.Set.of(), persisted.appliedOperationIds());
		assertEquals("Mpo task", firstTask(load(target)).getName());
	}

	@Test
	void mpoTaskCreateOperationRetainsItsParentWhenReplayed() throws Exception {
		Project project = projectForRoundTrip();
		assignPositiveUniqueIds(project);
		MpoFileImporter writer = new MpoFileImporter();
		ByteArrayOutputStream initial = new ByteArrayOutputStream();
		writer.saveProject(project, initial);
		Project base = new MpoFileImporter().loadProject(new ByteArrayInputStream(initial.toByteArray()));

		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl();
		parent.setName("Created parent");
		parent.setUniqueId(-91002L);
		NormalTask child = (NormalTask) project.createLocalTaskNode(null).getImpl();
		child.setName("Created child");
		child.setUniqueId(-91003L);
		project.setLocalParent(child, parent);
		ByteArrayOutputStream changed = new ByteArrayOutputStream();
		writer.saveProject(project, changed);

		java.util.List<OperationLog.Operation> operations = new OperationLog().readJsonl(
				readEntries(changed.toByteArray()).get(MpoFileImporter.OPERATIONS_ENTRY)).operations();
		new com.microproject.collaboration.MpoTaskOperationService().apply(base, operations);
		Task replayedChild = base.getTaskList().stream()
			.filter(task -> "Created child".equals(task.getName())).findFirst().orElseThrow();
		assertEquals("Created parent", replayedChild.getWbsParentTask().getName());
		assertEquals(3, base.getTaskList().size(), "replay must retain the original task and both new tasks");
	}

	@Test
	void mpoReplayUsesAlreadyAppliedParentsAsCausalContextWithoutApplyingThemTwice() throws Exception {
		Project project = projectForRoundTrip();
		OperationLog.Operation parent = new OperationLog.Operation(java.util.UUID.randomUUID().toString(),
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 1001L, "name", "Parent",
				"notes", "", "percentComplete", 0D));
		OperationLog.Operation child = new OperationLog.Operation(java.util.UUID.randomUUID().toString(),
			java.util.UUID.randomUUID().toString(), 1L, java.util.Set.of(parent.id()), "task.create",
			java.util.UUID.randomUUID().toString(), Map.of("legacyUniqueId", 1002L, "name", "Child",
				"notes", "", "percentComplete", 0D, "parentLegacyUniqueId", 1001L));
		com.microproject.collaboration.MpoTaskOperationService service = new com.microproject.collaboration.MpoTaskOperationService();
		service.apply(project, List.of(parent));
		Task existingParent = project.findByUniqueId(1001L);

		service.apply(project, List.of(parent, child), java.util.Set.of(parent.id()));

		assertEquals(existingParent, project.findByUniqueId(1001L));
		assertEquals(1001L, project.findByUniqueId(1002L).getWbsParentTask().getUniqueId());
	}

	@Test
	void separateJvmSavesMergeDifferentFieldsOnTheSameTask() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-field-merge", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-fields-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = release.resolveSibling("first-ready.flag");
		Path secondReady = release.resolveSibling("second-ready.flag");
		Path firstLog = release.resolveSibling("first-worker.log");
		Path secondLog = release.resolveSibling("second-worker.log");
		Process first = null;
		Process second = null;
		try {
			first = startMpoSaveWorker(shared, taskId, "name", "Process name", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, "notes", "Process notes", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			Task mergedTask = load(shared).findByUniqueId(taskId);
			assertEquals("Process name", mergedTask.getName());
			assertEquals("Process notes", mergedTask.getNotes());
		} finally {
			stopWorker(first);
			stopWorker(second);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	@Test
	void separateJvmSameFieldConflictReturnsDurableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-conflict", ".mpo");
		shared.deleteOnExit();
		new File(shared.getAbsolutePath() + ".lock").deleteOnExit();
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			first = startMpoSaveWorker(shared, taskId, "name", "Process winner A", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, "name", "Process winner B", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			String firstOutput = readWorkerLog(firstLog);
			String secondOutput = readWorkerLog(secondLog);
			List<String> recoveryMarkers = java.util.stream.Stream.of(firstOutput, secondOutput)
				.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY="))
				.toList();
			assertEquals(1, recoveryMarkers.size(), "exactly one process should produce a conflict recovery copy");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			String sharedValue = firstTask(load(shared)).getName();
			String recoveredValue = firstTask(load(recoveryCopy.toFile())).getName();
			assertTrue(java.util.Set.of("Process winner A", "Process winner B").contains(sharedValue));
			assertTrue(java.util.Set.of("Process winner A", "Process winner B").contains(recoveredValue));
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedValue, recoveredValue);
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
				readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertEquals(2, recoveryLog.operations().size());
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
		}
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String name, Path ready,
			Path release, Path log) throws Exception {
		return startMpoSaveWorker(shared, taskId, "name", name, ready, release, log);
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String field, String value,
			Path ready, Path release, Path log) throws Exception {
		return startMpoSaveWorker(shared, taskId, field, value, ready, release, log, "");
	}

	private static Process startMpoSaveWorker(File shared, long taskId, String field, String value,
			Path ready, Path release, Path log, String crashPoint) throws Exception {
		String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win") ? "java.exe" : "java")
			.toString();
		List<String> command = new ArrayList<>(List.of(javaExecutable, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-cp",
				processTestClasspath(), MpoConcurrentSaveProcess.class.getName(), shared.getAbsolutePath(),
				Long.toString(taskId), field, value, ready.toString(), release.toString()));
		if (!crashPoint.isEmpty()) command.add(crashPoint);
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		builder.redirectOutput(log.toFile());
		return builder.start();
	}

	private static String processTestClasspath() throws Exception {
		java.util.LinkedHashSet<String> entries = new java.util.LinkedHashSet<>();
		String systemClasspath = System.getProperty("java.class.path", "");
		for (String entry : systemClasspath.split(java.util.regex.Pattern.quote(File.pathSeparator)))
			if (!entry.isBlank()) entries.add(entry);
		for (ClassLoader loader = MpoFileImporterTest.class.getClassLoader(); loader != null; loader = loader.getParent()) {
			if (loader instanceof java.net.URLClassLoader urlClassLoader) {
				for (java.net.URL url : urlClassLoader.getURLs()) {
					if ("file".equalsIgnoreCase(url.getProtocol()))
						entries.add(Path.of(url.toURI()).toString());
				}
			}
		}
		return String.join(File.pathSeparator, entries);
	}

	private static void awaitWorkerReady(Process worker, Path ready, Path log) throws Exception {
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
		while (!Files.exists(ready)) {
			if (!worker.isAlive())
				throw new AssertionError("MPO worker exited before loading its editor snapshot: " + readWorkerLog(log));
			if (System.nanoTime() >= deadline)
				throw new AssertionError("MPO worker did not load its editor snapshot in time: " + readWorkerLog(log));
			Thread.sleep(20L);
		}
	}

	private static void assertWorkerSucceeded(Process worker, Path log) throws Exception {
		org.junit.jupiter.api.Assertions.assertTrue(worker.waitFor(30, java.util.concurrent.TimeUnit.SECONDS),
				"MPO worker did not finish: " + readWorkerLog(log));
		assertEquals(0, worker.exitValue(), "MPO worker failed: " + readWorkerLog(log));
	}

	private static void stopWorker(Process worker) throws InterruptedException {
		if (worker == null || !worker.isAlive())
			return;
		worker.destroyForcibly();
		worker.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
	}

	private static String readWorkerLog(Path log) throws IOException {
		return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
	}

	private static void saveAfter(java.util.concurrent.CountDownLatch start, File shared, Project editor) {
		try {
			start.await();
			MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
	}

	@Test
	void mpoSharedFolderSavePreservesExtensionAddedAfterEditorOpened() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-extension-merge", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		entries.put("vendor/remote.bin", new byte[] { 7, 8, 9 });
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("Edited locally");
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 7, 8, 9 }, readEntries(java.nio.file.Files.readAllBytes(shared.toPath())).get("vendor/remote.bin"));
	}

	@Test
	void mpoSharedFolderSaveRejectsATamperedExistingArchiveBeforeReplacingIt() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-checksum-merge", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		entries.put("meta.xml", "<meta formatVersion=\"1.0\" tampered=\"true\"/>".getBytes(StandardCharsets.UTF_8));
		byte[] tampered = zip(entries).toByteArray();
		java.nio.file.Files.write(shared.toPath(), tampered);
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor);
		assertThrows(IOException.class, writer::exportFile);
		org.junit.jupiter.api.Assertions.assertArrayEquals(tampered, java.nio.file.Files.readAllBytes(shared.toPath()));
	}

	@Test
	void mpoAtomicMoveFailureKeepsOriginalArchiveAndInMemoryProject() throws Exception {
		Project initial = projectForRoundTrip();
		File target = File.createTempFile("mpo-atomic-failure-", ".mpo");
		target.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter();
		initialWriter.setFileName(target.getAbsolutePath());
		initialWriter.setProject(initial);
		initialWriter.exportFile();
		byte[] originalBytes = java.nio.file.Files.readAllBytes(target.toPath());
		Project edited = load(target);
		firstTask(edited).setName("Edit retained after failed replacement");
		assertTrue(edited.needsSaving(), "fixture edit must remain dirty before the injected move failure");

		MpoFileImporter failingWriter = new MpoFileImporter() {
			@Override
			protected void moveTemporary(java.nio.file.Path temporary, java.nio.file.Path destination) throws IOException {
				throw new IOException("injected atomic move failure");
			}
		};
		failingWriter.setFileName(target.getAbsolutePath());
		failingWriter.setProject(edited);
		assertThrows(IOException.class, failingWriter::exportFile);
		org.junit.jupiter.api.Assertions.assertArrayEquals(originalBytes,
				java.nio.file.Files.readAllBytes(target.toPath()), "failed replacement must preserve original MPO bytes");
		assertTrue(edited.needsSaving(), "failed replacement must retain the dirty in-memory project");
		assertEquals("Edit retained after failed replacement", firstTask(edited).getName());
	}

	@Test
	void mpoSaveCleansOnlyStaleStagesForItsOwnTarget() throws Exception {
		Project initial = projectForRoundTrip();
		File target = File.createTempFile("mpo-stale-stage-cleanup-", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(target.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path parent = target.toPath().toAbsolutePath().getParent();
		Path staleStage = parent.resolve(target.getName() + ".abc123.tmp");
		Path recentStage = parent.resolve(target.getName() + ".def456.tmp");
		Path otherTargetStage = parent.resolve(target.getName() + ".other.ghi789.tmp");
		Files.write(staleStage, new byte[] { 1 });
		Files.write(recentStage, new byte[] { 2 });
		Files.write(otherTargetStage, new byte[] { 3 });
		Files.setLastModifiedTime(staleStage, java.nio.file.attribute.FileTime.fromMillis(
				System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(2)));

		try {
			Project edited = load(target);
			firstTask(edited).setName("Save after abandoned stage cleanup");
			MpoFileImporter writer = new MpoFileImporter();
			writer.setFileName(target.getAbsolutePath());
			writer.setProject(edited);
			writer.exportFile();
			assertFalse(Files.exists(staleStage), "old stage for this target should be removed under its lock");
			assertTrue(Files.exists(recentStage), "a fresh stage must not be mistaken for a crashed writer artifact");
			assertTrue(Files.exists(otherTargetStage), "a stage belonging to a different target must be preserved");
			assertEquals("Save after abandoned stage cleanup", firstTask(load(target)).getName());
		} finally {
			Files.deleteIfExists(staleStage);
			Files.deleteIfExists(recentStage);
			Files.deleteIfExists(otherTargetStage);
			Files.deleteIfExists(Path.of(target.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(target.toPath());
		}
	}

	@Test
	void mpoSharedFolderRejectsManifestDocumentMismatchBeforeMerge() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("mpo-manifest-mismatch", ".mpo"); shared.deleteOnExit();
		MpoFileImporter initialWriter = new MpoFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
			.replaceFirst("documentId=\\\"[^\\\"]+\\\"", "documentId=\\\"00000000-0000-0000-0000-000000000099\\\"");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("edited");
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor);
		assertThrows(IOException.class, writer::exportFile);
	}

	@Test
	void mpoContainerUsesOdfStyleLayout() throws Exception {
		Project original = projectForRoundTrip();
		File mpo = File.createTempFile("mpo-layout", ".mpo"); mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter(); writer.setFileName(mpo.getAbsolutePath()); writer.setProject(original); writer.exportFile();
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(mpo.toPath()));
		org.junit.jupiter.api.Assertions.assertEquals("application/vnd.microproject.openproject",
			new String(entries.get("mimetype"), StandardCharsets.UTF_8).trim());
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("META-INF/manifest.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("meta.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("content.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("settings.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("ccpm/history.jsonl"));
		org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("operations/log.jsonl"));
		org.junit.jupiter.api.Assertions.assertTrue(new String(entries.get("meta.xml"), StandardCharsets.UTF_8).contains("<meta "));
		String manifest = new String(entries.get("META-INF/manifest.xml"), StandardCharsets.UTF_8);
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("format=\"mpof\""));
		org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("formatVersion=\"1.0\""));
	}

	@Test
	void mpoReadsEarlierDraftLayoutAndWritesCurrentLayout() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		String xmlManifest = new String(entries.remove(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String sha256 = xmlManifest.replaceFirst("(?s).*projectSha256=\"([^\"]+)\".*", "$1");
		String documentId = xmlManifest.replaceFirst("(?s).*documentId=\"([0-9a-f-]{36})\".*", "$1");
		entries.remove("meta.xml"); entries.remove("settings.xml");
		byte[] jsonl = entries.remove("operations/log.jsonl");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, ("{\"format\":\"mpof\",\"formatVersion\":\"1.0\",\"projectEntry\":\"content.xml\",\"projectSha256\":\"" + sha256 + "\",\"documentId\":\"" + documentId + "\"}\n").getBytes(StandardCharsets.UTF_8));
		entries.put("changes/operations.json", new OperationLog().write(documentId, new OperationLog().readJsonl(jsonl).operations()));
		Project loaded = loadFromBytes(zip(entries).toByteArray());
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(original), taskCount(loaded));
		ByteArrayOutputStream rewritten = new ByteArrayOutputStream(); new MpoFileImporter().saveProject(loaded, rewritten);
		Map<String, byte[]> rewrittenEntries = readEntries(rewritten.toByteArray());
		org.junit.jupiter.api.Assertions.assertTrue(rewrittenEntries.containsKey("meta.xml"));
		org.junit.jupiter.api.Assertions.assertTrue(rewrittenEntries.containsKey("operations/log.jsonl"));
	}

	@Test
	void draftManifestMustIdentifyTheProjectSnapshotEntry() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		String xmlManifest = new String(entries.remove(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String sha256 = xmlManifest.replaceFirst("(?s).*projectSha256=\\\"([^\\\"]+)\\\".*", "$1");
		String documentId = xmlManifest.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
		entries.remove("meta.xml"); entries.remove("settings.xml"); entries.remove("operations/log.jsonl");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, ("{\"format\":\"mpof\",\"formatVersion\":\"1.0\",\"projectEntry\":\"wrong.xml\",\"projectSha256\":\"" + sha256 + "\",\"documentId\":\"" + documentId + "\"}\n").getBytes(StandardCharsets.UTF_8));
		org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void currentAndDraftCcpmSettingsCannotBeMixed() throws Exception {
		Project original = projectForRoundTrip();
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(original, current);
		Map<String, byte[]> entries = readEntries(current.toByteArray());
		entries.put("ccpm.json", "{\"schemaVersion\":1,\"enabled\":false,\"bufferFraction\":0.2,\"levelingOrder\":\"MIN_SLACK\",\"onlyWithinAvailableSlack\":false,\"allowTaskSplits\":false}\n".getBytes(StandardCharsets.UTF_8));
		org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> loadFromBytes(zip(entries).toByteArray()));
	}

	@Test
	void mpoRoundTripLoadsItsMspdiSnapshot() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainService.Settings ccpm = new CriticalChainService().settings(original);
		ccpm.setEnabled(true);
		ccpm.setBufferFraction(0.4D);
		new CriticalChainService().restoreBaseline(original, new CriticalChainService.Baseline(1L, 2L, 0.4D,
			java.util.List.of(), java.util.Map.of(), java.util.Map.of()));
		File mpo = File.createTempFile("mpo-roundtrip", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(readEntries(java.nio.file.Files.readAllBytes(mpo.toPath())).containsKey("operations/log.jsonl"));

		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(mpo.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();

		org.junit.jupiter.api.Assertions.assertNotNull(reader.getProject());
		org.junit.jupiter.api.Assertions.assertFalse(reader.getProject().isReadOnly(),
				"a locally opened MPOF project must remain editable");
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(original), taskCount(reader.getProject()));
		CriticalChainService.Settings restored = new CriticalChainService().settings(reader.getProject());
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.4D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertEquals(2L, new CriticalChainService().findBaseline(reader.getProject()).projectBufferMillis());
		byte[] operations = readEntries(java.nio.file.Files.readAllBytes(mpo.toPath())).get("operations/log.jsonl");
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(reader.getProject(), roundTrip);
		org.junit.jupiter.api.Assertions.assertArrayEquals(operations, readEntries(roundTrip.toByteArray()).get("operations/log.jsonl"));
	}

	@Test
	void mpoRoundTripPreservesAppliedCcpmAndCanReanalyzeTheChain() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask first = (NormalTask) firstTask(original);
		NormalTask second = (NormalTask) original.createLocalTaskNode(null).getImpl();
		second.setName("Second mpo task");
		assignPositiveUniqueIds(original);
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Shared engineer");
		MpoFileImporter writer = new MpoFileImporter();
		File mpo = File.createTempFile("mpo-ccpm-applied", ".mpo");
		mpo.deleteOnExit();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		AssignmentService.getInstance().newAssignment(first, resource, 1D, 0L, null, false);
		AssignmentService.getInstance().newAssignment(second, resource, 1D, 0L, null, false);

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(original);
		settings.setEnabled(true);
		settings.setBufferFraction(0.5D);
		CriticalChainService.Analysis applied = service.apply(original, java.util.List.of(resource), settings);
		org.junit.jupiter.api.Assertions.assertTrue(applied.criticalTaskIds().contains(Long.valueOf(second.getUniqueId())));

		writer.exportFile();

		Project loaded = load(mpo);
		CriticalChainService loadedService = new CriticalChainService();
		CriticalChainService.Settings restored = loadedService.findSettings(loaded);
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.5D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertNotNull(loadedService.findBaseline(loaded));
		// The importer rebuilds task-side assignments first; resource-side
		// assignment indexes are populated lazily for some MPO snapshots.  Use
		// the authoritative assignment reference from the restored task rather
		// than requiring that optional reverse index to be eagerly populated.
		Resource loadedResource = null;
		for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext() && loadedResource == null;) {
			Object value = tasks.next();
			if (value instanceof NormalTask task && !task.getAssignments().isEmpty()) {
				Object assignment = task.getAssignments().get(0);
				if (assignment instanceof com.microproject.pm.assignment.Assignment a)
					loadedResource = a.getResource();
			}
		}
		org.junit.jupiter.api.Assertions.assertNotNull(loadedResource, "restored task assignment must reference a resource");
		CriticalChainService.Analysis reanalyzed = loadedService.preview(loaded, java.util.List.of(loadedResource), restored);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(reanalyzed.projectBuffer().plannedMillis() >= 0L);
		// CCPM Apply -> Save -> Reload -> Clear must remove only the document-scoped
		// CCPM projection and restore the ordinary schedule state.
		loadedService.clear(loaded);
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findSettings(loaded));
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findBaseline(loaded));
		org.junit.jupiter.api.Assertions.assertNull(loadedService.findAnalysis(loaded));
		org.junit.jupiter.api.Assertions.assertTrue(loaded.getTaskOutlineIterator().hasNext(),
			"clearing CCPM must retain the reloaded task document");
	}

	/**
	 * Exercises the complete user-facing path on a real legacy ProjectLibre sample:
	 * load POD, apply CCPM, save as MPO, reload, and preview the restored chain.
	 * This guards against the synthetic fixture hiding importer/exporter differences
	 * in task hierarchies, calendars, and resource assignments.
	 */
	@Test
	void realPodSampleCanBeConvertedToMpoAndReanalyzedWithCcpm() throws Exception {
		File source = findSample("June_1_sample.pod");
		Project original = loadPod(source);
		List<Resource> selected = new ArrayList<>();
		selected.addAll(original.getResourcePool().getResourceList());
		org.junit.jupiter.api.Assertions.assertFalse(selected.isEmpty(), "sample must contain resources");

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(original);
		settings.setEnabled(true);
		settings.setBufferFraction(0.25D);
		CriticalChainService.Analysis applied = service.apply(original, selected, settings);
		org.junit.jupiter.api.Assertions.assertFalse(applied.criticalTaskIds().isEmpty(), "sample CCPM chain must not be empty");

		File mpo = File.createTempFile("sample-ccpm", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();

		Project restored = load(mpo);
		CriticalChainService.Settings restoredSettings = service.findSettings(restored);
		org.junit.jupiter.api.Assertions.assertNotNull(restoredSettings);
		org.junit.jupiter.api.Assertions.assertTrue(restoredSettings.isEnabled());
		org.junit.jupiter.api.Assertions.assertNotNull(service.findBaseline(restored));
		List<Resource> restoredResources = new ArrayList<>();
		restoredResources.addAll(restored.getResourcePool().getResourceList());
		CriticalChainService.Analysis reanalyzed = service.preview(restored, restoredResources, restoredSettings);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
	}

	@Test
	void mpoPreservesUnknownExtensionsOnRoundTrip() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		MpoFileImporter writer = new MpoFileImporter();
		writer.saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		byte[] extension = "opaque extension".getBytes(StandardCharsets.UTF_8);
		entries.put("vendor/example.json", extension);
		ByteArrayOutputStream input = zip(entries);

		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(input.toByteArray()));
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(loaded, roundTrip);

		org.junit.jupiter.api.Assertions.assertArrayEquals(extension, readEntries(roundTrip.toByteArray()).get("vendor/example.json"));
	}

	@Test
	void mpoSessionsCoordinateTaskLocksThroughTheSharedSidecar() throws Exception {
		Project original = projectForRoundTrip();
		File mpo = File.createTempFile("mpo-collaboration", ".mpo");
		mpo.deleteOnExit();
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(mpo.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		Project first = load(mpo);
		Project second = load(mpo);
		CollaborationSession alice = CollaborationSession.create(first, mpo.getAbsolutePath(), "alice");
		CollaborationSession bob = CollaborationSession.create(second, mpo.getAbsolutePath(), "bob");
		org.junit.jupiter.api.Assertions.assertNotNull(alice);
		org.junit.jupiter.api.Assertions.assertNotNull(bob);
		alice.start();
		bob.start();
		try {
			org.junit.jupiter.api.Assertions.assertTrue(alice.tryAcquireTaskLock(firstTask(first)));
			org.junit.jupiter.api.Assertions.assertFalse(bob.tryAcquireTaskLock(firstTask(second)));
			alice.stop();
			org.junit.jupiter.api.Assertions.assertTrue(bob.tryAcquireTaskLock(firstTask(second)));
		} finally {
			alice.stop();
			bob.stop();
		}
	}

	private static Project projectForRoundTrip() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("mpo-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Mpo task");
		return project;
	}

	@Test
	void taskSchedulingModeSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		com.microproject.pm.task.Task task = firstTask(original);
		task.setManuallyScheduled(true);
		Project restored = load(writeTempMpo(original));
		assertTrue(firstTask(restored).isManuallyScheduled(),
				"MPO reload must preserve the task scheduling mode");
	}

	@Test
	void statusDateSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		long expected = firstTask(original).getEnd();
		original.setStatusDate(expected);
		Project restored = load(writeTempMpo(original));
		assertTrue(restored.isStatusDateSet());
		assertEquals(original.getStatusDate(), restored.getStatusDate());
	}

	@Test
	void updateProjectRequestStateSurvivesMpoRoundTrip() throws Exception {
		Project original = projectForRoundTrip();
		Task task = firstTask(original);
		boolean statusDateWasSet = original.isStatusDateSet();
		long originalStatusDate = original.getStatusDate();
		long statusDate = task.getEnd() + 86_400_000L;
		UpdateProjectRequest request = new UpdateProjectRequest(statusDate, true, false, true);
		new UpdateProjectCommand(original, request).accept(task);
		assertEquals(1D, task.getPercentComplete(), 0.00001D,
			"Update Project must complete the source task before export");

		Project restored = load(writeTempMpo(original));
		assertEquals(statusDateWasSet, original.isStatusDateSet(),
			"Update Project uses its target date without changing the project Status Date setting");
		assertEquals(statusDateWasSet, restored.isStatusDateSet(),
			"MPO reload must preserve the unchanged Status Date setting");
		assertEquals(originalStatusDate, restored.getStatusDate());
		assertEquals(1D, firstTask(restored).getPercentComplete(), 0.00001D,
			"Update Project actual progress must survive MPO reload");
	}

	private static File writeTempMpo(Project project) throws Exception {
		File output = File.createTempFile("task-mode-roundtrip-", ".mpo");
		output.deleteOnExit();
		try (java.io.OutputStream stream = java.nio.file.Files.newOutputStream(output.toPath())) {
			new MpoFileImporter().saveProject(project, stream);
		}
		return output;
	}

	/** MSPDI export preserves unique ids only when they are positive (see MPXConverter.exportId). */
	private static void assignPositiveUniqueIds(Project project) {
		long next = 1L;
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext(); next++) {
			((com.microproject.pm.task.Task) tasks.next()).setUniqueId(next);
		}
	}

	private static int taskCount(Project project) {
		int count = 0;
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			tasks.next();
			count++;
		}
		return count;
	}

	private static Project load(File mpo) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(mpo.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		return reader.getProject();
	}

	@Test
	void separateJvmDependencyLagConflictReturnsDurableRecoveryCopy() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(initial);
		NormalTask successor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		successor.setName("Dependency successor");
		assignPositiveUniqueIds(initial);
		long predecessorId = predecessor.getUniqueId();
		long successorId = successor.getUniqueId();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		File shared = File.createTempFile("mpo-process-dependency-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-dependency-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			String field = "dependencyLag:" + successorId;
			first = startMpoSaveWorker(shared, predecessorId, field, "3600000", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, predecessorId, field, "7200000", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(firstLog), readWorkerLog(secondLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "exactly one process should preserve its dependency branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			long sharedLag = dependencyLag(load(shared), predecessorId, successorId);
			long recoveredLag = dependencyLag(load(recoveryCopy.toFile()), predecessorId, successorId);
			assertTrue(sharedLag > 0L, "the winning dependency edit must survive reload");
			assertTrue(recoveredLag > 0L, "the recovery dependency edit must survive reload");
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedLag, recoveredLag);
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertFalse(new OperationLog().merge(recoveryLog.operations()).conflicts().isEmpty());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmSaveWaitsForProcessHoldingTransactionLock() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-lock-wait", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();
		byte[] originalArchive = Files.readAllBytes(shared.toPath());

		Path lockPath = Path.of(shared.getAbsolutePath() + ".lock");
		Path tempDirectory = Files.createTempDirectory("mpo-process-lock-wait-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path ready = tempDirectory.resolve("worker-ready.flag");
		Path log = tempDirectory.resolve("worker.log");
		Process worker = null;
		try (java.nio.channels.FileChannel lockChannel = java.nio.channels.FileChannel.open(lockPath,
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
				java.nio.channels.FileLock heldLock = lockChannel.lock()) {
			worker = startMpoSaveWorker(shared, taskId, "name", "Saved after lock release", ready, release, log);
			awaitWorkerReady(worker, ready, log);
			Files.createFile(release);
			long attemptDeadline = System.nanoTime()
					+ java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
			while (!readWorkerLog(log).contains("MPO_SAVE_ATTEMPT=")) {
				if (!worker.isAlive())
					throw new AssertionError("MPO worker exited before attempting its save: " + readWorkerLog(log));
				if (System.nanoTime() >= attemptDeadline)
					throw new AssertionError("MPO worker did not attempt save in time: " + readWorkerLog(log));
				Thread.sleep(20L);
			}
			assertTrue(worker.isAlive(), "save must wait while another process owns the transaction lock");
			org.junit.jupiter.api.Assertions.assertArrayEquals(originalArchive, Files.readAllBytes(shared.toPath()),
					"the shared archive must remain unchanged while the lock is held");
			heldLock.release();
			assertWorkerSucceeded(worker, log);
			assertEquals("Saved after lock release", firstTask(load(shared)).getName());
		} finally {
			stopWorker(worker);
			Files.deleteIfExists(ready);
			Files.deleteIfExists(release);
			Files.deleteIfExists(log);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(lockPath);
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmTaskDeleteConflictsWithConcurrentUpdate() throws Exception {
		Project initial = projectForRoundTrip();
		assignPositiveUniqueIds(initial);
		long taskId = firstTask(initial).getUniqueId();
		File shared = File.createTempFile("mpo-process-delete-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-delete-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path deleteReady = tempDirectory.resolve("delete-ready.flag");
		Path updateReady = tempDirectory.resolve("update-ready.flag");
		Path deleteLog = tempDirectory.resolve("delete-worker.log");
		Path updateLog = tempDirectory.resolve("update-worker.log");
		Process deleteWorker = null;
		Process updateWorker = null;
		Path recoveryCopy = null;
		try {
			deleteWorker = startMpoSaveWorker(shared, taskId, "delete", "", deleteReady, release, deleteLog);
			updateWorker = startMpoSaveWorker(shared, taskId, "name", "Updated concurrently", updateReady, release,
					updateLog);
			awaitWorkerReady(deleteWorker, deleteReady, deleteLog);
			awaitWorkerReady(updateWorker, updateReady, updateLog);
			Files.createFile(release);
			assertWorkerSucceeded(deleteWorker, deleteLog);
			assertWorkerSucceeded(updateWorker, updateLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(deleteLog), readWorkerLog(updateLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "one process must preserve its delete/update branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			Project sharedProject = load(shared);
			Project recoveredProject = load(recoveryCopy.toFile());
			assertTrue((sharedProject.findByUniqueId(taskId) == null)
					!= (recoveredProject.findByUniqueId(taskId) == null),
					"shared and recovery archives must preserve opposite delete/update outcomes");
			Project updateBranch = sharedProject.findByUniqueId(taskId) == null ? recoveredProject : sharedProject;
			assertEquals("Updated concurrently", updateBranch.findByUniqueId(taskId).getName(),
					"the surviving task branch must retain the concurrent update");
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertTrue(recoveryLog.operations().size() >= 2,
					"the recovery journal must retain both competing branches");
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("task.delete")));
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("task.update")));
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(deleteWorker);
			stopWorker(updateWorker);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(deleteReady);
			Files.deleteIfExists(updateReady);
			Files.deleteIfExists(deleteLog);
			Files.deleteIfExists(updateLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmAssignmentAddsWithDifferentUnitsConflict() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(initial);
		Resource resource = initial.getResourcePool().newResourceInstance();
		resource.setName("Concurrent assignment resource");
		NormalTask resourceAnchor = (NormalTask) initial.createLocalTaskNode(null).getImpl();
		resourceAnchor.setName("Resource identity anchor");
		AssignmentService.getInstance().newAssignment(resourceAnchor, resource, 1D, 0L, null, false);
		String resourceName = resource.getName();
		assignPositiveUniqueIds(initial);
		long taskId = task.getUniqueId();
		File shared = File.createTempFile("mpo-process-assignment-conflict", ".mpo");
		MpoFileImporter seed = new MpoFileImporter();
		seed.setFileName(shared.getAbsolutePath());
		seed.setProject(initial);
		seed.exportFile();

		Path tempDirectory = Files.createTempDirectory("mpo-process-assignment-conflict-" + System.nanoTime());
		Path release = tempDirectory.resolve("release.flag");
		Path firstReady = tempDirectory.resolve("first-ready.flag");
		Path secondReady = tempDirectory.resolve("second-ready.flag");
		Path firstLog = tempDirectory.resolve("first-worker.log");
		Path secondLog = tempDirectory.resolve("second-worker.log");
		Process first = null;
		Process second = null;
		Path recoveryCopy = null;
		try {
			String field = "assignmentUnits:" + resourceName;
			first = startMpoSaveWorker(shared, taskId, field, "0.5", firstReady, release, firstLog);
			second = startMpoSaveWorker(shared, taskId, field, "1.0", secondReady, release, secondLog);
			awaitWorkerReady(first, firstReady, firstLog);
			awaitWorkerReady(second, secondReady, secondLog);
			Files.createFile(release);
			assertWorkerSucceeded(first, firstLog);
			assertWorkerSucceeded(second, secondLog);

			List<String> recoveryMarkers = java.util.stream.Stream.of(readWorkerLog(firstLog), readWorkerLog(secondLog))
					.flatMap(value -> value.lines()).filter(value -> value.startsWith("MPO_CONFLICT_RECOVERY=")).toList();
			assertEquals(1, recoveryMarkers.size(), "one process must preserve its assignment branch");
			recoveryCopy = Path.of(recoveryMarkers.getFirst().substring("MPO_CONFLICT_RECOVERY=".length()));
			assertTrue(Files.isRegularFile(recoveryCopy));
			double sharedUnits = assignmentUnits(load(shared), taskId, resourceName);
			double recoveredUnits = assignmentUnits(load(recoveryCopy.toFile()), taskId, resourceName);
			org.junit.jupiter.api.Assertions.assertNotEquals(sharedUnits, recoveredUnits,
					"the shared and recovery archives must preserve different assignment units");
			assertTrue(java.util.Set.of(0.5D, 1D).contains(sharedUnits));
			assertTrue(java.util.Set.of(0.5D, 1D).contains(recoveredUnits));
			OperationLog.DocumentLog recoveryLog = new OperationLog().readJsonl(
					readEntries(Files.readAllBytes(recoveryCopy)).get(MpoFileImporter.OPERATIONS_ENTRY));
			assertTrue(recoveryLog.operations().stream().anyMatch(operation -> operation.kind().equals("assignment.add")));
			assertEquals(1, new OperationLog().merge(recoveryLog.operations()).conflicts().size());
		} finally {
			stopWorker(first);
			stopWorker(second);
			if (recoveryCopy != null) Files.deleteIfExists(recoveryCopy);
			Files.deleteIfExists(release);
			Files.deleteIfExists(firstReady);
			Files.deleteIfExists(secondReady);
			Files.deleteIfExists(firstLog);
			Files.deleteIfExists(secondLog);
			Files.deleteIfExists(tempDirectory);
			Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
			Files.deleteIfExists(shared.toPath());
		}
	}

	@Test
	void separateJvmCrashAroundAtomicReplaceLeavesReloadableArchive() throws Exception {
		for (String crashPoint : List.of("before-replace", "after-replace")) {
			Project initial = projectForRoundTrip();
			assignPositiveUniqueIds(initial);
			long taskId = firstTask(initial).getUniqueId();
			String originalName = firstTask(initial).getName();
			File shared = File.createTempFile("mpo-process-crash-" + crashPoint, ".mpo");
			MpoFileImporter seed = new MpoFileImporter();
			seed.setFileName(shared.getAbsolutePath());
			seed.setProject(initial);
			seed.exportFile();
			byte[] originalArchive = Files.readAllBytes(shared.toPath());

			Path tempDirectory = Files.createTempDirectory("mpo-process-crash-" + crashPoint + "-" + System.nanoTime());
			Path release = tempDirectory.resolve("release.flag");
			Path ready = tempDirectory.resolve("worker-ready.flag");
			Path log = tempDirectory.resolve("worker.log");
			Process worker = null;
			try {
				worker = startMpoSaveWorker(shared, taskId, "name", "Crash edit " + crashPoint, ready, release, log,
						crashPoint);
				awaitWorkerReady(worker, ready, log);
				Files.createFile(release);
				assertTrue(worker.waitFor(30, java.util.concurrent.TimeUnit.SECONDS),
						"crash-injected worker did not terminate: " + readWorkerLog(log));
				assertEquals(crashPoint.equals("before-replace") ? 71 : 72, worker.exitValue());
				Project reopened = load(shared);
				assertEquals(crashPoint.equals("before-replace") ? originalName : "Crash edit " + crashPoint,
						firstTask(reopened).getName(), "destination must be a complete old or new archive");
				if (crashPoint.equals("before-replace")) {
					org.junit.jupiter.api.Assertions.assertArrayEquals(originalArchive, Files.readAllBytes(shared.toPath()),
							"crash before replacement must preserve the original archive byte-for-byte");
				} else {
					OperationLog.DocumentLog logAfterCrash = new OperationLog().readJsonl(
							readEntries(Files.readAllBytes(shared.toPath())).get(MpoFileImporter.OPERATIONS_ENTRY));
					String updateId = logAfterCrash.operations().stream()
							.filter(operation -> operation.kind().equals("task.update"))
							.map(OperationLog.Operation::id).findFirst()
							.orElseThrow(() -> new AssertionError("atomic replacement archive is missing its task.update"));
					assertTrue(logAfterCrash.appliedOperationIds().contains(updateId),
							"the post-crash snapshot must mark its task.update as applied");
				}
			} finally {
				stopWorker(worker);
				Files.deleteIfExists(ready);
				Files.deleteIfExists(release);
				Files.deleteIfExists(log);
				Files.deleteIfExists(tempDirectory);
				Path targetPath = shared.toPath().toAbsolutePath();
				try (java.util.stream.Stream<Path> siblings = Files.list(targetPath.getParent())) {
					for (Path sibling : siblings.filter(path -> path.getFileName().toString().startsWith(shared.getName() + ".")
							&& path.getFileName().toString().endsWith(".tmp")).toList())
						Files.deleteIfExists(sibling);
				}
				Files.deleteIfExists(Path.of(shared.getAbsolutePath() + ".lock"));
				Files.deleteIfExists(shared.toPath());
			}
		}
	}

	private static byte[] saveMpo(Project project) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, output);
		return output.toByteArray();
	}

	private static void changeDependencyLag(Project project, long predecessorId, long successorId, long lag)
			throws Exception {
		Task predecessor = project.findByUniqueId(predecessorId);
		Task successor = project.findByUniqueId(successorId);
		for (java.util.Iterator<?> links = predecessor.getSuccessorList().iterator(); links.hasNext();) {
			Dependency dependency = (Dependency) links.next();
			if (dependency.getSuccessor() == successor) {
				DependencyService.getInstance().setFields(dependency, lag, dependency.getDependencyKind(), null);
				return;
			}
		}
		throw new AssertionError("Expected dependency between tasks " + predecessorId + " and " + successorId);
	}

	private static long dependencyLag(Project project, long predecessorId, long successorId) {
		Task predecessor = project.findByUniqueId(predecessorId);
		Task successor = project.findByUniqueId(successorId);
		for (java.util.Iterator<?> links = predecessor.getSuccessorList().iterator(); links.hasNext();) {
			Dependency dependency = (Dependency) links.next();
			if (dependency.getSuccessor() == successor) return dependency.getLag();
		}
		throw new AssertionError("Expected dependency between tasks " + predecessorId + " and " + successorId);
	}

	private static double assignmentUnits(Project project, long taskId, String resourceName) {
		NormalTask task = (NormalTask) project.findByUniqueId(taskId);
		Resource resource = project.getResourcePool().getResourceList().stream()
				.filter(candidate -> candidate.getName().equals(resourceName)).findFirst()
				.orElseThrow(() -> new AssertionError("Expected resource " + resourceName));
		for (java.util.Iterator<?> assignments = task.getAssignments().iterator(); assignments.hasNext();) {
			Assignment assignment = (Assignment) assignments.next();
			if (assignment.getResource() == resource) return assignment.getUnits();
		}
		throw new AssertionError("Expected assignment for task " + taskId + " and resource " + resourceName);
	}

	@Test
	void checkedInEnglishAndJapaneseCcpmSamplesLoadForVisualization() throws Exception {
		CriticalChainService service = new CriticalChainService();
		for (String name : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project loaded = load(findSample(name));
			CriticalChainService.Settings settings = service.findSettings(loaded);
			org.junit.jupiter.api.Assertions.assertNotNull(settings, name);
			org.junit.jupiter.api.Assertions.assertTrue(settings.isEnabled(), name);
			// The comparison samples are a clean pre-CCPM plan. Applying CCPM is
			// part of the walkthrough, so no stale baseline may be embedded.
			org.junit.jupiter.api.Assertions.assertNull(service.findBaseline(loaded), name);
			List<Resource> resources = new ArrayList<>(loaded.getResourcePool().getResourceList());
			CriticalChainService.Analysis analysis = service.preview(loaded, resources, settings);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.criticalTaskIds().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.graphEdges().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertEquals(0D, loaded.getPercentComplete(), 0.00001D, name);
			org.junit.jupiter.api.Assertions.assertTrue(analysis.criticalTaskIds().size() < taskCount(loaded), name);
		}
	}

	@Test
	void checkedInDataDescriptorMpoLoadsThroughTheFileImportPath() throws Exception {
		Project loaded = load(findSample("CCPM sample English.mpo"));
		org.junit.jupiter.api.Assertions.assertTrue(taskCount(loaded) > 0);
		org.junit.jupiter.api.Assertions.assertTrue(new CriticalChainService().findSettings(loaded).isEnabled());
	}

	@Test
	void checkedInTwentyTaskJapaneseCcpmSampleLoadsForVisualization() throws Exception {
		Project loaded = load(findSample("CCPM 標準システム導入 20タスク.mpo"));
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.findSettings(loaded);
		org.junit.jupiter.api.Assertions.assertNotNull(settings);
		org.junit.jupiter.api.Assertions.assertTrue(settings.isEnabled());
		org.junit.jupiter.api.Assertions.assertNull(service.findBaseline(loaded));
		org.junit.jupiter.api.Assertions.assertEquals(20, taskCount(loaded));
		org.junit.jupiter.api.Assertions.assertEquals(0D, loaded.getPercentComplete(), 0.00001D,
			"the walkthrough must begin at the 0% / 0% fever-chart checkpoint");
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "要件定義").getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "基幹機能の実装").getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, findByName(loaded, "結合テスト").getPercentComplete(), 0.00001D);
		for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
			org.junit.jupiter.api.Assertions.assertFalse(task.isManuallyScheduled(), task.getName());
			org.junit.jupiter.api.Assertions.assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(task), task.getName());
		}
		CriticalChainService.Analysis analysis = service.preview(loaded,
			new ArrayList<>(loaded.getResourcePool().getResourceList()), settings);
		org.junit.jupiter.api.Assertions.assertFalse(analysis.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertFalse(analysis.graphEdges().isEmpty());
	}

	@Test
	void checkedInCcpmPathSamplesAreAutomaticallyScheduledAndHonorPredecessors() throws Exception {
		for (String name : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project loaded = load(findSample(name));
			for (java.util.Iterator<?> tasks = loaded.getTaskOutlineIterator(); tasks.hasNext();) {
				com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
				org.junit.jupiter.api.Assertions.assertFalse(task.isManuallyScheduled(), name + ": " + task.getName());
				org.junit.jupiter.api.Assertions.assertFalse(ScheduleDiagnosticsService.hasDependencyConflict(task),
					name + ": " + task.getName());
			}
		}
	}

	@Test
	void japaneseCcpmSampleStartsWithoutImportedCompletion() throws Exception {
		Project loaded = load(findSample("CCPM path comparison 日本語.mpo"));
		com.microproject.pm.task.Task completedTask = findByName(loaded, "操作手順書");
		org.junit.jupiter.api.Assertions.assertNotNull(completedTask);
		org.junit.jupiter.api.Assertions.assertEquals(0D, completedTask.getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(0D, ((NormalTask) completedTask).getPercentWorkComplete(), 0.00001D);
	}

	@Test
	void historySampleRestoresFourCcpmObservations() throws Exception {
		Project loaded = load(findSample("CCPM 標準システム導入 20タスク（履歴付き）.mpo"));
		CriticalChainBufferHistory history = loaded.findTransientDocumentState(CriticalChainBufferHistory.class);
		org.junit.jupiter.api.Assertions.assertNotNull(history);
		org.junit.jupiter.api.Assertions.assertEquals(4, history.points().size());
		org.junit.jupiter.api.Assertions.assertEquals(50D, history.points().get(2).progressPercent(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(55D, history.points().get(2).consumptionPercent(), 0.00001D);
	}

	@Test
	void retractedCcpmObservationSurvivesMpoSaveAndReloadAsAuditOnly() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainBufferHistory history = original.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		CriticalChainBufferHistory.Point point = new CriticalChainBufferHistory.Point(
			java.time.Instant.parse("2026-09-11T00:00:00Z"), "planner", "Planner", 40D, 70D, "RED", "baseline-1");
		history.add(point);
		assertTrue(new com.microproject.pm.ccpm.CriticalChainBufferHistoryService()
			.retract(original, point.observationId(), "Accidental status refresh", "planner", "Planner").changed());

		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, archive));
		Project loaded = loadFromBytes(archive.toByteArray());
		CriticalChainBufferHistory restored = loaded.findTransientDocumentState(CriticalChainBufferHistory.class);

		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		assertTrue(restored.points().isEmpty(), "retracted point must not return to the visible chart after reload");
		assertEquals(1, restored.retractions().size());
		assertEquals(point.observationId(), restored.retractions().getFirst().observationId());
		assertEquals("Accidental status refresh", restored.retractions().getFirst().reason());
	}

	private static Project loadFromBytes(byte[] mpo) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		return reader.loadProject(new ByteArrayInputStream(mpo));
	}

	private static DefaultSubProj findSubproject(Project project) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) return reference;
		}
		throw new AssertionError("Expected a subproject reference");
	}

	private static DefaultSubProj findSubprojectForId(Project project, long projectId) {
		java.util.List<Long> availableIds = new java.util.ArrayList<>();
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Object task = tasks.next();
			if (task instanceof DefaultSubProj reference) {
				availableIds.add(reference.getSubprojectUniqueId());
				if (reference.getSubprojectUniqueId() == projectId)
					return reference;
			}
		}
		throw new AssertionError("Expected extracted subproject " + projectId + "; found " + availableIds);
	}

	private static void addEmbeddedReference(Project master, Project child, File childFile) {
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setSubprojectFile(childFile.getAbsolutePath());
		master.connectTask(reference);
		master.addToDefaultOutline(null, NodeFactory.getInstance().createNode(reference));
	}

	private static Project loadPod(File pod) throws Exception {
		LocalFileImporter reader = new LocalFileImporter();
		reader.setFileName(pod.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		org.junit.jupiter.api.Assertions.assertNotNull(reader.getProject());
		return reader.getProject();
	}

	private static File findSample(String name) {
		for (String prefix : new String[] { "samples/", "../samples/", "../../samples/" }) {
			File sample = new File(prefix + name);
			if (sample.isFile()) return sample;
		}
		throw new AssertionError("Missing POD sample: " + name);
	}

	private static com.microproject.pm.task.Task firstTask(Project project) {
		java.util.Iterator<?> tasks = project.getTaskOutlineIterator();
		if (!tasks.hasNext()) throw new AssertionError("Expected a task");
		return (com.microproject.pm.task.Task) tasks.next();
	}

	private static com.microproject.pm.task.Task findByName(Project project, String name) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) tasks.next();
			if (java.util.Objects.equals(name, task.getName())) return task;
		}
		throw new AssertionError("Expected task: " + name);
	}

	private static void write(ZipOutputStream zip, String name, byte[] content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content);
		zip.closeEntry();
	}

	private static Map<String, byte[]> readEntries(byte[] archive) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				zip.transferTo(content);
				entries.put(entry.getName(), content.toByteArray());
			}
		}
		return entries;
	}

	private static ByteArrayOutputStream zip(Map<String, byte[]> entries) throws IOException {
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) write(zip, entry.getKey(), entry.getValue());
		}
		return archive;
	}

	private static String sha256(File file) throws IOException {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
					.digest(java.nio.file.Files.readAllBytes(file.toPath()));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
	}

	private static String sha256(byte[] bytes) {
		try {
			return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new AssertionError(impossible);
		}
	}

	private static void updateManifestChecksum(Map<String, byte[]> entries, String path, byte[] content) {
		String manifest = new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8);
		String pattern = "(path=\\\"" + java.util.regex.Pattern.quote(path) + "\\\" sha256=\\\")[^\\\"]*(\\\")";
		manifest = manifest.replaceFirst(pattern, "$1" + sha256(content) + "$2");
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifest.getBytes(StandardCharsets.UTF_8));
	}

	private static String manifestDocumentId(Map<String, byte[]> entries) {
		return new String(entries.get(MpoFileImporter.MANIFEST_ENTRY), StandardCharsets.UTF_8)
				.replaceFirst("(?s).*documentId=\\\"([0-9a-f-]{36})\\\".*", "$1");
	}
}
