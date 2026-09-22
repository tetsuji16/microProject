/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Font;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

class FlatUiTypographyTest {
	@Test
	void tableAndGanttHeadersShareTheSamePlainFont() {
		JLabel tableHeader = new JLabel();
		FlatUiSupport.applyTableHeaderCellStyle(tableHeader, false);

		assertEquals(FlatUiSupport.ganttHeaderFont(), FlatUiSupport.headerFont());
		assertEquals(FlatUiSupport.ganttHeaderFont(), tableHeader.getFont());
		assertEquals(Font.PLAIN, tableHeader.getFont().getStyle());
	}
}
