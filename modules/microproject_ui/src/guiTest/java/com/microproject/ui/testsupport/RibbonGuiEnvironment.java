/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microproject.ui.ribbon.RibbonCustomization;
import com.microproject.util.Environment;
import com.microproject.ui.util.FlatLafSupport;

/** Applies the production ribbon look-and-feel baseline to GUI acceptance fixtures. */
public final class RibbonGuiEnvironment {
	private RibbonGuiEnvironment() {
	}

	/** Initializes the same theme and ribbon flags used by the desktop application. */
	public static void initialize() {
		FlatLafSupport.initialize();
		if (!FlatLafSupport.isFlatLafLookAndFeel()) {
			throw new IllegalStateException("Ribbon GUI acceptance tests must run with the production FlatLaf theme.");
		}
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
	}

	/** The default tab the customization fixture hides. */
	public static final String FIXTURE_HIDDEN_TAB_ID = "HelpRibbonTask";
	/** The custom tab the customization fixture adds. */
	public static final String FIXTURE_CUSTOM_TAB_ID = "custom-evidence-tab";

	/** One custom tab with two custom groups holding three commands, and one hidden default tab. */
	public static RibbonCustomization customizedRibbon() {
		RibbonCustomization.CustomGroup edit = new RibbonCustomization.CustomGroup("custom-evidence-edit", "Evidence Edit",
			List.of("RibbonFind", "RibbonCut"));
		RibbonCustomization.CustomGroup copy = new RibbonCustomization.CustomGroup("custom-evidence-copy", "Evidence Copy",
			List.of("RibbonCopy"));
		return new RibbonCustomization(List.of(), Set.of(FIXTURE_HIDDEN_TAB_ID), List.of(
			new RibbonCustomization.CustomTab(FIXTURE_CUSTOM_TAB_ID, "Evidence Tab", List.of(edit, copy))), Map.of());
	}
}
