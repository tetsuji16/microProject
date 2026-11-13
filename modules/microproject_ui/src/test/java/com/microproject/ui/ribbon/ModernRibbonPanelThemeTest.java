/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatClientProperties;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/** Issue #1113: the ribbon takes the colours of a theme installed after it was built. */
class ModernRibbonPanelThemeTest {
	private boolean originalDark;

	@BeforeEach
	void captureTheme() {
		// The colours other tests observe come from the UI defaults, not from the token holder.
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
	void ribbonTakesTheColoursOfAThemeInstalledAfterItWasBuilt() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			JPanel host = createRibbonHost();
			ModernRibbonPanel ribbon = ribbon(host);
			tab(host, "Task").doClick();
			ribbon.setRibbonDisplayMode(RibbonDisplayMode.TABS_ONLY);
			assertFalse(ColorLuminance.isDark(ribbon.getBackground()), "the light ribbon starts light");

			MicroProjectTheme.installDark();
			SwingUtilities.updateComponentTreeUI(host);

			assertEquals(FlatUiSupport.ribbonChromeBackground(), ribbon.getBackground());
			assertTrue(ColorLuminance.isDark(ribbon.getBackground()), "the ribbon background must turn dark");
			for (Component component : UiComponentWalker.flatten(host)) {
				if (component instanceof AbstractButton button && button.getName() != null
					&& button.getName().startsWith("microProjectRibbonTab-")) {
					assertEquals(button.isSelected() ? FlatUiSupport.tabSelectedForeground()
						: FlatUiSupport.tabUnselectedForeground(), button.getForeground(),
						"tab text must use the dark theme's colour: " + button.getName());
					assertEquals(FlatUiSupport.ribbonChromeBackground(), button.getBackground(), button.getName());
				}
			}
			List<JComponent> bodies = named(host, "microProjectRibbonTabBody");
			assertFalse(bodies.isEmpty(), "the active tab keeps a command surface");
			for (JComponent body : bodies) {
				assertEquals(FlatUiSupport.ribbonSurfaceColor(), body.getBackground());
				assertTrue(ColorLuminance.isDark(body.getBackground()), "the command surface must turn dark");
			}
			List<JLabel> headings = new ArrayList<>();
			for (JComponent body : bodies) {
				for (Component component : UiComponentWalker.flatten(body)) {
					if (component instanceof JLabel label && label.getText() != null && !label.getText().isBlank()
						&& FlatUiSupport.ribbonBandTitleFont().equals(label.getFont()))
						headings.add(label);
				}
			}
			assertFalse(headings.isEmpty(), "the tab bodies have group headings");
			for (JLabel heading : headings) {
				assertEquals(FlatUiSupport.ribbonBandTitleForeground(), heading.getForeground(),
					"group heading: " + heading.getText());
			}

			assertTrue(tab(host, "Task").isSelected(), "the selected tab must survive the theme change");
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode(),
				"the display mode must survive the theme change");
			assertFalse(ribbon.isCommandSurfaceVisible());
		});
	}

	@Test
	void backstageCommandButtonsUseTheThemeSurfaceAndAreRebuiltOnThemeChange() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			ExtToolBarFactory registry = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = createRibbonHost(registry);
			ModernRibbonPanel ribbon = ribbon(host);
			List<JComponent> shown = new ArrayList<>();
			ribbon.setBackstageHost(new RibbonBackstageHost() {
				@Override
				public void show(JComponent backstageView, Runnable dismiss) {
					shown.add(backstageView);
				}

				@Override
				public void hide() {
				}
			});
			tab(host, "File").doClick();
			assertEquals(1, shown.size(), "the File tab opens the Backstage");

			MicroProjectTheme.installDark();
			SwingUtilities.updateComponentTreeUI(host);

			assertEquals(2, shown.size(), "an open Backstage must be shown again in the new theme");
			JComponent darkView = shown.getLast();
			assertNotSame(shown.getFirst(), darkView);
			assertTrue(ColorLuminance.isDark(darkView.getBackground()), "the Backstage surface must turn dark");
			List<AbstractButton> commands = UiComponentWalker.flatten(darkView).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> button.getName() != null && button.getName().startsWith("officeBackstageCommand-"))
				.toList();
			assertFalse(commands.isEmpty(), "the first Backstage page has command buttons");
			List<AbstractButton> immediateNavigation = UiComponentWalker.flatten(darkView).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> button.getName() != null && button.getName().startsWith("officeBackstageNav-")
					&& button.getActionCommand() != null
					&& !registry.getRibbonControlsFromId(button.getActionCommand()).isEmpty())
				.toList();
			assertFalse(immediateNavigation.isEmpty(), "the Backstage has navigation entries that run a command");
			for (AbstractButton navigation : immediateNavigation) {
				assertEquals(List.of(navigation), registry.getRibbonControlsFromId(navigation.getActionCommand()),
					"the discarded Backstage must not stay registered: " + navigation.getName());
			}
			String surface = String.format("#%06x", FlatUiSupport.ribbonSurfaceColor().getRGB() & 0xFFFFFF);
			for (AbstractButton command : commands) {
				String style = String.valueOf(command.getClientProperty(FlatClientProperties.STYLE));
				assertFalse(style.contains("#ffffff"), "no fixed white background: " + command.getName());
				assertTrue(style.contains("background: " + surface), command.getName() + " style=" + style);
			}
		});
	}

	@Test
	void menuBarAndToolBarBackgroundsFollowTheTheme() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			JMenuBar menuBar = new JMenuBar();
			JToolBar toolBar = new JToolBar();
			FlatUiSupport.followThemeBackground(menuBar, FlatUiSupport::ribbonChromeBackground);
			FlatUiSupport.followThemeBackground(toolBar, FlatUiSupport::panelBackground);
			assertEquals(FlatUiSupport.ribbonChromeBackground(), menuBar.getBackground());

			MicroProjectTheme.installDark();
			SwingUtilities.updateComponentTreeUI(menuBar);
			SwingUtilities.updateComponentTreeUI(toolBar);

			assertEquals(FlatUiSupport.ribbonChromeBackground(), menuBar.getBackground());
			assertEquals(FlatUiSupport.panelBackground(), toolBar.getBackground());
			assertTrue(ColorLuminance.isDark(menuBar.getBackground()), "the menu bar must turn dark");
			assertTrue(ColorLuminance.isDark(toolBar.getBackground()), "the tool bar must turn dark");
		});
	}

	private static List<JComponent> named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
			.filter(JComponent.class::isInstance).map(JComponent.class::cast)
			.filter(component -> name.equals(component.getName())).toList();
	}

	private static AbstractButton tab(JPanel root, String text) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText()) && button.getName() != null
				&& button.getName().startsWith("microProjectRibbonTab-")).findFirst().orElseThrow();
	}

	private static ModernRibbonPanel ribbon(JPanel host) {
		return (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
	}

	private static JPanel createRibbonHost() {
		return createRibbonHost(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)));
	}

	private static JPanel createRibbonHost(ExtToolBarFactory buttons) {
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		return factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
	}
}
