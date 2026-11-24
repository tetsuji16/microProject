/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.node;

import com.microproject.model.task.Project;

/** A view-only node that exposes a project's values as a summary row. */
public final class ProjectSummaryRow extends NodeBridge {
	private static final long serialVersionUID = 1L;

	public ProjectSummaryRow(Project project) {
		super(java.util.Objects.requireNonNull(project, "project"));
	}
}
