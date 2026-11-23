/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core;

import com.microproject.pm.task.Project;

/** A view-only node that exposes a project's values as a summary row. */
public final class ProjectSummaryRow extends NodeBridge {
	private static final long serialVersionUID = 1L;

	public ProjectSummaryRow(Project project) {
		super(java.util.Objects.requireNonNull(project, "project"));
	}
}
