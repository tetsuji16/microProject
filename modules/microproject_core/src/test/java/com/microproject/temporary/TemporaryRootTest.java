/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.temporary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TemporaryRootTest {
	@Test
	void overrideBeatsLocalAppDataBeatsUserHome() {
		assertEquals(Path.of("override"), TemporaryRoot.resolve("override", "appdata", "home"));
		assertEquals(Path.of("appdata", "microProject", "temp"), TemporaryRoot.resolve(" ", "appdata", "home"));
		assertEquals(Path.of("home", "AppData", "Local", "microProject", "temp"), TemporaryRoot.resolve(null, "", "home"));
	}
}
