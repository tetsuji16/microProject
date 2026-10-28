/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

class MspdiResourceImportTest {
	@Test
	void importsMppResourcesIntoOutlineAndKeepsAssignmentsBidirectionalAfterMpoReload() throws Exception {
		java.nio.file.Path sample = java.nio.file.Path.of("modules", "microproject_exchange", "testdata", "New Product.mpp");
		if (!java.nio.file.Files.isRegularFile(sample))
			sample = java.nio.file.Path.of("testdata", "New Product.mpp");
		assertTrue(java.nio.file.Files.isRegularFile(sample), sample.toString());
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.mpp.MPPReader().read(sample.toFile());
		Project imported = new MspImporter().importProject(sample.toAbsolutePath().toString(),
			(progress, label) -> {});
		assertAssignmentsPointBackToResources(imported, "direct MPP import");
		Project reopened = saveAndReload(imported);

		for (net.sf.mpxj.Resource sourceResource : source.getResources()) {
			if (sourceResource.getID() == null || sourceResource.getID() == 0) continue;
			Resource resource = reopened.getResourcePool().getResourceList().stream()
				.filter(candidate -> candidate.getUniqueId() == sourceResource.getUniqueID().longValue())
				.findFirst().orElseThrow(() -> new AssertionError("Missing MPP resource UID " + sourceResource.getUniqueID()));
			assertEquals(sourceResource.getName(), resource.getName());
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource),
				"MPP resource must be in the resource outline: " + resource.getName());
		}
		for (Task task : reopened.getTaskList()) {
			if (!(task instanceof NormalTask normal)) continue;
			for (Object value : normal.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignment.isDefault() || assignment.getResource().getUniqueId() == -65535L) continue;
				assertTrue(assignment.getResource().getAssignments().contains(assignment),
					"MPP assignment must be reachable from both task and resource after MPO reload: task="
						+ task.getName() + ", resource=" + assignment.getResource().getName()
						+ ", resourceAssignments=" + assignment.getResource().getAssignments().size());
			}
		}
	}

	@Test
	void importsMspdiResourceOutlineAndBidirectionalAssignmentsThroughMpoReload() throws Exception {
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.ProjectFile();
		source.addDefaultBaseCalendar();
		net.sf.mpxj.Task sourceTask = source.addTask();
		sourceTask.setName("Assigned task");
		sourceTask.setUniqueID(1);
		net.sf.mpxj.Resource first = addResource(source, 11, "Same name");
		net.sf.mpxj.Resource second = addResource(source, 12, "Same name");
		addResource(source, 13, "Unassigned resource");
		sourceTask.addResourceAssignment(first).setUnits(0.5d);
		sourceTask.addResourceAssignment(second).setUnits(1.0d);

		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		new net.sf.mpxj.mspdi.MSPDIWriter().write(source, xml);
		Project imported = new MspImporter().importProject(new ByteArrayInputStream(xml.toByteArray()), "xml",
			(progress, label) -> {});
		Project reopened = saveAndReload(imported);

		List<Resource> resources = reopened.getResourcePool().getResourceList();
		assertEquals(3, resources.size(), "all concrete MSPDI resource records, including unassigned, are retained");
		List<Resource> sameName = resources.stream().filter(resource -> "Same name".equals(resource.getName())).toList();
		assertEquals(2, sameName.size(), "duplicate names do not collapse distinct resource identities");
		assertTrue(sameName.get(0).getUniqueId() != sameName.get(1).getUniqueId());
		for (Resource resource : resources)
			assertNotNull(reopened.getResourcePool().getResourceOutline().search(resource),
				"imported resource must be present in the resource outline: " + resource.getName());

		NormalTask task = (NormalTask) reopened.findByUniqueId(1);
		assertNotNull(task);
		assertEquals(2, task.getAssignments().size());
		for (Object value : task.getAssignments()) {
			Assignment assignment = (Assignment) value;
			assertTrue(resources.contains(assignment.getResource()));
			assertTrue(assignment.getResource().getAssignments().contains(assignment),
				"assignment must be reachable from both its task and resource");
		}
		Resource unassigned = resources.stream().filter(resource -> "Unassigned resource".equals(resource.getName()))
			.findFirst().orElseThrow();
		assertTrue(unassigned.getAssignments().isEmpty());
		assertEquals(11L, assignmentFor(task, "Same name", 0).getResource().getUniqueId());
		assertEquals(12L, assignmentFor(task, "Same name", 1).getResource().getUniqueId());
	}

	private static net.sf.mpxj.Resource addResource(net.sf.mpxj.ProjectFile project, int uid, String name) {
		net.sf.mpxj.Resource resource = project.addResource();
		resource.setUniqueID(uid);
		resource.setName(name);
		return resource;
	}

	private static Assignment assignmentFor(NormalTask task, String resourceName, int occurrence) {
		return task.getAssignments().stream().map(Assignment.class::cast)
			.filter(assignment -> resourceName.equals(assignment.getResource().getName()))
			.sorted(java.util.Comparator.comparingLong(assignment -> assignment.getResource().getUniqueId()))
			.toList().get(occurrence);
	}

	private static void assertAssignmentsPointBackToResources(Project project, String context) {
		for (Task task : project.getTaskList()) {
			if (!(task instanceof NormalTask normal)) continue;
			for (Object value : normal.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignment.isDefault() || assignment.getResource().getUniqueId() == -65535L) continue;
				assertTrue(assignment.getResource().getAssignments().contains(assignment), context
					+ " assignment must be reachable from both task and resource: task=" + task.getName()
					+ ", resource=" + assignment.getResource().getName() + "#" + assignment.getResource().getUniqueId()
					+ ", resource assignment count=" + assignment.getResource().getAssignments().size()
					+ ", assignment=" + assignment);
			}
		}
	}

	private static Project saveAndReload(Project project) throws Exception {
		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, mpo));
		return new MpoFileImporter().loadProject(new ByteArrayInputStream(mpo.toByteArray()));
	}
}

