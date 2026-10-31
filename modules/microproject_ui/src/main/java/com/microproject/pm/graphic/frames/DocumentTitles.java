/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;

/** The one place that assembles a document window title. */
public final class DocumentTitles {
	private DocumentTitles() { }

	/**
	 * @param fallbackName shown when the project has no name
	 * @param isSaving the modified marker is suppressed while a save is in flight, so a
	 *        window never shows a stale marker for edits the save is about to commit
	 */
	public static String windowTitle(Project project, String fallbackName, boolean isSaving) {
		StringBuilder title = new StringBuilder(Messages.getContextString("Text.ApplicationTitle")); //$NON-NLS-1$
		if (project == null)
			return title.toString();
		String name = project.getName();
		if (name == null || name.isBlank())
			name = fallbackName;
		title.append(" - ").append(name == null || name.isBlank() ? "(unnamed project)" : name); //$NON-NLS-1$ //$NON-NLS-2$
		if (project.isMaster())
			title.append(" [Master]"); //$NON-NLS-1$
		if (project.isReadOnly())
			title.append(" [Read-only]"); //$NON-NLS-1$
		if (!isSaving && project.needsSaving())
			title.append(" *"); //$NON-NLS-1$
		return title.toString();
	}
}
