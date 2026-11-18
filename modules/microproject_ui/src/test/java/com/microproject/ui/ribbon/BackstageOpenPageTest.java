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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.application.RecentProjectStore;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.backstage.BackstageOpenPage;
import com.microproject.ui.theme.MicroProjectTheme;

/** Issue #1116: the Backstage Open page lists recent projects; every other page stays a card grid. */
class BackstageOpenPageTest {
	private static final Map<String, String> LABELS = Map.of(
			"location", "Open",
			"recentItems", "Recent Items",
			"browse", "Browse...",
			"empty", "No recent files",
			"lastModified", "Modified {0}");
	private static final long OPENED = Instant.parse("2026-11-19T03:04:00Z").toEpochMilli();

	@Test
	void emptyStoreShowsAMessageInsteadOfAnEmptyTable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BackstageOpenPage page = new BackstageOpenPage(List.of(), new JButton(), new JButton(), entry -> { }, LABELS::get);
			assertNull(page.getRecentList());
			assertNotNull(page.getEmptyMessage());
			assertEquals("No recent files", page.getEmptyMessage().getText());
		});
	}

	@Test
	void oneEntryShowsFileNameFolderAndDate() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecentProjectStore.Entry entry = entry("Roadmap.mpo", "C:/work/projects/Roadmap.mpo");
			BackstageOpenPage page = new BackstageOpenPage(List.of(entry), new JButton(), new JButton(), value -> { },
					LABELS::get);
			JList<RecentProjectStore.Entry> list = page.getRecentList();
			assertNotNull(list);
			assertEquals(1, list.getModel().getSize());
			assertSame(entry, list.getModel().getElementAt(0));
			assertNull(page.getEmptyMessage());

			JLabel rendered = (JLabel) list.getCellRenderer().getListCellRendererComponent(
					list, entry, 0, false, false);
			String html = rendered.getText();
			assertTrue(html.contains("Roadmap.mpo"), html);
			assertTrue(html.contains("C:/work/projects"), html);
			assertTrue(html.contains("Modified"), html);
			assertTrue(html.contains(formattedDate()), html);
			assertEquals("C:/work/projects/Roadmap.mpo", rendered.getToolTipText());
		});
	}

	@Test
	void longFolderIsTruncatedInTheRowAndWholeInTheTooltip() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			String folder = "C:/" + "very-long-folder-name/".repeat(8) + "deep";
			RecentProjectStore.Entry entry = entry("Plan.mpo", folder + "/Plan.mpo");
			BackstageOpenPage page = new BackstageOpenPage(List.of(entry), new JButton(), new JButton(), value -> { },
					LABELS::get);
			JLabel rendered = (JLabel) page.getRecentList().getCellRenderer().getListCellRendererComponent(
					page.getRecentList(), entry, 0, false, false);
			assertTrue(rendered.getText().contains("…"), rendered.getText());
			assertFalse(rendered.getText().contains(folder), "the row must not show the whole long path");
			assertEquals(folder + "/Plan.mpo", rendered.getToolTipText());
		});
	}

	@Test
	void rowClickAndEnterOpenTheSelectedProject() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			RecentProjectStore.Entry entry = entry("Roadmap.mpo", "C:/work/projects/Roadmap.mpo");
			List<RecentProjectStore.Entry> opened = new ArrayList<>();
			Consumer<RecentProjectStore.Entry> openAction = opened::add;
			BackstageOpenPage page = new BackstageOpenPage(List.of(entry), new JButton(), new JButton(), openAction,
					LABELS::get);
			JList<RecentProjectStore.Entry> list = page.getRecentList();
			list.setSelectedIndex(0);

			list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
					0, 5, 10, 1, false, MouseEvent.BUTTON1));
			assertEquals(List.of(entry), opened, "a row click opens that project");

			Object binding = list.getInputMap(JComponent.WHEN_FOCUSED).get(
					KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0));
			assertNotNull(binding);
			list.getActionMap().get(binding).actionPerformed(
					new ActionEvent(list, ActionEvent.ACTION_PERFORMED, "enter"));
			assertEquals(List.of(entry, entry), opened, "Enter opens the selected project");
		});
	}

	@Test
	void locationButtonsAreDisplayed() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JButton recent = new JButton("Recent Items");
			JButton browse = new JButton("Browse...");
			BackstageOpenPage page = new BackstageOpenPage(List.of(), recent, browse, value -> { }, LABELS::get);
			assertTrue(page.getName().equals("officeBackstageOpenPage"));
			assertSame(recent, findNamed(page, null, recent));
			assertSame(browse, findNamed(page, null, browse));
			assertNotNull(findNamed(page, "officeBackstageOpenLocations", null));
			assertNotNull(findNamed(page, "officeBackstageOpenRecent", null));
		});
	}

	@Test
	void otherBackstagePagesRemainCardGrids() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = openBackstage();
			((AbstractButton) named(backstage, "officeBackstageNav-open")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-open"));
			assertNotNull(findNamed(backstage, "officeBackstageOpenPage", null));
			assertNotNull(named(backstage, "officeBackstageOpenLocations"));
			assertNotNull(named(backstage, "officeBackstageOpenRecent"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonRecentProjects"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonOpenProject"));

			((AbstractButton) named(backstage, "officeBackstageNav-info")).doClick(0);
			assertNull(findNamed(backstage, "officeBackstageOpenPage", null), "Info must not use the Open page");
			AbstractButton info = (AbstractButton) named(backstage, "officeBackstageCommand-RibbonBackstageProjectInformation");
			assertNotNull(info);
			assertEquals(new Dimension(220, 132), info.getPreferredSize());
		});
	}

	private static Component findNamed(Component root, String name, Component byIdentity) {
		if ((name != null && name.equals(root.getName())) || (byIdentity != null && byIdentity == root)) return root;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = findNamed(child, name, byIdentity);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static JComponent named(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> name.equals(component.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("missing component: " + name));
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

	private static RecentProjectStore.Entry entry(String fileName, String fullPath) {
		return new RecentProjectStore.Entry(Path.of(fullPath), OPENED, false, true);
	}

	private static String formattedDate() {
		return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
				.format(Instant.ofEpochMilli(OPENED).atZone(ZoneId.systemDefault()));
	}
}
