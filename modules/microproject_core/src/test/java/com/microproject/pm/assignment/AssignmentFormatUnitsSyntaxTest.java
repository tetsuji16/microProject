/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssignmentFormatUnitsSyntaxTest {
	@Test
	void fullWidthUnitsSuffixBecomesAsciiAndTheNameIsKept() {
		assertEquals("\uFF21lice[50%]", AssignmentFormat.normalizeUnitsSyntax("\uFF21lice\uFF3B\uFF15\uFF10\uFF05\uFF3D"));
		assertEquals("Bob[2] ", AssignmentFormat.normalizeUnitsSyntax("Bob[\uFF12] "));
	}

	@Test
	void textWithoutUnitsSuffixIsUnchanged() {
		assertEquals("\uFF21lice \uFF11", AssignmentFormat.normalizeUnitsSyntax("\uFF21lice \uFF11"));
		assertEquals("Alice[50%]", AssignmentFormat.normalizeUnitsSyntax("Alice[50%]"));
	}
}
