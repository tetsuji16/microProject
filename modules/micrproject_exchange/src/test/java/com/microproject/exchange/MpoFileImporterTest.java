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

class MpoFileImporterTest {
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
	void manifestRejectsUnsupportedVersion() {
		byte[] projectXml = "<Project/>".getBytes(StandardCharsets.UTF_8);
		String unsupported = MpoFileImporter.manifestFor(projectXml).replace("formatVersion=\"1.0\"", "formatVersion=\"2.0\"");
		assertThrows(IOException.class, () -> MpoFileImporter.validateManifest(unsupported, projectXml));
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
	void mpoAppliesValidatedTaskUpdateOperationsToItsSnapshot() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask task = (NormalTask) firstTask(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation update = new OperationLog.Operation("00000000-0000-0000-0000-000000000011", "00000000-0000-0000-0000-000000000012", 1, java.util.Set.of(), "task.update", "00000000-0000-0000-0000-000000000013", Map.of("legacyUniqueId", Long.valueOf(task.getUniqueId()), "name", "Merged task"));
		entries.put("operations/log.jsonl", new OperationLog().writeJsonl("00000000-0000-0000-0000-000000000014", java.util.List.of(update)));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals("Merged task", findByName(loaded, "Merged task").getName());
	}

	@Test
	void mpoAppliesTaskCreateOperationsIdempotently() throws Exception {
		Project project = projectForRoundTrip();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation create = new OperationLog.Operation("00000000-0000-0000-0000-000000000021", "00000000-0000-0000-0000-000000000022", 1, java.util.Set.of(), "task.create", "00000000-0000-0000-0000-000000000023", Map.of("legacyUniqueId", Long.valueOf(9001L), "name", "Created task"));
		entries.put("operations/log.jsonl", new OperationLog().writeJsonl("00000000-0000-0000-0000-000000000024", java.util.List.of(create, create)));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals("Created task", loaded.findByUniqueId(9001L).getName());
		org.junit.jupiter.api.Assertions.assertEquals(2, taskCount(loaded));
	}

	@Test
	void mpoAppliesTaskDeleteOperationsIdempotently() throws Exception {
		Project project = projectForRoundTrip();
		long taskId = firstTask(project).getUniqueId();
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation delete = new OperationLog.Operation("00000000-0000-0000-0000-000000000031", "00000000-0000-0000-0000-000000000032", 1, java.util.Set.of(), "task.delete", "00000000-0000-0000-0000-000000000033", Map.of("legacyUniqueId", Long.valueOf(taskId)));
		entries.put("operations/log.jsonl", new OperationLog().writeJsonl("00000000-0000-0000-0000-000000000034", java.util.List.of(delete, delete)));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertNull(loaded.findByUniqueId(taskId));
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
	void mpoAppliesTaskMoveOperations() throws Exception {
		Project project = projectForRoundTrip();
		NormalTask child = (NormalTask) firstTask(project);
		NormalTask parent = (NormalTask) project.createLocalTaskNode(null).getImpl(); parent.setName("Parent");
		assignPositiveUniqueIds(project);
		ByteArrayOutputStream generated = new ByteArrayOutputStream(); new MpoFileImporter().saveProject(project, generated);
		Map<String, byte[]> entries = readEntries(generated.toByteArray());
		OperationLog.Operation move = new OperationLog.Operation("00000000-0000-0000-0000-000000000041", "00000000-0000-0000-0000-000000000042", 1, java.util.Set.of(), "task.move", "00000000-0000-0000-0000-000000000043", Map.of("legacyUniqueId", Long.valueOf(child.getUniqueId()), "parentLegacyUniqueId", Long.valueOf(parent.getUniqueId())));
		entries.put("operations/log.jsonl", new OperationLog().writeJsonl("00000000-0000-0000-0000-000000000044", java.util.List.of(move)));
		MpoFileImporter reader = new MpoFileImporter(); reader.setProjectFactory(ProjectFactory.getInstance());
		Project loaded = reader.loadProject(new ByteArrayInputStream(zip(entries).toByteArray()));
		org.junit.jupiter.api.Assertions.assertEquals(parent.getUniqueId(), loaded.findByUniqueId(child.getUniqueId()).getWbsParentTask().getUniqueId());
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
		Resource loadedResource = loaded.getResourcePool().getResourceList().stream()
			.filter(value -> !value.getAssignments().isEmpty()).findFirst().orElseThrow();
		CriticalChainService.Analysis reanalyzed = loadedService.preview(loaded, java.util.List.of(loadedResource), restored);
		org.junit.jupiter.api.Assertions.assertFalse(reanalyzed.criticalTaskIds().isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(reanalyzed.projectBuffer().plannedMillis() >= 0L);
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
	void checkedInEnglishAndJapaneseCcpmSamplesLoadForVisualization() throws Exception {
		CriticalChainService service = new CriticalChainService();
		for (String name : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project loaded = load(findSample(name));
			CriticalChainService.Settings settings = service.findSettings(loaded);
			org.junit.jupiter.api.Assertions.assertNotNull(settings, name);
			org.junit.jupiter.api.Assertions.assertTrue(settings.isEnabled(), name);
			org.junit.jupiter.api.Assertions.assertNotNull(service.findBaseline(loaded), name);
			List<Resource> resources = new ArrayList<>(loaded.getResourcePool().getResourceList());
			CriticalChainService.Analysis analysis = service.preview(loaded, resources, settings);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.criticalTaskIds().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertFalse(analysis.graphEdges().isEmpty(), name);
			org.junit.jupiter.api.Assertions.assertTrue(loaded.getPercentComplete() > 0D && loaded.getPercentComplete() < 1D, name);
			org.junit.jupiter.api.Assertions.assertTrue(analysis.criticalTaskIds().size() < taskCount(loaded), name);
		}
	}

	@Test
	void japaneseCcpmSamplePreservesImportedTaskCompletionAfterAssignments() throws Exception {
		Project loaded = load(findSample("CCPM sample 日本語.mpo"));
		com.microproject.pm.task.Task phaseFive = findByName(loaded, "工程 5：設計と検証");
		org.junit.jupiter.api.Assertions.assertNotNull(phaseFive);
		org.junit.jupiter.api.Assertions.assertEquals(1.0D, phaseFive.getPercentComplete(), 0.00001D);
		org.junit.jupiter.api.Assertions.assertEquals(1.0D, ((NormalTask) phaseFive).getPercentWorkComplete(), 0.00001D);
	}

	private static Project loadFromBytes(byte[] mpo) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		return reader.loadProject(new ByteArrayInputStream(mpo));
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
