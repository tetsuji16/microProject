package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

class FlatLafSupportTest {
	@Test
	void initializeUsesRibbonChromeForWindowAndMenuBar() {
		FlatLafSupport.initialize();

		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("TitlePane.background"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("TitlePane.inactiveBackground"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("MenuBar.background"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("Menu.background"));
	}
}
