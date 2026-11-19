/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JViewport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;

/**
 * Runtime theme switching (issue #483): several theme tokens are white in the
 * light theme, so the old-to-new colour remap cannot tell a viewport from a
 * dialog surface. Components painted from a token must be re-resolved from that
 * token, otherwise a white surface keeps another token's dark value.
 */
class ThemeTokenSurfaceRefreshTest {
	private boolean originalDark;

	@BeforeEach
	void setUp() {
		FlatLafSupport.initialize();
		originalDark = ThemeManager.isDark();
	}

	@AfterEach
	void restore() {
		ThemeManager.applyTheme(originalDark);
	}

	@Test
	void tokenPaintedSurfacesResolveTheirOwnColourAfterASwitch() {
		ThemeManager.applyTheme(false);
		JPanel root = new JPanel();
		JViewport viewport = new JViewport();
		FlatUiSupport.applyViewportSurface(viewport);
		JPanel panel = new JPanel();
		FlatUiSupport.applyPanelSurface(panel);
		JPanel header = new JPanel();
		FlatUiSupport.applyTableHeaderStyle(header);
		root.add(viewport);
		root.add(panel);
		root.add(header);

		Color lightViewport = FlatUiSupport.viewportBackground();
		ThemeManager.applyTheme(true);
		FlatUiSupport.refreshThemeColors(root);

		Color darkViewport = FlatUiSupport.viewportBackground();
		assertNotEquals(lightViewport, darkViewport, "the dark theme must change the viewport token");
		assertEquals(darkViewport, viewport.getBackground(),
			"the viewport must follow its own token, not another white surface's dark value");
		assertEquals(FlatUiSupport.panelBackground(), panel.getBackground(),
			"the panel surface must follow its own token");
		assertEquals(FlatUiSupport.labelForeground(), panel.getForeground(),
			"the panel foreground must follow its own token");
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), header.getBackground(),
			"the table-header surface must follow its own token");
		assertEquals(FlatUiSupport.headerForeground(), header.getForeground(),
			"the table-header foreground must follow its own token");
		assertNotEquals(darkViewport, panel.getBackground(),
			"two different surface tokens must not collapse onto one dark colour");

		ThemeManager.applyTheme(false);
		FlatUiSupport.refreshThemeColors(root);
		assertEquals(FlatUiSupport.viewportBackground(), viewport.getBackground(),
			"the viewport must return to its light token");
		assertEquals(lightViewport, viewport.getBackground(), "the light viewport token must be restored");
	}

	@Test
	void tokenColoursAreResolvableByName() {
		assertNotNull(FlatUiSupport.themeTokenColor("viewport"));
		assertEquals(FlatUiSupport.viewportBackground(), FlatUiSupport.themeTokenColor("viewport"));
		assertEquals(FlatUiSupport.panelBackground(), FlatUiSupport.themeTokenColor("panel"));
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), FlatUiSupport.themeTokenColor("spreadsheetHeader"));
		assertNull(FlatUiSupport.themeTokenColor("not-a-token"));
	}
}
