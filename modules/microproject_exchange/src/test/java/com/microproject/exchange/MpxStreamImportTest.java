/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.pm.task.Project;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpx.MPXWriter;

class MpxStreamImportTest {
	@Test
	void importsMpxStreamThroughTheSupportedExtensionRoute() throws Exception {
		ProjectFile mpx = new ProjectFile();
		mpx.addDefaultBaseCalendar();
		net.sf.mpxj.Task sourceTask = mpx.addTask();
		sourceTask.setName("MPX route task");
		sourceTask.setUniqueID(1);

		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		new MPXWriter().write(mpx, encoded);

		Project imported = new MspImporter().importProject(
			new ByteArrayInputStream(encoded.toByteArray()), "mpx", (progress, label) -> {});
		assertTrue(imported.getTaskList().stream()
			.anyMatch(task -> "MPX route task".equals(task.getName())),
			"the MPX reader route must import the task payload");
	}
}
