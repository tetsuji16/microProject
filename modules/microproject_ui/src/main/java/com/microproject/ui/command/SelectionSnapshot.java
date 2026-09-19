/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.command;

import java.util.Arrays;

/** Immutable view-row selection captured at command dispatch time. */
public record SelectionSnapshot(int[] viewRows) {
	public SelectionSnapshot {
		viewRows = viewRows == null ? new int[0] : viewRows.clone();
		Arrays.sort(viewRows);
	}

	@Override
	public int[] viewRows() {
		return viewRows.clone();
	}

	public int size() {
		return viewRows.length;
	}

	public boolean isEmpty() {
		return viewRows.length == 0;
	}
}
