/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.util.SafeObjectInput;
import com.microproject.workspace.WorkspaceSetting;

/** Holds the in-memory binary workspace snapshot used while restarting the UI. */
final class WorkspaceTransferStore {
	private static final Logger logger = Logger.getLogger(WorkspaceTransferStore.class.getName());
	private static volatile byte[] snapshot;

	private WorkspaceTransferStore() {
	}

	static void capture(WorkspaceSetting workspace) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
				objectOutput.writeObject(workspace);
			}
			snapshot = output.toByteArray();
		} catch (IOException exception) {
			logger.log(Level.WARNING, "Failed to encode workspace", exception);
		}
	}

	static WorkspaceSetting restore() {
		byte[] currentSnapshot = snapshot;
		if (currentSnapshot == null)
			return null;
		try (ObjectInputStream input = SafeObjectInput.create(new ByteArrayInputStream(currentSnapshot))) {
			return (WorkspaceSetting) input.readObject();
		} catch (IOException | ClassNotFoundException exception) {
			logger.log(Level.WARNING, "Failed to decode binary workspace", exception);
			return null;
		}
	}

	static Object getSnapshot() {
		return snapshot;
	}

	static void clear() {
		snapshot = null;
	}
}
