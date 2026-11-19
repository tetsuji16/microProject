/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.application.RecentProjectStore;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.backstage.BackstageAccountPage;
import com.microproject.ui.ribbon.backstage.BackstageHomePage;
import com.microproject.ui.theme.MicroProjectTheme;

/**
 * Issue #1120: the Backstage opens on a sign-in-free Home page and exposes an
 * Account page with product information and theme selection only.
 */
class BackstageHomeAccountPageTest {
	private static final long OPENED = Instant.parse("2026-11-19T03:04:00Z").toEpochMilli();
	private static final List<String> FORBIDDEN_ACCOUNT_TEXT = List.of(
			"sign in", "sign-in", "user name", "username", "connected services",
			"microsoft 365", "onedrive", "one drive");

	@Test
	void homeShowsNewProjectAndReusesTheOpenRecentList() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JButton newProject = new JButton("New project");
			newProject.setName("officeBackstageCommand-RibbonNewProject");
			int[] dispatched = { 0 };
			newProject.addActionListener(event -> dispatched[0]++);
			RecentProjectStore.Entry entry = new RecentProjectStore.Entry(
					Path.of("C:/work/Roadmap.mpo"), OPENED, false, true);
			List<RecentProjectStore.Entry> opened = new ArrayList<>();
			BackstageHomePage page = new BackstageHomePage(newProject, List.of(entry), opened::add, BackstageHomeAccountPageTest::label);

			assertEquals("officeBackstageHomePage", page.getName());
			assertNotNull(named(page, "officeBackstageHomeNewProjectSection"));
			assertNotNull(named(page, "officeBackstageHomeRecent"));
			assertNotNull(named(page, "officeBackstageHomeRecentList"));
			assertSame(newProject, named(page, "officeBackstageCommand-RibbonNewProject"));
			newProject.doClick();
			assertEquals(1, dispatched[0], "Home runs the supplied New project command control");

			JListFacade recent = new JListFacade(page);
			assertEquals(1, recent.size());
			assertEquals(entry, recent.valueAt(0));
			assertTrue(recent.renderedText().contains("Roadmap.mpo"), recent.renderedText());
			assertTrue(recent.renderedText().contains("Modified"), recent.renderedText());
			recent.activate();
			assertEquals(List.of(entry), opened, "a recent row uses the shared open action");
			for (String text : texts(page))
				assertFalse(text.matches("(?i).*(template|recommend).*"), "Home must not offer online templates: " + text);
		});
	}

	@Test
	void accountShowsProductFactsThemeAndNoSignInSurface() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			int[] updates = { 0 };
			int[] about = { 0 };
			List<Boolean> themes = new ArrayList<>();
			BackstageAccountPage.ProductInfo product = new BackstageAccountPage.ProductInfo(
					"microProject", "1.2.3", "MIT License");
			BackstageAccountPage page = new BackstageAccountPage(product, false,
					() -> updates[0]++, () -> about[0]++, themes::add, BackstageHomeAccountPageTest::label);

			assertEquals("officeBackstageAccountPage", page.getName());
			assertTrue(page.getProductName().getText().contains("microProject"));
			assertTrue(page.getProductVersion().getText().contains("1.2.3"));
			assertTrue(page.getProductLicense().getText().contains("MIT License"));
			named(page, "officeBackstageAccountCheckForUpdates");
			named(page, "officeBackstageAccountAbout");

			((AbstractButton) named(page, "officeBackstageAccountCheckForUpdates")).doClick();
			((AbstractButton) named(page, "officeBackstageAccountAbout")).doClick();
			assertEquals(1, updates[0]);
			assertEquals(1, about[0]);

			@SuppressWarnings("unchecked")
			JComboBox<String> theme = (JComboBox<String>) named(page, "officeBackstageAccountTheme");
			assertEquals(0, theme.getSelectedIndex(), "the current light theme is selected initially");
			theme.setSelectedIndex(1);
			assertEquals(List.of(true), themes, "selecting Dark calls the existing theme application API");

			String allText = String.join(" ", texts(page)).toLowerCase(Locale.ROOT);
			for (String forbidden : FORBIDDEN_ACCOUNT_TEXT)
				assertFalse(allText.contains(forbidden), "Account must not offer " + forbidden + ": " + allText);
		});
	}

	@Test
	void pageOrderKeepsOptionsPinnedAndAccountBeforeIt() {
		ResourceBundle internal = MenuDefinitionSupport.ribbonBundles(Locale.ROOT)[0];
		List<String> pages = Arrays.stream(internal.getString("FileBackstagePages").split("\\s+")).toList();
		assertEquals("home", pages.getFirst(), "Home is the first Backstage destination");
		assertTrue(pages.indexOf("account") < pages.indexOf("options"), "Account sits above pinned Options");
		assertEquals("options", internal.getString("FileBackstagePages.pinned"));
		assertEquals("open", internal.getString("FileBackstagePages.separatorAfter"));
		assertFalse("options".equals(internal.getString("FileBackstagePages.separatorAfter")),
				"pinned and separator destinations must differ");
		assertEquals("Home", MenuDefinitionSupport.menuBundle(Locale.ROOT).getString("FileBackstage.home"));
		assertEquals("Account", MenuDefinitionSupport.menuBundle(Locale.ROOT).getString("FileBackstage.account"));
		assertEquals("ホーム", MenuDefinitionSupport.menuBundle(Locale.JAPANESE).getString("FileBackstage.home"));
		assertEquals("アカウント", MenuDefinitionSupport.menuBundle(Locale.JAPANESE).getString("FileBackstage.account"));
		for (Locale locale : List.of(Locale.ROOT, Locale.JAPANESE)) {
			ResourceBundle labels = MenuDefinitionSupport.menuBundle(locale);
			assertFalse(labels.getString("FileBackstage.home.newProject").isBlank());
			assertFalse(labels.getString("FileBackstage.account.checkForUpdates").isBlank());
			assertFalse(labels.getString("FileBackstage.account.theme.dark").isBlank());
		}
	}

	@Test
	void openingBackstageSelectsHomeAndAccountHasNoSignInWidgets() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = openBackstage();
			AbstractButton home = (AbstractButton) named(backstage, "officeBackstageNav-home");
			assertTrue(home.isSelected(), "Backstage opens with Home selected");
			assertNotNull(named(backstage, "officeBackstagePage-home"));
			assertNotNull(named(backstage, "officeBackstageHomePage"));
			AbstractButton homeNew = (AbstractButton) named(backstage, "officeBackstageCommand-RibbonNewProject");
			assertNotNull(homeNew.getAction(), "Home uses the existing New project command");
			assertNull(UiComponentWalker.flatten(backstage).stream()
					.filter(component -> "officeBackstageOpenLocations".equals(component.getName()))
					.findFirst().orElse(null), "Home has no Open location column");

			((AbstractButton) named(backstage, "officeBackstageNav-new")).doClick(0);
			String canonicalCommand = ((AbstractButton) named(backstage,
					"officeBackstageCommand-RibbonNewProject")).getActionCommand();
			assertEquals(homeNew.getActionCommand(), canonicalCommand,
					"Home and New dispatch the same canonical New project command");
			assertFalse(canonicalCommand == null || canonicalCommand.isBlank());

			((AbstractButton) named(backstage, "officeBackstageNav-account")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-account"));
			assertNotNull(named(backstage, "officeBackstageAccountPage"));
			assertNotNull(named(backstage, "officeBackstageAccountTheme"));
			String allText = String.join(" ", texts(backstage)).toLowerCase(Locale.ROOT);
			for (String forbidden : FORBIDDEN_ACCOUNT_TEXT)
				assertFalse(allText.contains(forbidden), "Account destination must not render " + forbidden);
		});
	}

	private static String label(String key) {
		return key.substring(key.lastIndexOf('.') + 1).replace("theme.light", "Light").replace("theme.dark", "Dark");
	}

	private static JComponent named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> name.equals(component.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("missing component: " + name));
	}

	private static List<String> texts(Component root) {
		List<String> result = new ArrayList<>();
		collect(root, result);
		return result;
	}

	private static void collect(Component component, List<String> result) {
		if (component instanceof JLabel label) result.add(label.getText());
		if (component instanceof AbstractButton button) result.add(button.getText());
		if (component instanceof JComboBox<?> combo) {
			for (int i = 0; i < combo.getItemCount(); i++) result.add(String.valueOf(combo.getItemAt(i)));
		}
		if (component instanceof Container container)
			for (Component child : container.getComponents()) collect(child, result);
	}

	/** Small read-only facade over the shared recent-project JList. */
	private static final class JListFacade {
		private final javax.swing.JList<RecentProjectStore.Entry> list;

		JListFacade(JComponent root) {
			@SuppressWarnings("unchecked")
			javax.swing.JList<RecentProjectStore.Entry> found = (javax.swing.JList<RecentProjectStore.Entry>) named(root,
					"officeBackstageHomeRecentList");
			this.list = found;
		}

		int size() { return list.getModel().getSize(); }

		RecentProjectStore.Entry valueAt(int index) { return list.getModel().getElementAt(index); }

		String renderedText() {
			JLabel rendered = (JLabel) list.getCellRenderer()
					.getListCellRendererComponent(list, valueAt(0), 0, false, false);
			return rendered.getText();
		}

		void activate() {
			list.setSelectedIndex(0);
			list.getActionMap().get(list.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
					.get(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0)))
					.actionPerformed(new java.awt.event.ActionEvent(list,
							java.awt.event.ActionEvent.ACTION_PERFORMED, "enter"));
		}
	}

	private static JComponent openBackstage() {
		SwingRibbonFactory factory = new SwingRibbonFactory(
				new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
						MenuDefinitionSupport.ribbonBundles(Locale.ROOT))),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		List<JComponent> shown = new ArrayList<>();
		ribbon.setBackstageHost(new RibbonBackstageHost() {
			@Override public void show(JComponent backstageView, Runnable dismiss) { shown.add(backstageView); }
			@Override public void hide() { }
		});
		String fileTitle = MenuDefinitionSupport.menuBundle(Locale.ROOT).getString("FileRibbonTask.title");
		UiComponentWalker.flatten(host).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> fileTitle.equals(button.getText()) && button.getName() != null
						&& button.getName().startsWith("microProjectRibbonTab-")).findFirst().orElseThrow().doClick();
		return shown.getFirst();
	}
}
