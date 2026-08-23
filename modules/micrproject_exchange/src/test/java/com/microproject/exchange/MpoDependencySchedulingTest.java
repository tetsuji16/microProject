/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;

class MpoDependencySchedulingTest {
	@Test
	void ccpmComparisonSampleSchedulesAutomaticSuccessorsAfterTheirPredecessors() throws Exception {
		MpoFileImporter importer = new MpoFileImporter();
		importer.setProjectFactory(ProjectFactory.getInstance());
		Project project;
		try (FileInputStream input = new FileInputStream(Path.of("..", "..", "samples", "CCPM path comparison 日本語.mpo").toFile())) {
			project = importer.loadProject(input);
		}
		assertTrue(project.isForward(), "sample is scheduled from its start date");

		int dependencyCount = 0;
		for (Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task predecessor = (Task) tasks.next();
			for (Object value : predecessor.getSuccessorList()) {
				Dependency dependency = (Dependency) value;
				Task successor = (Task) dependency.getSuccessor();
				dependencyCount++;
				if (!dependency.isDisabled() && !successor.isManuallyScheduled()) {
					long expectedEnd = predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), predecessor.getDuration(), true);
					assertTrue(predecessor.getEnd() == expectedEnd,
						() -> "predecessor " + predecessor.getName() + " must retain its scheduled duration");
					assertTrue(dependency.calcDependencyDate(true, predecessor.getStart(), predecessor.getEnd(), true) >= predecessor.getEnd(),
						() -> "dependency from " + predecessor.getName() + " must be calculated from its finish");
					if (successor.getPercentComplete() == 0.0D) {
						assertTrue(successor.getStart() >= predecessor.getEnd(),
							() -> "unstarted successor " + successor.getName() + " starts before predecessor " + predecessor.getName() + " finishes");
					}
				}
			}
		}
		assertFalse(dependencyCount == 0, "sample must retain its predecessor links");
	}
}
