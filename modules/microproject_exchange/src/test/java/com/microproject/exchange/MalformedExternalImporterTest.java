/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.pm.task.Project;

import net.sf.mpxj.MPXJException;

class MalformedExternalImporterTest {
	private static final byte[] TRUNCATED_MPX = ("MPX,Microsoft Project for Windows,4.0,ANSI\r\n"
		+ "10,0,1,1,0,0,0,0,0\r\n").getBytes(StandardCharsets.US_ASCII);
	private static final byte[] TRUNCATED_PLANNER = "<?xml version=\"1.0\"?><project mrproject-version=\"2\"><tasks>"
		.getBytes(StandardCharsets.UTF_8);

	@Test
	void truncatedMpxFailsAtImporterBoundaryWithoutPublishingProjectOrChangingInput() {
		assertMalformed("mpx", TRUNCATED_MPX);
	}

	@Test
	void truncatedPlannerXmlFailsAtImporterBoundaryWithoutPublishingProjectOrChangingInput() {
		assertMalformed("planner", TRUNCATED_PLANNER);
	}

	@Test
	void validEmptyPlannerV2ProjectRemainsImportable() throws Exception {
		byte[] emptyProject = ("<?xml version=\"1.0\"?>\n"
			+ "<project name=\"Empty\" mrproject-version=\"2\" project-start=\"20260101T000000Z\">"
			+ "<properties/><phases/><calendars/><tasks/><resources/><allocations/></project>")
			.getBytes(StandardCharsets.UTF_8);
		Project imported = new MspImporter().importProject(new ByteArrayInputStream(emptyProject), "planner",
			(progress, label) -> {});
		assertEquals(0, imported.getTaskList().size());
	}

	private static void assertMalformed(String extension, byte[] source) {
		byte[] original = source.clone();
		List<String> progressLabels = new ArrayList<>();
		MPXJException failure = assertThrows(MPXJException.class,
			() -> new MspImporter().importProject(new ByteArrayInputStream(source), extension,
				(progress, label) -> progressLabels.add(label)));
		assertArrayEquals(original, source, "import must leave caller-owned source bytes unchanged");
		org.junit.jupiter.api.Assertions.assertFalse(progressLabels.contains("Completed"),
			"a failed parse must not publish a successful project");
		org.junit.jupiter.api.Assertions.assertFalse(failure.getMessage().isBlank());
	}
}
