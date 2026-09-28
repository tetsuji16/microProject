/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.util.Set;

/**
 * Presentation boundary for alerts requested by application and domain workflows.
 * Parent values are opaque to core and interpreted by the installed UI presenter.
 */
public interface AlertPresenter {
	void warn(Object message, Object parent);
	void error(Object message, Object parent);
	int confirmYesNo(Object message);
	int confirm(Object message);
	boolean okCancel(Object message);
	String renameProject(String name, Set<String> projectNames, boolean saveAs);
	void warnWithOnceOption(Object message, String preference, Object parent);
}
