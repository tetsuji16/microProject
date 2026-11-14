/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
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
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.theme.MicroProjectThemeTokens;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/** Issue #1115: the File/Backstage navigation follows the Microsoft Project layout and the accent colour. */
class ModernRibbonBackstageNavigationTest {
	private static final List<String> SCROLLING_DESTINATIONS =
		List.of("new", "open", "info", "save", "saveAs", "print", "export", "close");

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
	void destinationsFollowTheMicrosoftProjectOrderWithOptionsPinnedBelowTheScrollingList() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			JComponent backstage = openBackstage(Locale.ROOT);

			JComponent destinations = named(backstage, "officeBackstageDestinations");
			assertEquals(SCROLLING_DESTINATIONS, navigationIds(destinations));
			assertTrue(SwingUtilities.getAncestorOfClass(JScrollPane.class, destinations) != null,
				"the destinations scroll when the window is short");
			JComponent pinned = named(backstage, "officeBackstagePinned");
			assertEquals(List.of("options"), navigationIds(pinned));
			assertNull(SwingUtilities.getAncestorOfClass(JScrollPane.class, pinned),
				"Options must stay visible while the destinations scroll");
			List<Component> entries = List.of(destinations.getComponents());
			int open = entries.indexOf(named(backstage, "officeBackstageNav-open"));
			assertFalse(entries.get(open + 1) instanceof AbstractButton, "a separator follows Open");
			assertSame(named(backstage, "officeBackstageNav-info"), entries.get(open + 2));
		});
	}

	@Test
	void navigationUsesTheAccentColourWithReadableTextInBothThemes() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			JComponent light = openBackstage(Locale.ROOT);
			assertEquals(FlatUiSupport.ribbonAccentColor(), named(light, "officeBackstageNavigation").getBackground(),
				"the light navigation pane is the application accent colour");
			assertNavigationIsReadable(light, MicroProjectThemeTokens.light());

			MicroProjectTheme.installDark();
			assertNavigationIsReadable(openBackstage(Locale.ROOT), MicroProjectThemeTokens.dark());
		});
	}

	@Test
	void backIsAnIconButtonWithALocalizedAccessibleName() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			AbstractButton back = (AbstractButton) named(openBackstage(Locale.ROOT), "officeBackstageBack");
			assertNotNull(back.getIcon(), "Back is drawn as an icon");
			assertTrue(back.getText() == null || back.getText().isEmpty(), "Back has no visible caption");
			assertEquals("Back", back.getAccessibleContext().getAccessibleName());

			JComponent japanese = openBackstage(Locale.JAPANESE);
			assertEquals("戻る",
				((AbstractButton) named(japanese, "officeBackstageBack")).getAccessibleContext().getAccessibleName());
			assertEquals("上書き保存", ((AbstractButton) named(japanese, "officeBackstageNav-save")).getText());
		});
	}

	@Test
	void upDownAndEnterAreBoundOnEveryNavigationEntry() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			JComponent backstage = openBackstage(Locale.ROOT);
			List<AbstractButton> entries = new ArrayList<>();
			entries.add((AbstractButton) named(backstage, "officeBackstageBack"));
			for (Component component : UiComponentWalker.flatten(backstage)) {
				if (component instanceof AbstractButton button && button.getName() != null
					&& button.getName().startsWith("officeBackstageNav-"))
					entries.add(button);
			}
			assertEquals(SCROLLING_DESTINATIONS.size() + 2, entries.size());
			for (AbstractButton entry : entries) {
				for (int key : new int[] { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER }) {
					Object binding = entry.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(key, 0));
					assertNotNull(binding, entry.getName() + " key " + key);
					assertNotNull(entry.getActionMap().get(binding), entry.getName() + " action " + binding);
				}
			}
			AbstractButton info = (AbstractButton) named(backstage, "officeBackstageNav-info");
			Object activate = info.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
			info.getActionMap().get(activate).actionPerformed(
				new java.awt.event.ActionEvent(info, java.awt.event.ActionEvent.ACTION_PERFORMED, "enter"));
			assertTrue(info.isSelected(), "Enter opens the focused destination");
			assertNotNull(named(backstage, "officeBackstagePage-info"));
		});
	}

	private static void assertNavigationIsReadable(JComponent backstage, MicroProjectThemeTokens tokens) {
		assertEquals(tokens.backstageNavigationBackground(),
			named(backstage, "officeBackstageNavigation").getBackground());
		AbstractButton selected = null;
		for (Component component : UiComponentWalker.flatten(backstage)) {
			if (component instanceof AbstractButton button && button.getName() != null
				&& button.getName().startsWith("officeBackstageNav-")) {
				assertEquals(tokens.backstageNavigationForeground(), button.getForeground(), button.getName());
				assertTrue(contrast(button.getForeground(), button.getBackground()) >= 4.5, button.getName());
				if (button.isSelected()) selected = button;
			}
		}
		assertNotNull(selected, "the first destination is selected when the Backstage opens");
		assertEquals(tokens.backstageNavigationSelectedBackground(), selected.getBackground());
	}

	private static double contrast(Color first, Color second) {
		double a = ColorLuminance.relativeLuminance(first);
		double b = ColorLuminance.relativeLuminance(second);
		return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
	}

	private static List<String> navigationIds(JComponent container) {
		List<String> ids = new ArrayList<>();
		for (Component component : container.getComponents()) {
			if (component instanceof AbstractButton button && button.getName() != null
				&& button.getName().startsWith("officeBackstageNav-"))
				ids.add(button.getName().substring("officeBackstageNav-".length()));
		}
		return ids;
	}

	private static JComponent named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
			.filter(JComponent.class::isInstance).map(JComponent.class::cast)
			.filter(component -> name.equals(component.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("missing component: " + name));
	}

	private static JComponent openBackstage(Locale locale) {
		SwingRibbonFactory factory = new SwingRibbonFactory(
			new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(locale))),
			MenuDefinitionSupport.ribbonBundles(locale));
		JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
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
		String fileTitle = MenuDefinitionSupport.menuBundle(locale).getString("FileRibbonTask.title");
		UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> fileTitle.equals(button.getText()) && button.getName() != null
				&& button.getName().startsWith("microProjectRibbonTab-")).findFirst().orElseThrow().doClick();
		assertEquals(1, shown.size(), "the File tab opens the Backstage");
		return shown.getFirst();
	}
}
