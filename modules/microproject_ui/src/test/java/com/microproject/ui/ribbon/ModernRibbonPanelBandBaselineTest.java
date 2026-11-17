/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;

/** Band captions must share one baseline across every tab at every responsive width. */
class ModernRibbonPanelBandBaselineTest {
	@Test
	void bandTitlesShareOneBaselineAcrossAllTabsAtEveryWidth() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = createRibbonHost();
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(
				ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			assertNotNull(ribbon);
			List<AbstractButton> tabs = UiComponentWalker.flatten(host).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> MenuDefinitionSupport.ribbonTaskIds().stream().anyMatch(id ->
					MenuDefinitionSupport.menuBundle(Locale.ROOT).getString(id + ".title").equals(button.getText())))
				.toList();
			assertFalse(tabs.isEmpty(), "the ribbon must expose its standard tabs");

			for (int width : new int[] { 1100, 1300, 1450, 1920 }) {
				host.setSize(width, 400);
				ribbon.setSize(width, 400);
				host.doLayout();
				ribbon.doLayout();
				int expectedBottom = -1;
				for (AbstractButton tab : tabs) {
					tab.doClick();
					host.doLayout();
					ribbon.doLayout();
					int bottom = bandTitleBottom(host);
					if (expectedBottom < 0) expectedBottom = bottom;
					assertEquals(expectedBottom, bottom,
						"tab " + tab.getText() + " at width " + width
							+ " has a band caption baseline different from the first tab");
				}
			}
		});
	}

	private static JPanel createRibbonHost() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		return factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
	}

	/** Bottom edge of the first band caption, in command-surface coordinates. */
	private static int bandTitleBottom(Component root) {
		Component band = UiComponentWalker.flatten(root).stream()
			.filter(component -> ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME.equals(component.getName()))
			.filter(component -> {
				for (Component cursor = component; cursor != null; cursor = cursor.getParent())
					if ("microProjectRibbonTabBody".equals(cursor.getName())) return true;
				return false;
			})
			.findFirst().orElse(null);
		assertNotNull(band, "the active tab must show at least one ribbon group");
		JLabel title = UiComponentWalker.flatten((Container) band).stream()
			.filter(JLabel.class::isInstance).map(JLabel.class::cast)
			.filter(label -> label.getText() != null && !label.getText().isBlank())
			.findFirst().orElse(null);
		assertNotNull(title, "the group must expose its caption label");
		// Headless tests have no peer, so validate() does not lay out the tree.
		// Drive every layout manager from the root downwards instead.
		layoutTree(root);
		return title.getY() + title.getHeight();
	}

	private static void layoutTree(Component root) {
		if (!(root instanceof Container container)) return;
		java.awt.LayoutManager layout = container.getLayout();
		if (layout != null) layout.layoutContainer(container);
		for (Component child : container.getComponents()) layoutTree(child);
	}
}
