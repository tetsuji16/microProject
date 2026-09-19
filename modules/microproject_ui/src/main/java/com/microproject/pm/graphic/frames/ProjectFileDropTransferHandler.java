/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/** Receives desktop file drops and routes them through the normal local-file loader. */
final class ProjectFileDropTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;
	private final GraphicManager graphicManager;

	ProjectFileDropTransferHandler(GraphicManager graphicManager) {
		this.graphicManager = graphicManager;
	}

	@Override
	public boolean canImport(TransferSupport support) {
		return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
	}

	@Override
	public boolean canImport(JComponent component, DataFlavor[] transferFlavors) {
		if (transferFlavors == null)
			return false;
		for (DataFlavor flavor : transferFlavors) {
			if (DataFlavor.javaFileListFlavor.equals(flavor))
				return true;
		}
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		return importFiles(support == null ? null : support.getTransferable());
	}

	@Override
	public boolean importData(JComponent component, Transferable transferable) {
		return importFiles(transferable);
	}

	private boolean importFiles(Transferable transferable) {
		if (transferable == null || !transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			return false;
		try {
			Object value = transferable.getTransferData(DataFlavor.javaFileListFlavor);
			if (!(value instanceof List<?> values))
				return false;
			List<String> paths = new ArrayList<String>(values.size());
			for (Object candidate : values) {
				if (candidate instanceof File file && file.isFile())
					paths.add(file.getPath());
			}
			if (paths.isEmpty())
				return false;
			graphicManager.openLocalProjectsSequentially(paths.toArray(new String[0]));
			return true;
		} catch (Exception ignored) {
			// The local loader supplies user-facing diagnostics once a selected file
			// reaches it.  A malformed drag payload itself is simply not imported.
			return false;
		}
	}
}
