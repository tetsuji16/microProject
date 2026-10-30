/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.server.data.MPXConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;

class PodSampleResourceRoundTripTest {
	@Test
	void commercialConstructionResourcesSurviveMpoRoundTrip() throws Exception {
		LocalFileImporter pod = new LocalFileImporter();
		pod.setFileName(Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize().toString());
		pod.importFile();
		Project original = pod.getProject();
		assertNotNull(original);
		List<String> names = original.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList();
		assertTrue(names.contains("G.C. General Management"));
		assertTrue(original.getResourcePool().getResourceList().stream().mapToInt(r -> r.getAssignments().size()).sum() > 0);
		Map<Long, Long> resourceIds = MPXConverter.exportedResourceIdentities(original);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(original, bytes));
		JsonNode taskIds = null;
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.getName().equals("changes/task-identities.json")) taskIds = new ObjectMapper().readTree(zip.readAllBytes());
			}
		}
		assertNotNull(taskIds);
		Project reopened = new MpoFileImporter().loadProject(new ByteArrayInputStream(bytes.toByteArray()));
		assertEquals(names, reopened.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList());
		for (Resource resource : reopened.getResourcePool().getResourceList()) {
			Resource source = original.getResourcePool().getResourceList().stream()
				.filter(r -> r.getName().equals(resource.getName())).findFirst().orElseThrow();
			assertEquals(resourceIds.get(source.getUniqueId()).longValue(), resource.getUniqueId());
			assertEquals(source.getAssignments().size(), resource.getAssignments().size(), resource.getName());
			for (Object value : source.getAssignments()) {
				Assignment before = (Assignment) value;
				long taskId = taskIds.path(Long.toString(before.getTask().getUniqueId())).asLong(-1);
				assertTrue(resource.getAssignments().stream().map(Assignment.class::cast).anyMatch(after ->
					after.getTask().getUniqueId() == taskId && after.getResource() == resource
					&& ((NormalTask) after.getTask()).getAssignments().contains(after)), resource.getName());
			}
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource), resource.getName());
		}
	}

	/** Issue #967: POD, add a row, save as MPO, re-save, reopen from the file. */
	@Test
	void editedPodSavedAsMpoFileKeepsResourcesAcrossResave(@TempDir Path directory) throws Exception {
		Project original = loadSample();
		List<String> names = resourceNames(original);
		int assignments = assignmentCount(original);
		assertTrue(assignments > 0);
		addRow(original);
		Path target = directory.resolve("Commercial construction project plan2.mpo");
		export(original, target);
		assertReopenedResources(target, names, assignments);
		// The second save merges with the archive written by the first one.
		addRow(original);
		MpoFileImporter resave = new MpoFileImporter();
		resave.setFileName(target.toString());
		resave.setProject(original);
		resave.exportFile();
		assertTrue(resave.confirmSavedState(), "a re-save must not leave the project looking edited");
		assertReopenedResources(target, names, assignments);
	}

	/** Issue #967: a remnant MPO from an earlier attempt must not block Save As. */
	@Test
	void savingPodOverRemnantMpoFromEarlierAttemptKeepsResources(@TempDir Path directory) throws Exception {
		Path target = directory.resolve("Commercial construction project plan2.mpo");
		export(loadSample(), target);
		Project original = loadSample();
		List<String> names = resourceNames(original);
		int assignments = assignmentCount(original);
		addRow(original);
		// A plain save must not replace a file that holds another document.
		assertThrows(java.io.IOException.class, () -> export(original, target));
		MpoFileImporter saveAs = new MpoFileImporter();
		saveAs.setFileName(target.toString());
		saveAs.setProject(original);
		saveAs.setReplaceDifferentDocument(true);
		saveAs.exportFile();
		assertReopenedResources(target, names, assignments);
	}

	private static Project loadSample() throws Exception {
		LocalFileImporter pod = new LocalFileImporter();
		pod.setFileName(Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize().toString());
		pod.importFile();
		return pod.getProject();
	}

	private static void addRow(Project project) {
		NormalTask row = new NormalTask(project);
		row.setName("Row added after POD open");
		project.connectTask(row);
		project.addToDefaultOutline(null, NodeFactory.getInstance().createNode(row));
	}

	private static void export(Project project, Path target) throws Exception {
		MpoFileImporter writer = new MpoFileImporter();
		writer.setFileName(target.toString());
		writer.setProject(project);
		writer.exportFile();
	}

	private static void assertReopenedResources(Path target, List<String> names, int assignments) throws Exception {
		MpoFileImporter reader = new MpoFileImporter();
		reader.setFileName(target.toString());
		reader.setProjectFactory(ProjectFactory.getInstance());
		reader.importFile();
		Project reopened = reader.getProject();
		assertEquals(names, resourceNames(reopened));
		assertEquals(assignments, assignmentCount(reopened));
		for (Resource resource : reopened.getResourcePool().getResourceList())
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource), resource.getName());
	}

	private static List<String> resourceNames(Project project) {
		return project.getResourcePool().getResourceList().stream().map(Resource::getName).sorted().toList();
	}

	private static int assignmentCount(Project project) {
		return project.getResourcePool().getResourceList().stream().mapToInt(r -> r.getAssignments().size()).sum();
	}
}
