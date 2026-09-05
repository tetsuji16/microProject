/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.NodeFactory;

import org.junit.jupiter.api.Test;

class MpoFileImporterTest {
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
	void portableMasterRemapsChildCrossProjectAndSharedPoolPathsAfterReopen() throws Exception {
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
		assertEquals(new File(extractedTarget.getSubprojectFile()).getCanonicalPath(),
				new File(restoredExternal.getExternalProjectFile()).getCanonicalPath());
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
