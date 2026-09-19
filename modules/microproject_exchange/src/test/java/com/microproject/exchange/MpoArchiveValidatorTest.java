/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MpoArchiveValidatorTest {
	@Test
	void missingSourceReturnsCause() {
		MpoValidationResult result = MpoArchiveValidator.validate(Path.of("does-not-exist.mpo"));
		assertFalse(result.valid());
		assertEquals("source-missing", result.reason());
	}
}
