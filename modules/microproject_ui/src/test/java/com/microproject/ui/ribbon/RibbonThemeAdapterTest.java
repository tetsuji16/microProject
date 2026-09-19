/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.microproject.util.FlatUiSupport;

class RibbonThemeAdapterTest {
	@Test
	void flatLafAdapterPreservesExistingRibbonMetrics() {
		FlatLafRibbonTheme theme = new FlatLafRibbonTheme();
		assertEquals(FlatUiSupport.ribbonTabHeight(), theme.tabHeight());
		assertEquals(FlatUiSupport.ribbonSurfaceHeight(), theme.surfaceHeight());
		assertEquals(FlatUiSupport.ribbonLargeButtonHeight(), theme.largeButtonHeight());
		assertEquals(FlatUiSupport.ribbonInlineButtonHeight(), theme.inlineButtonHeight());
	}
}
