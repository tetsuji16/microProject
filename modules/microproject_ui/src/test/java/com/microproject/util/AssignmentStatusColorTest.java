/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;

class AssignmentStatusColorTest {
	@Test
	void assignmentStatusFillsUseMutedThemeColors() {
		MicroProjectTheme.installLight();

		assertEquals(new Color(0xE5F0E5), FlatUiSupport.assignmentCompleteBackground());
		assertEquals(new Color(0xFFF3D9), FlatUiSupport.assignmentPartialBackground());
	}
}
