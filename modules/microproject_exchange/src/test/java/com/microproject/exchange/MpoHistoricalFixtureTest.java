/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
	private static final String FIXED_FIXTURE = "fixed-schedule-previous-writer.mpo";
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

	@Test
	void fixedSchedulePreviousWriterFixturePreservesLiteralScheduleAndWorkAfterReopen() throws Exception {
		Path fixture = Path.of("modules", "microproject_exchange", "testdata", "mpo", FIXED_FIXTURE);
		if (!Files.isRegularFile(fixture)) fixture = Path.of("testdata", "mpo", FIXED_FIXTURE);

		Project imported = load(Files.readAllBytes(fixture));
		assertFixedScheduleSemantics(imported);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		assertFixedScheduleSemantics(load(saved.toByteArray()));
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

	private static void assertFixedScheduleSemantics(Project project) {
		long monday0800 = modelTime("2030-06-03T08:00:00");
		long monday1700 = modelTime("2030-06-03T17:00:00");
		long tuesday0800 = modelTime("2030-06-04T08:00:00");
		long tuesday1700 = modelTime("2030-06-04T17:00:00");
		long eightHours = 8L * HOUR;
		assertEquals("Fixed schedule native fixture", project.getName());
		assertEquals(monday0800, project.getStart());
		assertEquals(tuesday1700, project.getEnd());
		assertEquals(42L, project.getWorkCalendar().getUniqueId());
		assertEquals(2, project.getTaskList().size());

		NormalTask predecessor = (NormalTask) taskByUid(project, 4201L);
		assertEquals(1, predecessor.getId());
		assertEquals("Predecessor — 8h", predecessor.getName());
		assertEquals(monday0800, predecessor.getStart());
		assertEquals(monday1700, predecessor.getEnd());
		assertEquals(eightHours, predecessor.getDuration());
		assertEquals(eightHours, Duration.millis(predecessor.getRawDuration()));
		assertEquals(0.0D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(com.microproject.pm.scheduling.ConstraintType.SNET, predecessor.getConstraintType());
		assertEquals(monday0800, predecessor.getConstraintDate());
		assertFalse(predecessor.isManuallyScheduled());
		assertEquals(42L, predecessor.getWorkCalendar().getUniqueId());
		assertFixtureAssignment(predecessor, 0L, 8L, 8L);

		NormalTask successor = (NormalTask) taskByUid(project, 4202L);
		assertEquals(2, successor.getId());
		assertEquals("Successor — partial progress", successor.getName());
		assertEquals(tuesday0800, successor.getStart());
		assertEquals(tuesday1700, successor.getEnd());
		assertEquals(eightHours, successor.getDuration());
		assertEquals(eightHours, Duration.millis(successor.getRawDuration()));
		assertEquals(0.25D, successor.getPercentComplete(), 0.00001D);
		assertEquals(com.microproject.pm.scheduling.ConstraintType.SNET, successor.getConstraintType());
		assertEquals(tuesday0800, successor.getConstraintDate());
		assertFalse(successor.isManuallyScheduled());
		assertEquals(42L, successor.getWorkCalendar().getUniqueId());
		assertFixtureAssignment(successor, 2L, 6L, 8L);

		assertEquals(1, successor.getPredecessorList().size());
		Dependency predecessorLink = (Dependency) successor.getPredecessorList().iterator().next();
		assertEquals(4201L, ((Task) predecessorLink.getPredecessor()).getUniqueId());
		assertEquals(DependencyType.Kind.FS, predecessorLink.getDependencyKind());
		assertEquals(0L, predecessorLink.getLag());
	}

	private static long modelTime(String floatingWallTime) {
		// MSPDI dates are floating wall times. The model stores the same wall
		// components in a UTC-like epoch so expectations stay host-zone neutral.
		return LocalDateTime.parse(floatingWallTime).toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private static Task taskByUid(Project project, long uniqueId) {
		for (Task task : project.getTaskList())
			if (task.getUniqueId() == uniqueId) return task;
		throw new AssertionError("Missing task UID " + uniqueId);
	}

	private static void assertFixtureAssignment(NormalTask task, long actualHours, long remainingHours,
		long workHours) {
		assertEquals(1, task.getAssignments().size());
		Assignment assignment = (Assignment) task.getAssignments().get(0);
		assertEquals(7L, assignment.getResource().getUniqueId());
		assertEquals("Fixture resource", assignment.getResource().getName());
		assertEquals(0.5D, assignment.getResource().getMaximumUnits(), 0.00001D);
		assertEquals(workHours * HOUR, assignment.getWork(null));
		assertEquals(actualHours * HOUR, assignment.getActualWork(null));
		assertEquals(remainingHours * HOUR, assignment.getRemainingWork(null));
		assertEquals(1.0D, assignment.getUnits(), 0.00001D);
	}

	private static Task taskNamed(List<Task> tasks, String name) {
		return tasks.stream().filter(task -> name.equals(task.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("Missing task from historical MPO fixture: " + name));
	}

}
