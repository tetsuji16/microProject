/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.SwingRibbonFactory;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.FlatUiSupport;

/** Issue #1112: the window chrome takes the colours of a theme installed after it was built. */
class OfficeChromePanelThemeTest {
	private boolean originalDark;

	@BeforeEach
	void captureTheme() {
		originalDark = FlatUiSupport.isDarkTheme();
	}

	@AfterEach
	void restoreTheme() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (originalDark)
				MicroProjectTheme.installDark();
			else
				MicroProjectTheme.installLight();
		});
	}

	@Test
	void chromeColoursFollowAThemeSwitchInBothDirections() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			OfficeChromePanel panel = createPanel();
			Color lightBackground = panel.getBackground();
			assertEquals(FlatUiSupport.officeTitleBarBackground(), lightBackground);

			MicroProjectTheme.installDark();
			panel.refreshTheme();
			assertNotEquals(lightBackground, FlatUiSupport.officeTitleBarBackground(),
				"the two themes must differ for this test to mean anything");
			assertChromeUsesCurrentTheme(panel);

			MicroProjectTheme.installLight();
			panel.refreshTheme();
			assertEquals(lightBackground, panel.getBackground());
			assertChromeUsesCurrentTheme(panel);
		});
	}

	private static void assertChromeUsesCurrentTheme(OfficeChromePanel panel) {
		assertEquals(FlatUiSupport.officeTitleBarBackground(), panel.getBackground());
		assertEquals(FlatUiSupport.officeTitleBarBackground(), panel.getHeaderComponent().getBackground());
		List<JLabel> labels = UiComponentWalker.flatten(panel.getHeaderComponent()).stream()
			.filter(JLabel.class::isInstance).map(JLabel.class::cast)
			.filter(label -> label.getText() != null && !label.getText().isBlank()).toList();
		assertFalse(labels.isEmpty(), "the chrome header has text labels");
		for (Component label : labels)
			assertEquals(FlatUiSupport.officeTitleBarForeground(), label.getForeground(),
				"chrome text: " + ((JLabel) label).getText());
	}

	private static OfficeChromePanel createPanel() {
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingRibbonFactory ribbonFactory = new SwingRibbonFactory(
			new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT))),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		JPanel ribbonPanel = ribbonFactory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
		return new OfficeChromePanel(menuManager, ribbonPanel, () -> { });
	}
}
