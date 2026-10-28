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
}
