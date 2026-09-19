/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SelectionSnapshotTest {
	@Test
	void snapshotIsImmutableAndSorted() {
		int[] rows = { 4, 1, 3 };
		SelectionSnapshot snapshot = new SelectionSnapshot(rows);
		rows[0] = 99;
		assertArrayEquals(new int[] { 1, 3, 4 }, snapshot.viewRows());
		int[] copy = snapshot.viewRows();
		copy[0] = 88;
		assertArrayEquals(new int[] { 1, 3, 4 }, snapshot.viewRows());
	}
}
