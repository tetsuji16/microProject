/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Host-facing operations required by the application shell to control a ribbon.
 *
 * <p>The shell depends on these behaviors rather than a particular ribbon
 * widget implementation, allowing the presentation to be migrated without
 * coupling frames and window chrome to its concrete component.</p>
 */
public interface RibbonController {
	String CONTEXTUAL_TABS_PROPERTY = "microproject.ribbon.contextualTabs";
	String AUTO_HIDE_REVEAL_CONTROL_PROPERTY = "microproject.ribbon.autoHideRevealControl";

	void setRibbonDisplayMode(RibbonDisplayMode mode);

	RibbonDisplayMode getRibbonDisplayMode();

	boolean isCommandSurfaceVisible();

	/** Temporarily reveals the command surface without changing the saved display mode. */
	void revealAutoHiddenRibbon();

	void toggleRibbonCollapseMode();

	void addRibbonDisplayModeListener(Consumer<RibbonDisplayMode> listener);

	void showProjectTab();

	void setVisibleContextualTabs(Collection<String> tabIds);

	void setContextualTabTitles(Map<String, String> titles);

	/** Connects File-tab navigation to the window-level Backstage surface. */
	void setBackstageHost(RibbonBackstageHost host);
}
