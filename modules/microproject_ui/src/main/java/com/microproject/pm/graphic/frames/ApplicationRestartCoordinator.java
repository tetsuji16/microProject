/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

/** Owns the optional action that replaces the current application shell. */
final class ApplicationRestartCoordinator {
	private Runnable restartAction;

	void setRestartAction(Runnable restartAction) {
		this.restartAction = restartAction;
	}

	boolean restart() {
		if (restartAction == null)
			return false;
		restartAction.run();
		return true;
	}
}
