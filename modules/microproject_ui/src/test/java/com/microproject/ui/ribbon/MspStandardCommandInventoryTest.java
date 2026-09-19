/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.SwingRibbonModel;

/** Ensures unsupported MSP services are not advertised as successful commands. */
class MspStandardCommandInventoryTest {
	@Test
	void advertisedCommandsAreCataloguedAndUnsupportedServicesRemainUnbound() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT));
		SwingRibbonModel model = new SwingRibbonFactory(
			new com.microproject.menu.MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT))
			.createModel(MenuManager.STANDARD_RIBBON);
		Set<String> advertised = RibbonCommandCatalog.from(model).stream()
			.map(RibbonCommandCatalog.CommandDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
		assertTrue(advertised.contains("RibbonInsert"));
		assertTrue(advertised.contains("RibbonIndent"));
		assertTrue(advertised.contains("RibbonLink"));
		assertFalse(advertised.contains("RibbonVisualReports"));
		assertFalse(advertised.contains("RibbonMacros"));
		assertFalse(advertised.contains("RibbonPlannerLink"));
	}
}
