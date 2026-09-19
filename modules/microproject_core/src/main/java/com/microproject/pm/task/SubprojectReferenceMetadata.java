/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import java.io.File;
import com.microproject.util.FilePathUtils;

/** Keeps the persisted, portable and observed identities of one linked child consistent. */
final class SubprojectReferenceMetadata {
	private SubprojectReferenceMetadata() {
	}

	static void record(Project master, SubProj reference, Project child) {
		if (reference == null)
			return;
		String childPath = child != null && child.getFileName() != null && !child.getFileName().isBlank()
			? child.getFileName() : reference.getSubprojectFile();
		if (childPath == null || childPath.isBlank())
			return;
		String canonicalChildPath = FilePathUtils.canonicalPath(childPath);
		reference.setCanonicalSubprojectPath(canonicalChildPath);
		reference.setStoredSubprojectPath(portablePath(master == null ? null : master.getFileName(), canonicalChildPath));
		if (child != null)
			reference.setLastKnownProjectId(child.getDocumentId());
		reference.setLastKnownModifiedTime(lastModified(canonicalChildPath));
	}

	private static String portablePath(String masterFileName, String canonicalChildPath) {
		if (masterFileName == null || masterFileName.isBlank())
			return canonicalChildPath;
		return FilePathUtils.relativePath(masterFileName, canonicalChildPath);
	}

	private static long lastModified(String canonicalChildPath) {
		return Math.max(0L, new File(canonicalChildPath).lastModified());
	}
}
