/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		for (String sample : new String[] { "CCPM path comparison English.mpo", "CCPM path comparison 日本語.mpo" }) {
			Project project;
			try (FileInputStream input = new FileInputStream(Path.of("..", "..", "samples", sample).toFile())) {
				project = importer.loadProject(input);
			}
			assertTrue(project.isForward(), sample + " is scheduled from its start date");

			int dependencyCount = 0;
			for (Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
				Task predecessor = (Task) tasks.next();
				assertEquals(0.0D, predecessor.getPercentComplete(), 0.00001D,
					sample + ": " + predecessor.getName() + " must start without actual progress (actual start=" + predecessor.getActualStart()
						+ ", actual duration=" + predecessor.getActualDuration() + ", remaining duration=" + predecessor.getRemainingDuration() + ")");
				assertEquals(0L, predecessor.getActualStart(), sample + ": sample tasks must not have an actual start");
				for (Object value : predecessor.getSuccessorList()) {
					Dependency dependency = (Dependency) value;
					Task successor = (Task) dependency.getSuccessor();
					dependencyCount++;
					if (!dependency.isDisabled() && !successor.isManuallyScheduled()) {
						long expectedEnd = predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), predecessor.getDuration(), true);
						assertEquals(expectedEnd, predecessor.getEnd(),
							sample + ": predecessor " + predecessor.getName() + " must retain its scheduled duration");
						long dependencyDate = dependency.calcDependencyDate(true, predecessor.getStart(), predecessor.getEnd(), true);
						assertTrue(successor.getStart() >= dependencyDate,
							sample + ": successor " + successor.getName() + " starts before its predecessor link permits");
					}
				}
			}
			assertFalse(dependencyCount == 0, sample + ": sample must retain predecessor links");
		}
	}
}
