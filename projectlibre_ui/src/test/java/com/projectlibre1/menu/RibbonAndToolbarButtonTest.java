package com.projectlibre1.menu;

import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertAttachedButtonsAreVisible;
import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertValidCommandButton;
import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertValidSwingButton;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuBundle;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandsByTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIdsForTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBundles;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.stubActionMap;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.toolBarButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.ribbon.AbstractRibbonBand;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;

import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.frames.GraphicManager;

class RibbonAndToolbarButtonTest {
	@Test
	void standardRibbonButtonsCanBeConstructedInDefaultLocale() throws Exception {
		ExtRibbonFactory factory = new ExtRibbonFactory(stubActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractCommandButton button = factory.createJButton(id);
				assertValidCommandButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonButtonsCanBeConstructedInJapaneseLocale() throws Exception {
		ExtRibbonFactory factory = new ExtRibbonFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractCommandButton button = factory.createJButton(id);
				assertValidCommandButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonCreatesAttachedVisibleButtons() throws Exception {
		ExtRibbonFactory factory = new ExtRibbonFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (RibbonTask task : factory.createRibbon("StandardRibbon", null)) {
				for (AbstractRibbonBand<?> band : task.getBands()) {
					assertAttachedButtonsAreVisible(band, task.getTitle() + "/" + band.getTitle());
				}
			}
		});
	}

	@Test
	void standardRibbonButtonsResolveAgainstLiveActionWiring() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager menuManager = graphicManager.getMenuManager();
			menuManager.getRibbon(MenuManager.STANDARD_RIBBON, null);

			for (String id : ribbonButtonIds()) {
				String actionId = menuManager.getRibbonFactory().getActionStringFromId(id);
				assertTrue(actionId != null && !actionId.isBlank(), () -> id + " is missing a ribbon action mapping");
				org.junit.jupiter.api.Assertions.assertDoesNotThrow(
					() -> graphicManager.getAction(actionId),
					() -> id + " does not resolve to a live action: " + actionId);
			}
		});
	}

	@Test
	void ribbonViewToolbarButtonsCanBeConstructed() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : toolBarButtonIds(MenuManager.RIBBON_VIEW_BAR)) {
				assertValidSwingButton(id, factory.createJButton(id), true);
			}
			JToolBar toolBar = factory.createJToolBar(MenuManager.RIBBON_VIEW_BAR);
			assertAttachedButtonsAreVisible(toolBar, MenuManager.RIBBON_VIEW_BAR);
		});
	}

	@Test
	void printPreviewToolbarButtonsCanBeConstructed() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : toolBarButtonIds(MenuManager.PRINT_PREVIEW_TOOL_BAR)) {
				assertValidSwingButton(id, factory.createJButton(id), true);
			}
			JToolBar toolBar = factory.createJToolBar(MenuManager.PRINT_PREVIEW_TOOL_BAR);
			assertAttachedButtonsAreVisible(toolBar, MenuManager.PRINT_PREVIEW_TOOL_BAR);
		});
	}

	@Test
	void japaneseBundleStillProvidesLabelsForStandardRibbonButtons() {
		var japaneseBundle = menuBundle(Locale.JAPANESE);
		for (String id : ribbonButtonIds()) {
			assertTrue(
				com.projectlibre1.menu.testsupport.MenuDefinitionSupport.hasLocalizedLabel(japaneseBundle, id),
				() -> id + " is missing Japanese text and tooltip");
		}
	}

	@Test
	void ribbonButtonsCanHaveSelectionStateUpdatedWithoutClassCast() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(stubActionMap());
			manager.getRibbon(MenuManager.STANDARD_RIBBON, null);
			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
				manager.setActionSelected("Projects", true);
				manager.setActionSelected("Report", true);
				manager.setActionEnabled("Projects", true);
				manager.setActionVisible("Report", true);
			});
		});
	}

	@Test
	void standardRibbonUsesMsProjectStyleTaskOrder() {
		assertEquals(
			java.util.List.of(
				"FileRibbonTask",
				"TaskRibbonTask",
				"ResourceRibbonTask",
				"ReportRibbonTask",
				"ProjectRibbonTask",
				"ViewRibbonTask",
				"FormatRibbonTask"),
			ribbonTaskIds());
	}

	@Test
	void viewRibbonTaskOwnsItsViewButtonsExclusively() {
		Set<String> viewButtons = ribbonButtonIdsForTask("ViewRibbonTask");
		Set<String> taskButtons = ribbonButtonIdsForTask("TaskRibbonTask");
		Set<String> resourceButtons = ribbonButtonIdsForTask("ResourceRibbonTask");
		Set<String> reportButtons = ribbonButtonIdsForTask("ReportRibbonTask");

		for (String buttonId : viewButtons) {
			assertFalse(taskButtons.contains(buttonId), () -> "View button leaked into TaskRibbonTask: " + buttonId);
			assertFalse(resourceButtons.contains(buttonId), () -> "View button leaked into ResourceRibbonTask: " + buttonId);
			assertFalse(reportButtons.contains(buttonId), () -> "View button leaked into ReportRibbonTask: " + buttonId);
		}
	}

	@Test
	void preferencesRibbonBandAppearsOnlyInFileRibbonTask() {
		var owners = new ArrayList<String>();
		for (Map.Entry<String, java.util.List<String>> entry : ribbonBandsByTask().entrySet()) {
			if (entry.getValue().contains("PreferencesRibbonBand")) {
				owners.add(entry.getKey());
			}
		}
		assertEquals(java.util.List.of("FileRibbonTask"), owners);
	}

	@Test
	void reportButtonsDoNotRemainInViewRibbonTask() {
		Set<String> reportButtons = ribbonButtonIdsForTask("ReportRibbonTask");
		Set<String> viewButtons = ribbonButtonIdsForTask("ViewRibbonTask");
		assertTrue(reportButtons.contains("RibbonReport"));
		assertTrue(reportButtons.contains("RibbonHistogram"));
		assertTrue(reportButtons.contains("RibbonCharts"));
		assertFalse(viewButtons.contains("RibbonReport"));
		assertFalse(viewButtons.contains("RibbonHistogram"));
		assertFalse(viewButtons.contains("RibbonCharts"));
	}

	@Test
	void formatRibbonIncludesDisplayAndBarBands() {
		assertTrue(ribbonBandIds("FormatRibbonTask").contains("FormatDisplayRibbonBand"));
		assertTrue(ribbonBandIds("FormatRibbonTask").contains("FormatBarRibbonBand"));
		assertTrue(ribbonBandIds("FormatRibbonTask").contains("FormatLayoutRibbonBand"));
		assertEquals(
			java.util.List.of("RibbonToggleProgressLine", "RibbonLabelResourceNames", "RibbonLabelTaskName", "RibbonGridlines"),
			com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatDisplayRibbonBand"));
		assertEquals(
			java.util.List.of("RibbonTimescale", "RibbonBar", "RibbonBarStyles", "RibbonTextStyles"),
			com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatBarRibbonBand"));
	}

	@Test
	void newRibbonButtonsHaveBackingMenuItems() {
		ResourceBundle internal = com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuInternalBundle();
		ResourceBundle labels = menuBundle(Locale.ROOT);
		for (String id : java.util.List.of(
			"ToggleProgressLine",
			"LabelResourceNames",
			"LabelTaskName",
			"InsertRecurring",
			"LevelResources",
			"CalendarOptions",
			"Expand",
			"Collapse",
			"ChooseFilter",
			"ChooseSort",
			"ChooseGroup",
			"Timescale",
			"Gridlines",
			"TextStyles",
			"BarStyles",
			"Layout")) {
			assertTrue(internal.containsKey(id + ".action"), () -> id + " is missing an internal action mapping");
			assertTrue(labels.containsKey(id + ".text"), () -> id + " is missing menu text");
		}
	}

	@Test
	void transformChooserButtonsMapToTheirOwnActions() {
		ResourceBundle internal = com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuInternalBundle();
		assertEquals("ChooseFilter", internal.getString("ChooseFilter.action"));
		assertEquals("ChooseSort", internal.getString("ChooseSort.action"));
		assertEquals("ChooseGroup", internal.getString("ChooseGroup.action"));
	}
}
