/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.ui.menu.ExtToolBarFactory;
import com.microproject.ui.menu.MenuActionMapSupport;
import com.microproject.ui.menu.MenuManager;
import com.microproject.ui.menu.MenuRibbonCommandSource;
import com.microproject.ui.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ui.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.backstage.BackstageInfoPage;
import com.microproject.ui.theme.MicroProjectTheme;

/** Issue #1117: the Backstage Info page shows commands and read-only project information. */
class BackstageInfoPageTest {
	private static final Map<String, String> LABELS = Map.of("projectInformation", "Project Information");
	private static final String DASH = "\u2014";

	@Test
	void unsavedProjectHasEmptyPathAndDashForLastModified() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BackstageInfoPage page = new BackstageInfoPage("Untitled", "", List.of(),
					List.of(new BackstageInfoPage.Property("Last modified", DASH)), null, LABELS::get);
			assertTrue(page.getProjectNameLabel().getText().contains("Untitled"));
			assertEquals("", page.getProjectPathLabel().getText());
			assertTrue(page.getPropertyValue("Last modified").getText().contains(DASH));
		});
	}

	@Test
	void savedProjectShowsNamePathAndAllReadOnlyProperties() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BackstageInfoPage page = new BackstageInfoPage("Roadmap", "C:/work/projects/Roadmap.mpo",
					List.of(), projectProperties(), null, LABELS::get);
			assertTrue(page.getProjectNameLabel().getText().contains("Roadmap"));
			assertTrue(page.getProjectPathLabel().getText().contains("C:/work/projects/Roadmap.mpo"),
					page.getProjectPathLabel().getText());
			for (BackstageInfoPage.Property property : projectProperties()) {
				JLabel value = page.getPropertyValue(property.label());
				assertNotNull(value, "missing property: " + property.label());
				assertTrue(value.getText().contains(property.value()),
						property.label() + " -> " + value.getText());
			}
			assertEquals(8, UiComponentWalker.flatten(page).stream()
					.filter(JLabel.class::isInstance)
					.filter(component -> "officeBackstageInfoPropertyValue".equals(component.getName()))
					.count());
		});
	}

	@Test
	void assignedCommandsAreShownOnceWithTheirDescription() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JButton button = new JButton("Project Information");
			button.setName("officeBackstageCommand-RibbonBackstageProjectInformation");
			button.setPreferredSize(new Dimension(220, 132));
			String description = "View and edit the project's details.";
			BackstageInfoPage page = new BackstageInfoPage("Roadmap", "C:/work/Roadmap.mpo",
					List.of(new BackstageInfoPage.Command(button, description)), projectProperties(), null, LABELS::get);
			assertSame(button, named(page, "officeBackstageCommand-RibbonBackstageProjectInformation"));
			List<String> texts = texts(page);
			assertEquals(1, texts.stream().filter(text -> text.contains(description)).count(), texts.toString());
			assertTrue(texts.stream().anyMatch(text -> text.contains("Start date")), texts.toString());
		});
	}

	@Test
	void projectInformationLinkInvokesTheSuppliedAction() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AtomicInteger calls = new AtomicInteger();
			BackstageInfoPage page = new BackstageInfoPage("Roadmap", "C:/work/Roadmap.mpo",
					List.of(), projectProperties(), calls::incrementAndGet, LABELS::get);
			JButton link = (JButton) named(page, "officeBackstageInfoPropertiesLink");
			link.doClick();
			assertEquals(1, calls.get());
		});
	}

	@Test
	void narrowWidthWrapsPropertiesBelowWithoutHorizontalScroll() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JButton button = new JButton("Project Information");
			button.setPreferredSize(new Dimension(220, 132));
			BackstageInfoPage page = new BackstageInfoPage("Roadmap", "C:/work/Roadmap.mpo",
					List.of(new BackstageInfoPage.Command(button, "View and edit the project's details.")),
					projectProperties(), null, LABELS::get);
			JComponent commands = named(page, "officeBackstageInfoCommands");
			JComponent properties = named(page, "officeBackstageInfoProperties");

			page.setSize(1000, 600);
			layoutTree(page);
			assertTrue(properties.getX() >= commands.getX() + commands.getWidth(),
					"wide layout keeps the properties on the right: " + bounds(commands) + " " + bounds(properties));
			assertTrue(properties.getY() < commands.getY() + commands.getHeight(),
					"wide layout keeps both columns on one row: " + bounds(commands) + " " + bounds(properties));

			page.setSize(380, 600);
			layoutTree(page);
			assertTrue(properties.getY() >= commands.getY() + commands.getHeight(),
					"narrow layout wraps properties below: " + bounds(commands) + " " + bounds(properties));
			assertTrue(properties.getX() + properties.getWidth() <= page.getWidth(),
					"narrow layout must not scroll horizontally: " + bounds(properties));
		});
	}

	@Test
	void infoPageIsWiredIntoTheFullBackstageAndOtherPagesSurvive() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = openBackstage();
			((AbstractButton) named(backstage, "officeBackstageNav-open")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-open"));
			assertNotNull(named(backstage, "officeBackstageOpenPage"));

			((AbstractButton) named(backstage, "officeBackstageNav-info")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-info"));
			assertNotNull(named(backstage, "officeBackstageInfoPage"));
			assertNotNull(named(backstage, "officeBackstageInfoColumns"));
			assertNotNull(named(backstage, "officeBackstageInfoCommands"));
			assertNotNull(named(backstage, "officeBackstageInfoProperties"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonBackstageProjectInformation"));
			assertNull(namedOrNull(backstage, "officeBackstageOpenPage"), "Info must not use the Open page");

			((AbstractButton) named(backstage, "officeBackstageNav-new")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePage-new"));
			assertNull(namedOrNull(backstage, "officeBackstageInfoPage"), "New must stay a card grid");
		});
	}

	private static List<BackstageInfoPage.Property> projectProperties() {
		return List.of(
				new BackstageInfoPage.Property("Start date", "2026/01/05"),
				new BackstageInfoPage.Property("Finish date", "2026/06/30"),
				new BackstageInfoPage.Property("Baseline", "2026/01/05 \u2013 2026/06/30"),
				new BackstageInfoPage.Property("Current date", "2026/11/19"),
				new BackstageInfoPage.Property("Status date", "2026/11/19"),
				new BackstageInfoPage.Property("Calendar", "Standard"),
				new BackstageInfoPage.Property("Author", "tetsuji16"),
				new BackstageInfoPage.Property("Last modified", "2026/11/19 08:02"));
	}

	private static void layoutTree(Component root) {
		if (!(root instanceof Container container)) return;
		java.awt.LayoutManager layout = container.getLayout();
		if (layout != null) layout.layoutContainer(container);
		for (Component child : container.getComponents()) layoutTree(child);
	}

	private static String bounds(Component component) {
		return component.getName() + "[" + component.getX() + "," + component.getY() + ","
				+ component.getWidth() + "x" + component.getHeight() + "]";
	}

	private static List<String> texts(Component root) {
		List<String> result = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof JLabel label) result.add(label.getText());
			if (component instanceof AbstractButton button) result.add(button.getText());
		}
		return result;
	}

	private static JComponent named(Component root, String name) {
		JComponent found = namedOrNull(root, name);
		if (found == null) throw new AssertionError("missing component: " + name);
		return found;
	}

	private static JComponent namedOrNull(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> name.equals(component.getName())).findFirst().orElse(null);
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
