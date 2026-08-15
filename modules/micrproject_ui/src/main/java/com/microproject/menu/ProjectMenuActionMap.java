package com.microproject.menu;

import javax.swing.Action;

/**
 * ProjectLibre-local abstraction for resolving menu actions and action ids.
 */
public interface ProjectMenuActionMap {
	Action getAction(String key);

	String getStringFromAction(Action action);
}
