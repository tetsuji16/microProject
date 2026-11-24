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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.ui.menu.ExtToolBarFactory;
import com.microproject.ui.menu.MenuActionMapSupport;
import com.microproject.ui.menu.MenuManager;
import com.microproject.ui.menu.MenuRibbonCommandSource;
import com.microproject.ui.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ui.menu.testsupport.UiComponentWalker;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.ribbon.backstage.BackstageExportPage;
import com.microproject.ui.ribbon.backstage.BackstageSaveAsPage;
import com.microproject.ui.theme.MicroProjectTheme;

/** Issue #1119: Office-style Backstage Save As and Export pages. */
class BackstageSaveAsAndExportPageTest {
	private static final Map<String, String> LABELS = Map.ofEntries(
			Map.entry("FileBackstage.saveAs.thisPc", "This PC"),
			Map.entry("FileBackstage.saveAs.browse", "Browse..."),
			Map.entry("FileBackstage.saveAs.currentFolder", "Current folder"),
			Map.entry("FileBackstage.saveAs.recentFolders", "Recent folders"),
			Map.entry("FileBackstage.saveAs.empty", "No recent folders"),
			Map.entry("FileBackstage.saveAs.description", "Choose a folder to save this project in."),
			Map.entry("FileBackstage.saveAs.mpo", "MicroProject file (*.mpo)"),
			Map.entry("FileBackstage.saveAs.otherFormat", "Other file formats..."),
			Map.entry("FileBackstage.export.pdfDocument", "Create PDF/XPS document"),
			Map.entry("FileBackstage.export.changeType", "Change file type"),
			Map.entry("FileBackstage.export.pdfDescription", "Create a PDF or XPS document of the current view."),
			Map.entry("FileBackstage.export.changeTypeDescription", "Save the current project in another file format."),
			Map.entry("FileBackstage.export.description.RibbonPDF", "PDF document of the current view"),
			Map.entry("FileBackstage.export.description.RibbonExportProject", "Export the project to another file format"),
			Map.entry("FileBackstage.export.description.RibbonImportProject", "Open a project saved in another file format"),
			Map.entry("FileBackstage.export.pdfAction", "Create PDF/XPS..."),
			Map.entry("FileBackstage.export.exportAction", "Change file type..."),
			Map.entry("FileBackstage.export.importAction", "Open other format..."));

	private static Path path(String value) { return Path.of(value); }

	private static AbstractButton command(String id) {
		JButton button = new JButton(id);
		button.setActionCommand(id);
		button.setName("officeBackstageCommand-" + id);
		return button;
	}

	private static String label(String key) { return LABELS.getOrDefault(key, key); }

	@Test
	void saveAsPageShowsThisPcBrowseAndDistinctRecentFolders() {
		Path current = path("C:/work/current.mpo").getParent();
		Path first = path("C:/recent/one");
		Path second = path("C:/recent/two");
		BackstageSaveAsPage page = new BackstageSaveAsPage(current.toString(), List.of(first, first, second),
				command("RibbonSaveMpoAs"), command("RibbonSaveProjectAs"), picked -> { }, BackstageSaveAsAndExportPageTest::label);

		List<String> texts = texts(page);
		assertTrue(texts.contains("This PC"), texts.toString());
		assertTrue(texts.contains("Browse..."), texts.toString());
		assertTrue(texts.contains("Current folder"), texts.toString());
		assertTrue(texts.contains("Recent folders"), texts.toString());
		assertTrue(texts.contains(current.toString().replace('\\', '/')), texts.toString());
		assertNotNull(named(page, "officeBackstageSaveAsCurrentFolder"));
		assertNotNull(named(page, "officeBackstageSaveAsChoice-thisPc"));
		assertNotNull(named(page, "officeBackstageSaveAsChoice-browse"));
		assertEquals(first.toString().replace('\\', '/'), ((AbstractButton) named(page, "officeBackstageSaveAsFolder-0")).getText());
		assertEquals(second.toString().replace('\\', '/'), ((AbstractButton) named(page, "officeBackstageSaveAsFolder-1")).getText());
		assertTrue(find(page, "officeBackstageSaveAsFolder-2") == null, "duplicate parent folders must be removed");
		assertTrue(texts.stream().noneMatch(text -> text.contains("OneDrive") || text.contains("Add a place")
				|| text.contains("サイト")), "cloud locations must not be offered: " + texts);
	}

	@Test
	void folderAndBrowseInvokeTheSuppliedOpenAction() {
		Path folder = path("C:/recent/one");
		AtomicReference<Path> picked = new AtomicReference<>();
		BackstageSaveAsPage page = new BackstageSaveAsPage(null, List.of(folder),
				command("RibbonSaveMpoAs"), command("RibbonSaveProjectAs"), picked::set, BackstageSaveAsAndExportPageTest::label);
		((AbstractButton) named(page, "officeBackstageSaveAsFolder-0")).doClick();
		assertEquals(folder, picked.get(), "clicking a folder must hand that folder to the caller");
		((AbstractButton) named(page, "officeBackstageSaveAsChoice-browse")).doClick();
		assertNull(picked.get(), "Browse must open the chooser without a preset folder");
	}

	@Test
	void unsavedProjectHasNoCurrentFolder() {
		BackstageSaveAsPage page = new BackstageSaveAsPage(null, List.of(),
				command("RibbonSaveMpoAs"), command("RibbonSaveProjectAs"), picked -> { }, BackstageSaveAsAndExportPageTest::label);
		assertTrue(find(page, "officeBackstageSaveAsCurrentFolder") == null,
				"an unsaved project must not show a current folder");
		assertTrue(texts(page).contains("No recent folders"));
	}

	@Test
	void exportPageShowsOnlySuppliedCommandsAndNoPodExport() {
		BackstageExportPage page = new BackstageExportPage(command("RibbonPDF"),
				command("RibbonExportProject"), command("RibbonImportProject"), BackstageSaveAsAndExportPageTest::label);
		List<String> texts = texts(page);
		assertTrue(texts.contains("Create PDF/XPS document"), texts.toString());
		assertTrue(texts.contains("Change file type"), texts.toString());
		for (String id : List.of("RibbonPDF", "RibbonExportProject", "RibbonImportProject"))
			assertNotNull(named(page, "officeBackstageCommand-" + id), "missing export command " + id);
		assertNotNull(named(page, "officeBackstageExportChoice-pdf"));
		assertNotNull(named(page, "officeBackstageExportChoice-changeType"));
		assertTrue(texts.stream().noneMatch(text -> text.toUpperCase(java.util.Locale.ROOT).contains("POD")),
				"POD export must not be offered: " + texts);
	}

	@Test
	void exportExecuteButtonsInvokeOnlyTheSuppliedCommands() {
		AbstractButton pdf = command("RibbonPDF");
		AbstractButton export = command("RibbonExportProject");
		AbstractButton imported = command("RibbonImportProject");
		AtomicInteger calls = new AtomicInteger();
		for (AbstractButton button : List.of(pdf, export, imported))
			button.addActionListener(event -> calls.incrementAndGet());
		BackstageExportPage page = new BackstageExportPage(pdf, export, imported, BackstageSaveAsAndExportPageTest::label);
		assertEquals(0, calls.get(), "opening the page must not dispatch an export");
		((AbstractButton) named(page, "officeBackstageCommand-RibbonPDF")).doClick();
		assertEquals(1, calls.get(), "PDF execute button must invoke the supplied PDF command");
		((AbstractButton) named(page, "officeBackstageCommand-RibbonExportProject")).doClick();
		((AbstractButton) named(page, "officeBackstageCommand-RibbonImportProject")).doClick();
		assertEquals(3, calls.get(), "Change file type buttons must invoke their own commands");
	}

	@Test
	void saveAsFolderHintKeepsTheMpoDefault() throws Exception {
		Path folder = Files.createTempDirectory("microproject-backstage-saveas-");
		try {
			assertEquals(folder, GraphicManager.saveAsFolderFromCommand(folder.toString()));
			assertNull(GraphicManager.saveAsFolderFromCommand("RibbonSaveProjectAs"));
			assertNull(GraphicManager.saveAsFolderFromCommand(folder.resolve("missing").toString()));
			assertEquals(folder.resolve("project.mpo").toString(),
					GraphicManager.saveAsInFolder("project.mpo", folder));
			assertEquals("project.mpo", GraphicManager.saveAsInFolder("project.mpo", null));
		} finally {
			Files.deleteIfExists(folder);
		}
	}

	@Test
	void pagesAreWiredIntoTheFullBackstageAndPrintHasNoPdf() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = openBackstage();
			((AbstractButton) named(backstage, "officeBackstageNav-saveAs")).doClick(0);
			assertNotNull(named(backstage, "officeBackstageSaveAsPage"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonSaveMpoAs"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonSaveProjectAs"));

			((AbstractButton) named(backstage, "officeBackstageNav-export")).doClick(0);
			assertNotNull(named(backstage, "officeBackstageExportPage"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonPDF"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonExportProject"));
			assertNotNull(named(backstage, "officeBackstageCommand-RibbonImportProject"));

			((AbstractButton) named(backstage, "officeBackstageNav-print")).doClick(0);
			assertNotNull(named(backstage, "officeBackstagePrintPage"));
			assertNull(find(backstage, "officeBackstageCommand-RibbonPDF"),
					"PDF export moved to Export and must be absent from Print");
		});
	}

	/**
	 * #1339: the detail column must follow the available width, not keep its preferred width and push
	 * the right side of a section out of view. 683 logical px at 150 % leaves about 380 px beside the
	 * Backstage navigation.
	 */
	@Test
	void narrowPageKeepsTheDetailColumnInsideTheAvailableWidth() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BackstageSaveAsPage saveAs = new BackstageSaveAsPage("C:/work", List.of(path("C:/recent/one")),
					command("RibbonSaveMpoAs"), command("RibbonSaveProjectAs"), picked -> { },
					BackstageSaveAsAndExportPageTest::label);
			assertDetailFits(saveAs, "officeBackstageSaveAsSection-browse");

			BackstageExportPage export = new BackstageExportPage(command("RibbonPDF"),
					command("RibbonExportProject"), command("RibbonImportProject"),
					BackstageSaveAsAndExportPageTest::label);
			assertDetailFits(export, "officeBackstageExportSection-changeType");
		});
	}

	private static void assertDetailFits(JComponent page, String sectionName) {
		layoutTree(page, 380, 500);
		assertSectionInsidePage(page, sectionName, "narrow");
		Component list = page.getComponent(0);
		Component detail = page.getComponent(1);
		assertTrue(detail.getY() >= list.getY() + list.getHeight(),
				"a narrow page must place the detail under the choice list: list=" + list.getBounds()
						+ " detail=" + detail.getBounds());
		for (Component choice : ((java.awt.Container) list).getComponents()) {
			if (!(choice instanceof AbstractButton button)) continue;
			assertTrue(button.getWidth() > 0 && button.getHeight() > 0,
					"choice buttons must stay visible when the detail moves under them: " + button.getBounds());
		}

		layoutTree(page, 900, 500);
		assertSectionInsidePage(page, sectionName, "wide");
		assertTrue(detail.getX() >= list.getX() + list.getWidth(),
				"a wide page must keep the detail beside the choice list: list=" + list.getBounds()
						+ " detail=" + detail.getBounds());
	}

	private static void assertSectionInsidePage(JComponent page, String sectionName, String shape) {
		JComponent section = named(page, sectionName);
		java.awt.Rectangle sectionBounds = SwingUtilities.convertRectangle(section.getParent(),
				section.getBounds(), page);
		assertTrue(new java.awt.Rectangle(page.getSize()).contains(sectionBounds),
				sectionName + " must stay inside a " + shape + " page: " + sectionBounds + " page=" + page.getSize());
		for (AbstractButton button : UiComponentWalker.flatten(section).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList()) {
			java.awt.Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), page);
			assertTrue(new java.awt.Rectangle(page.getSize()).contains(bounds),
					button.getName() + " must stay inside a " + shape + " page: " + bounds + " page=" + page.getSize());
		}
	}

	private static void layoutTree(java.awt.Container root, int width, int height) {
		root.setSize(width, height);
		layoutDescendants(root);
	}

	private static void layoutDescendants(java.awt.Container container) {
		container.doLayout();
		for (Component child : container.getComponents())
			if (child instanceof java.awt.Container nested) layoutDescendants(nested);
	}

	private static List<String> texts(Component root) {
		List<String> result = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof javax.swing.JLabel label && label.getText() != null) result.add(label.getText());
			if (component instanceof AbstractButton button && button.getText() != null) result.add(button.getText());
		}
		return result;
	}

	private static JComponent named(Component root, String name) {
		JComponent found = find(root, name);
		if (found == null) throw new AssertionError("missing component: " + name);
		return found;
	}

	private static JComponent find(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> name.equals(component.getName())).findFirst().orElse(null);
	}

	private static JComponent openBackstage() {
		SwingRibbonFactory factory = new SwingRibbonFactory(
				new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
						MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT))),
				MenuDefinitionSupport.ribbonBundles(java.util.Locale.ROOT));
		JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		List<JComponent> shown = new ArrayList<>();
		ribbon.setBackstageHost(new RibbonBackstageHost() {
			@Override public void show(JComponent backstageView, Runnable dismiss) { shown.add(backstageView); }
			@Override public void hide() { }
		});
		String fileTitle = MenuDefinitionSupport.menuBundle(java.util.Locale.ROOT).getString("FileRibbonTask.title");
		UiComponentWalker.flatten(host).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> fileTitle.equals(button.getText()) && button.getName() != null
						&& button.getName().startsWith("microProjectRibbonTab-")).findFirst().orElseThrow().doClick();
		return shown.getFirst();
	}
}
