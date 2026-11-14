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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/** Resource parsing and popup wiring of the ribbon control-kind contract. */
class RibbonControlContractTest {
	@Test
	void commandsWithoutControlMetadataKeepTheKindImpliedByTypeAndPresentation() {
		SwingRibbonModel model = model(Map.of());
		assertEquals(RibbonControlKind.BUTTON, button(model, "RibbonPaste").getControlKind());
		assertEquals(RibbonControlKind.TOGGLE_BUTTON, button(model, "RibbonFormatPainter").getControlKind());
		assertEquals(RibbonControlKind.SPLIT_BUTTON, button(model, "RibbonChooseSort").getControlKind());
		assertTrue(button(model, "RibbonPaste").getItemIds().isEmpty());
		assertNull(button(model, "RibbonPaste").getKeyTip());
	}

	@Test
	void controlItemsAndKeyTipAreReadFromResources() {
		SwingRibbonModel model = model(Map.of(
			"RibbonPaste.control", "splitButton",
			"RibbonPaste.items", "RibbonPaste - RibbonCopy",
			"RibbonPaste.keytip", " V ",
			"RibbonClear.control", "MENU",
			"RibbonClear.items", "RibbonCut"));
		SwingRibbonModel.RibbonButton paste = button(model, "RibbonPaste");
		assertEquals(RibbonControlKind.SPLIT_BUTTON, paste.getControlKind());
		assertTrue(paste.isSplit());
		assertEquals(List.of("RibbonPaste", "-", "RibbonCopy"), paste.getItemIds());
		assertEquals("V", paste.getKeyTip());
		assertEquals(RibbonControlKind.MENU, button(model, "RibbonClear").getControlKind());
	}

	@Test
	void groupDialogLauncherIsReadFromResources() {
		String bandId = bandOf(model(Map.of()), "RibbonPaste").getId();
		assertNull(bandOf(model(Map.of()), "RibbonPaste").getDialogLauncherId());
		SwingRibbonModel model = model(Map.of(bandId + ".dialogLauncher", "RibbonInformation"));
		assertEquals("RibbonInformation", bandOf(model, "RibbonPaste").getDialogLauncherId());
	}

	@Test
	void invalidControlMetadataFailsModelCreation() {
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.control", "carousel")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.items", "RibbonCopy")));
		assertThrows(IllegalStateException.class, () -> model(Map.of("RibbonPaste.control", "dialogBoxLauncher")));
	}

	@Test
	void splitButtonKeepsItsCommandAndGetsAnArrowThatOwnsThePopup() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button, specification(RibbonControlKind.SPLIT_BUTTON), commandSource(), key -> null);

		button.doClick(0);
		assertEquals(1, invoked.get());
		JPopupMenu popup = assertInstanceOf(JPopupMenu.class, button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertEquals(RibbonControlPopups.POPUP_NAME_PREFIX + "RibbonPaste", popup.getName());
		assertEquals(3, popup.getComponentCount());
		assertEquals("RibbonCopy", assertInstanceOf(JMenuItem.class, popup.getComponent(0)).getActionCommand());
		assertInstanceOf(JPopupMenu.Separator.class, popup.getComponent(1));
		JButton arrow = assertInstanceOf(JButton.class, button.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));
		assertEquals(RibbonControlPopups.ARROW_BUTTON_NAME_PREFIX + "RibbonPaste", arrow.getName());
		assertEquals(button, arrow.getParent());
		assertEquals(Boolean.TRUE, button.getClientProperty("MicroProject.ribbonSplit"));
		assertEquals("V", button.getClientProperty(RibbonControlPopups.KEY_TIP_PROPERTY));
	}

	@Test
	void menuOnlyOpensItsPopup() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button, specification(RibbonControlKind.MENU), commandSource(), key -> null);

		button.doClick(0);
		assertEquals(0, invoked.get(), "a menu control has no command of its own");
		assertNotNull(button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertNull(button.getClientProperty(RibbonControlPopups.ARROW_BUTTON_PROPERTY));
		assertEquals(RibbonControlKind.MENU, button.getClientProperty(RibbonControlPopups.CONTROL_KIND_PROPERTY));
	}

	@Test
	void controlsWithoutItemsAreLeftAlone() {
		AtomicInteger invoked = new AtomicInteger();
		JButton button = countingButton(invoked);
		RibbonControlPopups.install(button,
			new SwingRibbonModel.RibbonButton("RibbonPaste", SwingRibbonModel.ButtonPriority.MEDIUM),
			commandSource(), key -> null);

		button.doClick(0);
		assertEquals(1, invoked.get());
		assertNull(button.getClientProperty(RibbonControlPopups.POPUP_PROPERTY));
		assertEquals(0, button.getComponentCount());
	}

	@Test
	void onlyButtonToggleSplitAndMenuHaveADedicatedRendererSoFar() {
		for (RibbonControlKind kind : RibbonControlKind.values()) {
			boolean expected = kind == RibbonControlKind.BUTTON || kind == RibbonControlKind.TOGGLE_BUTTON
				|| kind == RibbonControlKind.SPLIT_BUTTON || kind == RibbonControlKind.MENU;
			assertEquals(expected, RibbonControlPopups.isNativelyRendered(kind), kind.name());
		}
		assertFalse(RibbonControlPopups.isNativelyRendered(RibbonControlKind.GALLERY));
	}

	private static SwingRibbonModel.RibbonButton specification(RibbonControlKind kind) {
		return new SwingRibbonModel.RibbonButton("RibbonPaste", SwingRibbonModel.ButtonPriority.MEDIUM)
			.withControl(kind, List.of("RibbonCopy", "-", "RibbonCut"), "V");
	}

	private static JButton countingButton(AtomicInteger invoked) {
		return new JButton(new AbstractAction("Paste") {
			@Override
			public void actionPerformed(ActionEvent event) {
				invoked.incrementAndGet();
			}
		});
	}

	private static MenuRibbonCommandSource commandSource() {
		return new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)));
	}

	private static SwingRibbonModel model(Map<String, String> overrides) {
		ResourceBundle[] base = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
		ResourceBundle[] bundles = new ResourceBundle[base.length + 1];
		bundles[0] = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return overrides.entrySet().stream()
					.map(entry -> new Object[] { entry.getKey(), entry.getValue() })
					.toArray(Object[][]::new);
			}
		};
		System.arraycopy(base, 0, bundles, 1, base.length);
		return new SwingRibbonFactory(new MenuRibbonCommandSource(
			new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), bundles)), bundles)
			.createModel(MenuManager.STANDARD_RIBBON);
	}

	private static SwingRibbonModel.RibbonButton button(SwingRibbonModel model, String id) {
		return bandOf(model, id).getButtons().stream()
			.filter(candidate -> candidate.getId().equals(id)).findFirst().orElseThrow();
	}

	private static SwingRibbonModel.RibbonBand bandOf(SwingRibbonModel model, String id) {
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				if (band.getButtons().stream().anyMatch(candidate -> candidate.getId().equals(id))) {
					return band;
				}
			}
		}
		throw new AssertionError("No ribbon band contains " + id);
	}
}
