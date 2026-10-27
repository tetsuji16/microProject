/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class CommonSpreadSheetWorkspaceV2Test {
	@Test
	void stableTaskSelectionPayloadSurvivesWorkspaceSerialization() throws Exception {
		CommonSpreadSheet.Workspace workspace = new CommonSpreadSheet.Workspace();
		workspace.setWorkspaceVersion(2);
		workspace.setStableTaskSelectionPresent(true);
		workspace.setSelectedProjectIds(new long[] { 17L, 17L });
		workspace.setSelectedTaskIds(new long[] { 31L, 45L });
		workspace.setSelectedOccurrences(new int[] { 0, 1 });
		workspace.setStableScrollAnchorPresent(true);
		workspace.setScrollAnchorProjectId(17L);
		workspace.setScrollAnchorTaskId(45L);
		workspace.setScrollAnchorOccurrence(1);
		workspace.setScrollAnchorOffset(6);

		CommonSpreadSheet.Workspace restored = roundTrip(workspace);

		assertTrue(restored.hasValidStableTaskSelection());
		assertArrayEquals(new long[] { 17L, 17L }, restored.getSelectedProjectIds());
		assertArrayEquals(new long[] { 31L, 45L }, restored.getSelectedTaskIds());
		assertArrayEquals(new int[] { 0, 1 }, restored.getSelectedOccurrences());
		assertTrue(restored.hasValidStableScrollAnchor());
	}

	@Test
	void localNegativeTaskSelectionAndScrollAnchorAreRejectedForIndexFallback() throws Exception {
		CommonSpreadSheet.Workspace workspace = new CommonSpreadSheet.Workspace();
		workspace.setWorkspaceVersion(2);
		workspace.setStableTaskSelectionPresent(true);
		workspace.setSelectedProjectIds(new long[] { -999_999_991L, -999_999_991L });
		workspace.setSelectedTaskIds(new long[] { -999_999_984L, -999_999_983L });
		workspace.setSelectedOccurrences(new int[] { 0, 0 });
		workspace.setStableScrollAnchorPresent(true);
		workspace.setScrollAnchorProjectId(-999_999_991L);
		workspace.setScrollAnchorTaskId(-999_999_983L);
		workspace.setScrollAnchorOccurrence(0);
		workspace.setScrollAnchorOffset(6);

		CommonSpreadSheet.Workspace restored = roundTrip(workspace);

		assertFalse(restored.hasValidStableTaskSelection());
		assertArrayEquals(new long[] { -999_999_984L, -999_999_983L }, restored.getSelectedTaskIds());
		assertFalse(restored.hasValidStableScrollAnchor());
	}

	@Test
	void zeroAndSentinelIdentifiersAreRejectedForWorkspaceV2() {
		CommonSpreadSheet.Workspace workspace = new CommonSpreadSheet.Workspace();
		workspace.setWorkspaceVersion(2);
		workspace.setStableTaskSelectionPresent(true);
		workspace.setSelectedProjectIds(new long[] { 17L, 17L });
		workspace.setSelectedTaskIds(new long[] { 0L, -1L });
		workspace.setSelectedOccurrences(new int[] { 0, 0 });
		assertFalse(workspace.hasValidStableTaskSelection());
		workspace.setStableScrollAnchorPresent(true);
		workspace.setScrollAnchorProjectId(17L);
		workspace.setScrollAnchorTaskId(0L);
		workspace.setScrollAnchorOccurrence(0);
		workspace.setScrollAnchorOffset(0);
		assertFalse(workspace.hasValidStableScrollAnchor());
	}

	@Test
	void legacyVersionOneAndMalformedV2PayloadsUseTheIndexFallback() {
		CommonSpreadSheet.Workspace legacy = new CommonSpreadSheet.Workspace();
		legacy.setWorkspaceVersion(1);
		legacy.setSelectedRows(new int[] { 4, 8 });
		assertFalse(legacy.hasValidStableTaskSelection());

		CommonSpreadSheet.Workspace malformed = new CommonSpreadSheet.Workspace();
		malformed.setWorkspaceVersion(2);
		malformed.setStableTaskSelectionPresent(true);
		malformed.setSelectedProjectIds(new long[] { 17L, 17L });
		malformed.setSelectedTaskIds(new long[] { 31L });
		malformed.setSelectedOccurrences(new int[] { 0, 0 });
		assertFalse(malformed.hasValidStableTaskSelection());

		malformed.setStableScrollAnchorPresent(true);
		malformed.setScrollAnchorProjectId(17L);
		malformed.setScrollAnchorTaskId(31L);
		malformed.setScrollAnchorOccurrence(0);
		malformed.setScrollAnchorOffset(-1);
		assertFalse(malformed.hasValidStableScrollAnchor());

		malformed.setScrollAnchorOffset(0);
		malformed.setScrollAnchorProjectId(-1L);
		malformed.setScrollAnchorTaskId(-999_999_983L);
		assertFalse(malformed.hasValidStableScrollAnchor(), "the unassigned -1 sentinel is not a stable project ID");

		malformed.setStableTaskSelectionPresent(true);
		malformed.setSelectedProjectIds(new long[] { -999_999_991L });
		malformed.setSelectedTaskIds(new long[] { -1L });
		malformed.setSelectedOccurrences(new int[] { 0 });
		assertFalse(malformed.hasValidStableTaskSelection(), "the unassigned -1 sentinel is not a stable task ID");
	}

	private static CommonSpreadSheet.Workspace roundTrip(CommonSpreadSheet.Workspace workspace) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(workspace);
		}
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (CommonSpreadSheet.Workspace) input.readObject();
		}
	}
}
