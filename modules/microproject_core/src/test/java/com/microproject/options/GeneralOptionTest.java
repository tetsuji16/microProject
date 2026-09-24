/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.options;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class GeneralOptionTest {
	@Test
	void getInstanceAlwaysReturnsTheSharedInstance() {
		assertSame(GeneralOption.getInstance(), GeneralOption.getInstance());
	}
}
