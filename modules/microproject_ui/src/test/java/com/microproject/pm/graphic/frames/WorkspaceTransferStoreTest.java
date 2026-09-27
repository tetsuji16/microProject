/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WorkspaceTransferStoreTest {
	@AfterEach
	void clearSnapshot() {
		WorkspaceTransferStore.clear();
	}

	@Test
	void binarySnapshotRestoresTheExistingWorkspaceSerializationType() {
		GraphicManager.Workspace workspace = new GraphicManager.Workspace();
		HashMap<String, String> themes = new HashMap<>();
		themes.put("theme", "ocean");
		workspace.setColorThemes(themes);

		WorkspaceTransferStore.capture(workspace);

		assertInstanceOf(byte[].class, WorkspaceTransferStore.getSnapshot());
		GraphicManager.Workspace restored = assertInstanceOf(GraphicManager.Workspace.class,
			WorkspaceTransferStore.restore());
		assertEquals(themes, restored.getColorThemes());
		assertNull(restored.getFrames());
	}

	@Test
	void missingSnapshotRestoresAsNull() {
		WorkspaceTransferStore.clear();
		assertNull(WorkspaceTransferStore.restore());
	}
}
