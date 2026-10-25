/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.exchange.mpxj.ProjectWriterFactory;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.planner.PlannerReader;

class RegisteredExternalWriterTest {
	@TempDir Path tempDir;

	@Test
	void registeredMpxWriterPreservesLiteralTaskNameAndProgress() throws Exception {
		ProjectFile source = literalProject();
		Path output = tempDir.resolve("plan.mpx");
		ProjectWriterFactory.forFile(output.toString()).write(source, output.toFile());

		ProjectFile readback = new MPXReader().read(output.toFile());
		assertEquals("Bounded task", readback.getTasks().get(0).getName());
		assertEquals(40, readback.getTasks().get(0).getPercentageComplete().intValue());
		String text = Files.readString(output);
		assertTrue(text.startsWith("MPX,Microsoft Project for Windows,4.0"));
	}

	@Test
	void registeredPlannerWriterPreservesLiteralTaskNameAndRecordsProgressLoss() throws Exception {
		ProjectFile source = literalProject();
		Path output = tempDir.resolve("plan.planner");
		ProjectWriterFactory.forFile(output.toString()).write(source, output.toFile());

		ProjectFile readback = new PlannerReader().read(output.toFile());
		assertEquals("Bounded task", readback.getTasks().get(0).getName());
		// MPXJ's Planner v2 writer drops task completion percentages.
		assertEquals(0, readback.getTasks().get(0).getPercentageComplete().intValue());
		String text = Files.readString(output);
		assertTrue(text.contains("mrproject-version=\"2\""));
	}

	private static ProjectFile literalProject() {
		ProjectFile project = new ProjectFile();
		java.util.Date start = java.util.Date.from(java.time.Instant.parse("2026-01-05T08:00:00Z"));
		project.getProjectProperties().setStartDate(start);
		project.getProjectProperties().setFinishDate(java.util.Date.from(
			java.time.Instant.parse("2026-01-05T17:00:00Z")));
		net.sf.mpxj.ProjectCalendar calendar = project.addDefaultBaseCalendar();
		project.getProjectProperties().setDefaultCalendar(calendar);
		Task task = project.addTask();
		task.setName("Bounded task");
		task.setUniqueID(1);
		task.setPercentageComplete(40);
		task.setStart(start);
		task.setFinish(java.util.Date.from(java.time.Instant.parse("2026-01-05T17:00:00Z")));
		return project;
	}

}
