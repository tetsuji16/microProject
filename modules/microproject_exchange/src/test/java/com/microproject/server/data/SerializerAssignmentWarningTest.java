/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.ObjectInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.util.SafeObjectInput;

/** A POD assignment that cannot be restored must be reported, not silently defaulted. */
class SerializerAssignmentWarningTest {
	private static final Path SAMPLE = Path.of("../../samples/Commercial construction project plan2.pod")
		.toAbsolutePath().normalize();

	private static ProjectData readSample() throws Exception {
		try (InputStream in = Files.newInputStream(SAMPLE)) {
			ObjectInput input = SafeObjectInput.create(in);
			Object value = input.readObject();
			if (value instanceof String)
				value = input.readObject();
			ProjectData data = (ProjectData) value;
			data.setMaster(true);
			data.setLocal(true);
			return data;
		}
	}

	@Test
	void corruptedAssignmentIsReportedAndOtherTasksStillLoad() throws Exception {
		int taskCount = new Serializer().deserializeLocalDocument(readSample()).getTasks().size();
		ProjectData data = readSample();
		AssignmentData damaged = null;
		for (Object taskObject : data.getTasks()) {
			TaskData taskData = (TaskData) taskObject;
			if (taskData.getAssignments() == null)
				continue;
			for (AssignmentData assignmentData : taskData.getAssignments())
				if (damaged == null && assignmentData.getSerialized() != null)
					damaged = assignmentData;
		}
		assertNotNull(damaged);
		damaged.setSerialized(new byte[] { 1, 2, 3, 4 });

		Project reloaded = new Serializer().deserializeLocalDocument(data);

		List<String> warnings = reloaded.takeLoadWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).startsWith(Messages.format("Message.assignmentsNotRestored", 1)), warnings.get(0));
		assertEquals(2, warnings.get(0).lines().count(), warnings.get(0));
		assertTrue(warnings.get(0).contains(" / "), warnings.get(0));
		assertTrue(reloaded.takeLoadWarnings().isEmpty(), "warnings are reported once");
		assertEquals(taskCount, reloaded.getTasks().size(), "the other tasks still load");
	}

	@Test
	void intactProjectProducesNoWarning() throws Exception {
		Project reloaded = new Serializer().deserializeLocalDocument(readSample());

		assertTrue(reloaded.takeLoadWarnings().isEmpty());
	}
}
