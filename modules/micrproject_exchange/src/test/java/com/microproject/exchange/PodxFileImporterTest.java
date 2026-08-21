/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.undo.DataFactoryUndoController;

import org.junit.jupiter.api.Test;

class PodxFileImporterTest {
	@Test
	void manifestValidatesTheExactProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> PodxFileImporter.validateManifest(PodxFileImporter.manifestFor(projectXml), projectXml));
	}

	@Test
	void manifestRejectsChangedProjectPayload() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		byte[] changedXml = "<Project><Name>changed</Name></Project>".getBytes(StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> PodxFileImporter.validateManifest(PodxFileImporter.manifestFor(projectXml), changedXml));
	}

	@Test
	void manifestRejectsUnsupportedVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String unsupported = PodxFileImporter.manifestFor(projectXml).replace("\"0.1\"", "\"2.0\"");
		assertThrows(IOException.class, () -> PodxFileImporter.validateManifest(unsupported, projectXml));
	}

	@Test
	void manifestRejectsDuplicateRequiredFields() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String duplicate = PodxFileImporter.manifestFor(projectXml).replace("\"format\":\"podx\"", "\"format\":\"podx\",\"format\":\"podx\"");
		assertThrows(IOException.class, () -> PodxFileImporter.validateManifest(duplicate, projectXml));
	}

	@Test
	void podxRejectsMalformedOperationLogsBeforeLoadingTheSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new PodxFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		entries.put("changes/operations.json", "{\"schemaVersion\":1,\"documentId\":\"not-a-uuid\",\"operations\":[],\"conflicts\":[]}".getBytes(StandardCharsets.UTF_8));
		PodxFileImporter reader = new PodxFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		assertThrows(IOException.class, () -> reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray())));
	}

	@Test
	void podxAppliesValidatedTaskUpdateOperationsToItsSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new PodxFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation update = new OperationLog.Operation("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000012", 1, java.util.Set.of(), "task.update", "00000000-0000-0000-0000-000000000013", Map.of("legacyUniqueId", Long.valueOf(task.getUniqueId()), "name", "Merged task"));
		entries.put("changes/operations.json", new OperationLog().write("00000000-0000-0000-0000-000000000014", java.util.List.of(update)));
		PodxFileImporter reader = new PodxFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals("Merged task", findByName(loaded, "Merged task").getName());
	}

	@Test
	void podxAppliesTaskCreateOperationsIdempotently() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new PodxFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation create = new OperationLog.Operation("00000000-0000-0000-0000-000000000021", "00000000-0000-0000-0000-000000000022", 1, java.util.Set.of(), "task.create", "00000000-0000-0000-0000-000000000023", Map.of("legacyUniqueId", Long.valueOf(9001L), "name", "Created task"));
		entries.put("changes/operations.json", new OperationLog().write("00000000-0000-0000-0000-000000000024", java.util.List.of(create, create)));
		PodxFileImporter reader = new PodxFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals("Created task", loaded.findByUniqueId(9001L).getName());
		org.junit.jupiter.api.Assertions.assertEquals(2, taskCount(loaded));
	}

	@Test
	void podxAppliesTaskDeleteOperationsIdempotently() throws Exception {
		Project project = projectForRoundTrip();
		long taskId = firstTask(project).getUniqueId();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new PodxFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation delete = new OperationLog.Operation("00000000-0000-0000-0000-000000000031", "00000000-0000-0000-0000-000000000032", 1, java.util.Set.of(), "task.delete", "00000000-0000-0000-0000-000000000033", Map.of("legacyUniqueId", Long.valueOf(taskId)));
		entries.put("changes/operations.json", new OperationLog().write("00000000-0000-0000-0000-000000000034", java.util.List.of(delete, delete)));
		PodxFileImporter reader = new PodxFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNull(loaded.findByUniqueId(taskId));
	}

	@Test
	void podxSaveAppendsTaskUpdatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		PodxFileImporter writer = new PodxFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) firstTask(project)).setName("Edited after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("changes/operations.json");
		org.junit.jupiter.api.Assertions.assertEquals(1, new OperationLog().read(operations).size());
	}

	@Test
	void podxSaveAppendsTaskCreatesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		PodxFileImporter writer = new PodxFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		((NormalTask) project.createLocalTaskNode(null).getImpl()).setName("Added after save");
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		byte[] operations = readEntries(second.toByteArray()).get("changes/operations.json");
		org.junit.jupiter.api.Assertions.assertEquals("task.create", new OperationLog().read(operations).get(0).kind());
	}

	@Test
	void podxAppliesTaskMoveOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		ByteArrayOutputStream generated = new ByteArrayOutputStream(); new PodxFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation move = new OperationLog.Operation("00000000-0000-0000-0000-000000000041", "00000000-0000-0000-0000-000000000042", 1, java.util.Set.of(), "task.move", "00000000-0000-0000-0000-000000000043", Map.of("legacyUniqueId", Long.valueOf(child.getUniqueId()), "parentLegacyUniqueId", Long.valueOf(parent.getUniqueId())));
		entries.put("changes/operations.json", new OperationLog().write("00000000-0000-0000-0000-000000000044", java.util.List.of(move)));
		PodxFileImporter reader = new PodxFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(parent.getUniqueId(), loaded.findByUniqueId(child.getUniqueId()).getWbsParentTask().getUniqueId());
	}

	@Test
	void podxSaveAppendsTaskMovesAfterTheInitialSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		PodxFileImporter writer = new PodxFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		project.setLocalParent(child, parent);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().read(readEntries(second.toByteArray()).get("changes/operations.json"));
		org.junit.jupiter.api.Assertions.assertEquals("task.move", operations.get(0).kind());
		org.junit.jupiter.api.Assertions.assertEquals(parent.getUniqueId(), ((Number) operations.get(0).payload().get("parentLegacyUniqueId")).longValue());
	}

	@Test
	void podxSavesAndReplaysDependencyOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask predecessor = (NormalTask) firstTask(project);
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl(); successor.setName("Successor");
		PodxFileImporter writer = new PodxFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, null);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().read(readEntries(second.toByteArray()).get("changes/operations.json"));
		org.junit.jupiter.api.Assertions.assertEquals("dependency.add", operations.get(0).kind());
		PodxFileImporter reader = new PodxFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		org.junit.jupiter.api.Assertions.assertTrue(findByName(loaded, predecessor.getName()).getSuccessorList().iterator().hasNext());
	}

	@Test
	void podxSavesAndReplaysAssignmentOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		Resource resource = project.getResourcePool().newResourceInstance(); resource.setName("Engineer");
		PodxFileImporter writer = new PodxFileImporter();
		ByteArrayOutputStream first = new ByteArrayOutputStream(); writer.saveProject(project, first);
		AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, null, false);
		ByteArrayOutputStream second = new ByteArrayOutputStream(); writer.saveProject(project, second);
		java.util.List<OperationLog.Operation> operations = new OperationLog().read(readEntries(second.toByteArray()).get("changes/operations.json"));
		org.junit.jupiter.api.Assertions.assertEquals("assignment.add", operations.get(0).kind());
		PodxFileImporter reader = new PodxFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(second.toByteArray()));
		org.junit.jupiter.api.Assertions.assertTrue(((NormalTask) loaded.findByUniqueId(task.getUniqueId())).getAssignments().iterator().hasNext());
	}

	@Test
	void podxSequentialSharedFolderSavesMergeIndependentTaskEdits() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		File shared = File.createTempFile("podx-shared", ".podx"); shared.deleteOnExit();
		PodxFileImporter initialWriter = new PodxFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(new File(shared.getAbsolutePath() + ".lock").isFile(), "shared saves use a stable transaction lock");
		Project firstEditor = load(shared); Project secondEditor = load(shared);
		firstTask(firstEditor).setName("First editor");
		secondEditor.findByUniqueId(second.getUniqueId()).setName("Second editor");
		PodxFileImporter firstWriter = new PodxFileImporter(); firstWriter.setFileName(shared.getAbsolutePath()); firstWriter.setProject(firstEditor); firstWriter.exportFile();
		PodxFileImporter secondWriter = new PodxFileImporter(); secondWriter.setFileName(shared.getAbsolutePath()); secondWriter.setProject(secondEditor); secondWriter.exportFile();
		Project merged = load(shared);
		org.junit.jupiter.api.Assertions.assertEquals("First editor", firstTask(merged).getName());
		org.junit.jupiter.api.Assertions.assertEquals("Second editor", merged.findByUniqueId(second.getUniqueId()).getName());
	}

	@Test
	void podxConcurrentSharedFolderSavesSerializeAndMergeBothEditors() throws Exception {
		Project initial = projectForRoundTrip();
		NormalTask second = (NormalTask) initial.createLocalTaskNode(null).getImpl(); second.setName("Second");
		File shared = File.createTempFile("podx-concurrent", ".podx"); shared.deleteOnExit();
		PodxFileImporter initialWriter = new PodxFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
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

	private static void saveAfter(java.util.concurrent.CountDownLatch start, File shared, Project editor) {
		try {
			start.await();
			PodxFileImporter writer = new PodxFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
	}

	@Test
	void podxSharedFolderSavePreservesExtensionAddedAfterEditorOpened() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("podx-extension-merge", ".podx"); shared.deleteOnExit();
		PodxFileImporter initialWriter = new PodxFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		entries.put("vendor/remote.bin", new byte[] { 7, 8, 9 });
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("Edited locally");
		PodxFileImporter writer = new PodxFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor); writer.exportFile();
		org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 7, 8, 9 }, readEntries(java.nio.file.Files.readAllBytes(shared.toPath())).get("vendor/remote.bin"));
	}

	@Test
	void podxSharedFolderRejectsManifestDocumentMismatchBeforeMerge() throws Exception {
		Project initial = projectForRoundTrip();
		File shared = File.createTempFile("podx-manifest-mismatch", ".podx"); shared.deleteOnExit();
		PodxFileImporter initialWriter = new PodxFileImporter(); initialWriter.setFileName(shared.getAbsolutePath()); initialWriter.setProject(initial); initialWriter.exportFile();
		Project editor = load(shared);
		Map<String, byte[]> entries = readEntries(java.nio.file.Files.readAllBytes(shared.toPath()));
		String manifest = new String(entries.get("manifest.json"), StandardCharsets.UTF_8)
			.replaceFirst("\\\"documentId\\\":\\\"[^\\\"]+\\\"", "\\\"documentId\\\":\\\"00000000-0000-0000-0000-000000000099\\\"");
		entries.put("manifest.json", manifest.getBytes(StandardCharsets.UTF_8));
		java.nio.file.Files.write(shared.toPath(), zip(entries).toByteArray());
		firstTask(editor).setName("edited");
		PodxFileImporter writer = new PodxFileImporter(); writer.setFileName(shared.getAbsolutePath()); writer.setProject(editor);
		assertThrows(IOException.class, writer::exportFile);
	}

	@Test
	void podxRoundTripLoadsItsMspdiSnapshot() throws Exception {
		Project original = projectForRoundTrip();
		CriticalChainService.Settings ccpm = new CriticalChainService().settings(original);
		ccpm.setEnabled(true);
		ccpm.setBufferFraction(0.4D);
		new CriticalChainService().restoreBaseline(original, new CriticalChainService.Baseline(1L, 2L, 0.4D,
			java.util.List.of(), java.util.Map.of(), java.util.Map.of()));
		File podx = File.createTempFile("podx-roundtrip", ".podx");
		podx.deleteOnExit();
		PodxFileImporter writer = new PodxFileImporter();
		writer.setFileName(podx.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		org.junit.jupiter.api.Assertions.assertTrue(readEntries(java.nio.file.Files.readAllBytes(podx.toPath())).containsKey("changes/operations.json"));

		PodxFileImporter reader = new PodxFileImporter();
		reader.setFileName(podx.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();

		org.junit.jupiter.api.Assertions.assertNotNull(reader.getProject());
		org.junit.jupiter.api.Assertions.assertEquals(taskCount(original), taskCount(reader.getProject()));
		CriticalChainService.Settings restored = new CriticalChainService().settings(reader.getProject());
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.4D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertEquals(2L, new CriticalChainService().findBaseline(reader.getProject()).projectBufferMillis());
		byte[] operations = readEntries(java.nio.file.Files.readAllBytes(podx.toPath())).get("changes/operations.json");
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(reader.getProject(), roundTrip);
		org.junit.jupiter.api.Assertions.assertArrayEquals(operations, readEntries(roundTrip.toByteArray()).get("changes/operations.json"));
	}

	@Test
	void podxRoundTripPreservesAppliedCcpmAndCanReanalyzeTheChain() throws Exception {
		Project original = projectForRoundTrip();
		NormalTask first = (NormalTask) firstTask(original);
		NormalTask second = (NormalTask) original.createLocalTaskNode(null).getImpl();
		second.setName("Second podx task");
		Resource resource = original.getResourcePool().newResourceInstance();
		resource.setName("Shared engineer");
		PodxFileImporter writer = new PodxFileImporter();
		File podx = File.createTempFile("podx-ccpm-applied", ".podx");
		podx.deleteOnExit();
		writer.setFileName(podx.getAbsolutePath());
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

		Project loaded = load(podx);
		CriticalChainService loadedService = new CriticalChainService();
		CriticalChainService.Settings restored = loadedService.findSettings(loaded);
		org.junit.jupiter.api.Assertions.assertNotNull(restored);
		org.junit.jupiter.api.Assertions.assertTrue(restored.isEnabled());
		org.junit.jupiter.api.Assertions.assertEquals(0.5D, restored.getBufferFraction());
		org.junit.jupiter.api.Assertions.assertNotNull(loadedService.findBaseline(loaded));
		Resource loadedResource = loaded.getResourcePool().getResourceList().stream()
			.filter(value -> !value.getAssignments().isEmpty()).findFirst().orElseThrow();
		CriticalChainService.Analysis reanalyzed = loadedService.preview(loaded, java.util.List.of(loadedResource), restored);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(reanalyzed.projectBuffer().plannedMillis() >= 0L);
	}

	/**
	 * Exercises the complete user-facing path on a real legacy ProjectLibre sample:
	 * load POD, apply CCPM, save as PODX, reload, and preview the restored chain.
	 * This guards against the synthetic fixture hiding importer/exporter differences
	 * in task hierarchies, calendars, and resource assignments.
	 */
	@Test
	void realPodSampleCanBeConvertedToPodxAndReanalyzedWithCcpm() throws Exception {
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

		File podx = File.createTempFile("sample-ccpm", ".podx");
		podx.deleteOnExit();
		PodxFileImporter writer = new PodxFileImporter();
		writer.setFileName(podx.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();

		Project restored = load(podx);
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
	void podxPreservesUnknownExtensionsOnRoundTrip() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		PodxFileImporter writer = new PodxFileImporter();
		writer.saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		byte[] extension = "opaque extension".getBytes(StandardCharsets.UTF_8);
		entries.put("vendor/example.json", extension);
		ByteArrayOutputStream input = zip(entries);

		PodxFileImporter reader = new PodxFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(input.toByteArray()));
		ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
		writer.saveProject(loaded, roundTrip);

		org.junit.jupiter.api.Assertions.assertArrayEquals(extension, readEntries(roundTrip.toByteArray()).get("vendor/example.json"));
	}

	@Test
	void podxSessionsCoordinateTaskLocksThroughTheSharedSidecar() throws Exception {
		Project original = projectForRoundTrip();
		File podx = File.createTempFile("podx-collaboration", ".podx");
		podx.deleteOnExit();
		PodxFileImporter writer = new PodxFileImporter();
		writer.setFileName(podx.getAbsolutePath());
		writer.setProject(original);
		writer.exportFile();
		Project first = load(podx);
		Project second = load(podx);
		CollaborationSession alice = CollaborationSession.create(first, podx.getAbsolutePath(), "alice");
		CollaborationSession bob = CollaborationSession.create(second, podx.getAbsolutePath(), "bob");
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
		Project project = Project.createProject(ResourcePool.createRourcePool("podx-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Podx task");
		return project;
	}

	private static int taskCount(Project project) {
		int count = 0;
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			tasks.next();
			count++;
		}
		return count;
	}

	private static Project load(File podx) throws Exception {
		PodxFileImporter reader = new PodxFileImporter();
		reader.setFileName(podx.getAbsolutePath());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		return reader.getProject();
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
}
