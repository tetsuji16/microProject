package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FileHelperEdgeCaseTest {
	@Test
	void missingFileNameHasNoExtension() {
		assertNull(FileHelper.getFileExtension(null));
	}

	@Test
	void extensionMatchingIsLocaleIndependent() {
		assertEquals(FileHelper.MSP_FILE_TYPE, FileHelper.getFileType("PLAN.XLSX"));
	}
}
