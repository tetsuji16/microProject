/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.TimeUnit;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;

class MpoHistoricalFixtureTest {
	private static final String FIXTURE = "ccpm-sample-previous-release.mpo";
	private static final long HOUR = 60L * 60L * 1000L;

	@AfterEach
	void closeOwnedExtractionSessions() {
		MpoExtractionOwnershipRegistry.closeAll();
	}

	@Test
	void priorReleaseMpoFixturePreservesSelectedFieldsAndReopens() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", FIXTURE);

		Project imported = load(Files.readAllBytes(fixture));
		assertProjectSemantics(imported);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		assertProjectSemantics(load(saved.toByteArray()));
	}

	private static Project load(byte[] bytes) throws Exception {
		MpoFileImporter importer = new MpoFileImporter();
		importer.setProjectFactory(ProjectFactory.getInstance());
		Project project = importer.loadProject(new ByteArrayInputStream(bytes));
		assertNotNull(project);
		return project;
	}

	private static void assertProjectSemantics(Project project) {
		assertEquals("CCPM Visualization Sample", project.getName());
		assertEquals(8, project.getTaskList().size());
		List<Task> tasks = project.getTaskList();
		Task phaseTwo = taskNamed(tasks, "Phase 2: Design and Verification");
		NormalTask phaseTwoNormal = (NormalTask) phaseTwo;
		long encodedDuration = phaseTwoNormal.getRawDuration();
		assertEquals(TimeUnit.DAYS, Duration.getType(encodedDuration));
		assertEquals(34L * HOUR, Duration.millis(encodedDuration));
		assertEquals(0.25D, phaseTwo.getPercentComplete(), 0.00001D);

		Assignment assignment = (Assignment) ((NormalTask) phaseTwo).getAssignments().get(0);
		assertEquals(34L * HOUR, assignment.getWork(null));
		assertEquals(32L * HOUR, assignment.getActualWork(null));
		assertEquals(2L * HOUR, assignment.getRemainingWork(null));
		assertEquals(1.0D, assignment.getUnits(), 0.00001D);

		Task phaseFive = taskNamed(tasks, "Phase 5: Design and Verification");
		assertEquals(1, phaseFive.getPredecessorList().size());
		Dependency predecessor = (Dependency) phaseFive.getPredecessorList().iterator().next();
		assertEquals("Phase 4: Design and Verification", ((Task) predecessor.getPredecessor()).getName());
		assertEquals(DependencyType.Kind.FS, predecessor.getDependencyKind());
		assertEquals(0L, predecessor.getLag());
	}

	private static Task taskNamed(List<Task> tasks, String name) {
		return tasks.stream().filter(task -> name.equals(task.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("Missing task from historical MPO fixture: " + name));
	}

}
