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
