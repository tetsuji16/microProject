/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import com.microproject.pm.task.Project;

/** Document-scoped presentation state for the CCPM overlay in Gantt views. */
public final class CriticalChainDisplayState {
	private boolean visible = true;

	private CriticalChainDisplayState() {
	}

	public static boolean isVisible(Project project) {
		CriticalChainDisplayState state = project == null ? null
			: project.findTransientDocumentState(CriticalChainDisplayState.class);
		return state == null || state.visible;
	}

	public static boolean toggle(Project project) {
		if (project == null) return false;
		CriticalChainDisplayState state = project.getOrCreateTransientDocumentState(
			CriticalChainDisplayState.class, CriticalChainDisplayState::new);
		state.visible = !state.visible;
		return state.visible;
	}
}
