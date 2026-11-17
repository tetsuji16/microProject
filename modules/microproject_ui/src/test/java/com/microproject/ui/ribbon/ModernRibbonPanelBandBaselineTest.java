/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
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
import com.microproject.ui.theme.MicroProjectTheme;

/**
 * The ribbon surface has one height for every tab: the tallest command group of
 * any tab.  A group caption is therefore a row of that surface, and every tab
 * must place its captions on the same baseline.  A width-aware rebuild compacts
 * the groups of the tab that no longer fits, which makes that tab's groups
 * shorter; before this contract a compacted tab lifted its captions above the
 * captions of the other tabs.
 */
class ModernRibbonPanelBandBaselineTest {
	/** 1100 keeps every tab un-compacted; 1300 and 1450 compact Resource/Format and View. */
	private static final List<Integer> WIDTHS = List.of(1100, 1300, 1450, 1920);

	@Test
	void everyTabPlacesItsGroupCaptionsOnTheSharedSurfaceBaseline() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
		for (int width : WIDTHS) {
			// Reuse one ribbon per width. Every tab of that ribbon must share the
			// one command-surface baseline, and a fresh factory per tab would also
			// need far more heap than the CI test workers have.
			JPanel host = createRibbonHost();
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			ribbon.setVisibleContextualTabs(java.util.Set.of("FormatRibbonTask", "NetworkFormatRibbonTask",
				"CalendarFormatRibbonTask", "TimelineFormatRibbonTask", "TeamPlannerFormatRibbonTask"));
			layoutAt(host, ribbon, width);
			Map<String, Integer> baselines = new LinkedHashMap<>();
			for (String tabId : MenuDefinitionSupport.ribbonTaskIds()) {
				// File is the Backstage surface, not a command tab.
				if ("FileRibbonTask".equals(tabId)) continue;
				AbstractButton tab = tab(host, tabId);
				if (!tab.isVisible()) continue;
				tab.doClick();
				layoutAt(host, ribbon, width);

				List<JComponent> bands = named(ribbon, ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME);
				assertFalse(bands.isEmpty(), "tab " + tabId + " must render its command groups at width " + width);
				for (JComponent band : bands) {
					JLabel caption = caption(band);
					assertNotNull(caption, "group without a caption in tab " + tabId);
					// The caption's bottom edge is the shared surface baseline; a
					// caption's own height must not move it.
					int baseline = SwingUtilities.convertPoint(caption, 0, caption.getHeight(), ribbon).y;
					baselines.merge(tabId, baseline, (first, second) ->
						first.intValue() == second.intValue() ? first : Integer.MIN_VALUE);
				}
			}
			assertFalse(baselines.isEmpty(), "no tab rendered at width " + width);
			int expected = baselines.values().iterator().next();
			baselines.forEach((tabId, baseline) -> assertEquals(expected, baseline.intValue(),
				"tab " + tabId + " lifts its group captions off the shared surface baseline at width " + width
					+ ": " + baselines));
		}
		});
	}

	/** Sizes the host, then lays the whole tree out as the window would. */
	private static void layoutAt(JPanel host, ModernRibbonPanel ribbon, int width) {
		ribbon.setSize(width, 0);
		host.setSize(width, 0);
		Dimension preferred = ribbon.getPreferredSize();
		ribbon.setSize(width, preferred.height);
		host.setSize(width, preferred.height);
		layoutTree(host);
	}

	private static void layoutTree(Component component) {
		if (component instanceof Container container) {
			container.doLayout();
			for (Component child : container.getComponents()) layoutTree(child);
		}
	}

	private static JLabel caption(JComponent band) {
		for (Component child : band.getComponents()) {
			if (child instanceof JLabel label) return label;
			if (child instanceof Container container) {
				for (Component inner : container.getComponents()) {
					if (inner instanceof JLabel label) return label;
				}
			}
		}
		return null;
	}

	private static List<JComponent> named(Component root, String name) {
		List<JComponent> result = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof JComponent swing && name.equals(swing.getName())) result.add(swing);
		}
		return result;
	}

	private static AbstractButton tab(JPanel host, String tabId) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)))
			.findFirst().orElseThrow(() -> new AssertionError("Ribbon tab not found: " + tabId));
	}

	private static JPanel createRibbonHost() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		return factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
	}
}
