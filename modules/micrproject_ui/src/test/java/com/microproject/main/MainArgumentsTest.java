/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MainArgumentsTest {
	@Test
	void reassemblesUnquotedWindowsMpoPaths() {
		ArrayList<String> raw = new ArrayList<>(List.of(
			"--fileNames",
			"C:\\workspace\\CCPM",
			"sample",
			"English.mpo",
			"C:\\workspace\\CCPM",
			"path",
			"comparison",
			"English.mpo"));

		assertEquals(List.of(
			"--fileNames",
			"C:\\workspace\\CCPM sample English.mpo",
			"C:\\workspace\\CCPM path comparison English.mpo"),
			Main.normalizeFileNameArguments(raw));
	}

	@Test
	void leavesQuotedPathArgumentsUnchanged() {
		ArrayList<String> raw = new ArrayList<>(List.of(
			"--fileNames", "C:\\workspace\\one.mpo", "C:\\workspace\\two.mpo"));

		assertEquals(raw, Main.normalizeFileNameArguments(raw));
	}
}
