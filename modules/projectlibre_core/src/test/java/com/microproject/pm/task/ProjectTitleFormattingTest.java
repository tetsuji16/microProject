package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProjectTitleFormattingTest {
	@Test
	void fileNameDoesNotStartWithSeparatorWhenProjectNameIsMissing() {
		assertEquals("C:\\projects\\plan.mpp", Project.formatTitle("", "C:\\projects\\plan.mpp"));
		assertEquals("C:\\projects\\plan.mpp", Project.formatTitle(null, "C:\\projects\\plan.mpp"));
	}

	@Test
	void namedProjectKeepsNameAndFileName() {
		assertEquals("Launch - C:\\projects\\plan.pod",
			Project.formatTitle("Launch", "C:\\projects\\plan.pod"));
		assertEquals("Launch", Project.formatTitle("Launch", null));
	}
}
