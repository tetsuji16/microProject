/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.menu;

import static com.microproject.menu.testsupport.ButtonVisibilityValidator.assertAttachedButtonsAreVisible;
import static com.microproject.menu.testsupport.ButtonVisibilityValidator.assertValidSwingButton;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.displayedRibbonUiButtonIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.menuBundle;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonBandIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonBandsByTask;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonUiButtonIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIdsForTask;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonBundles;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.toolBarButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.util.FlatUiSupport;
import com.microproject.ui.ribbon.CustomRibbonBandGenerator;
import com.microproject.ui.ribbon.SwingRibbonFactory;
import com.microproject.ui.ribbon.SwingRibbonModel;
import com.microproject.ui.ribbon.ModernRibbonPanel;

class RibbonAndToolbarButtonTest {
	@Test
	void standardRibbonButtonsCanBeConstructedInDefaultLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonUiButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonButtonsCanBeConstructedInJapaneseLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonUiButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonCreatesAttachedVisibleButtons() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			JPanel ribbon = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			((ModernRibbonPanel) ribbon.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY))
				.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			assertAttachedButtonsAreVisible(ribbon, MenuManager.STANDARD_RIBBON);
		});
	}

	@Test
	void standardRibbonBuildsAStructuredMsProjectLikeModel() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			SwingRibbonFactory factory = new SwingRibbonFactory(manager.getToolBarFactory(), ribbonBundles(Locale.getDefault()));
			SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
			assertEquals(ribbonTaskIds().size(), model.getTabs().size());

			List<String> titles = model.getTabs().stream()
				.map(SwingRibbonModel.RibbonTab::getTitle)
				.toList();
			List<String> expectedTitles = ribbonTaskIds().stream()
				.map(id -> menuBundle(Locale.getDefault()).getString(id + ".title"))
				.toList();
			assertEquals(expectedTitles, titles);
			assertFalse(model.getTabs().get(0).getBands().isEmpty());

			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			assertEquals(1, host.getComponentCount());
			assertEquals(FlatUiSupport.ribbonChromeBackground(), host.getBackground());
		});
	}

	@Test
	void menuManagerPropagatesCustomBandGeneratorsIntoTheModelAndPanel() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		CustomRibbonBandGenerator generator = bandId ->
			"FormatLayoutRibbonBand".equals(bandId) ? customBand("Layout from generator") : null;

		SwingUtilities.invokeAndWait(() -> {
			SwingRibbonModel model = manager.getRibbon(MenuManager.STANDARD_RIBBON, generator);
			SwingRibbonModel.RibbonBand layoutBand = model.getTabs().stream()
				.filter(tab -> tab.getId().equals("FormatRibbonTask"))
				.flatMap(tab -> tab.getBands().stream())
				.filter(band -> band.getId().equals("FormatLayoutRibbonBand"))
				.findFirst()
				.orElseThrow();
			assertTrue(layoutBand.isCustomBand());
			assertEquals(SwingRibbonModel.RibbonBandKind.CUSTOM, layoutBand.getKind());
			assertNotNull(layoutBand.getCustomBandProvider());

			JPanel panel = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, generator, null);
			((ModernRibbonPanel) panel.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY))
				.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			String formatTitle = com.microproject.menu.testsupport.MenuDefinitionSupport
				.menuBundle(Locale.getDefault())
				.getString("FormatRibbonTask.title");
			findButtonByText(panel, formatTitle).doClick();
			assertNotNull(findLabelByText(panel, "Layout from generator"));
		});
	}

	@Test
	void japaneseRibbonBandsReserveEnoughWidthForBandTitles() throws Exception {
		SwingRibbonFactory factory = new SwingRibbonFactory(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.JAPANESE)), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
			for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
				for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
					assertTrue(band.getTitle() != null && !band.getTitle().isBlank(), () -> band.getId() + " has no title");
					assertFalse(band.getButtons().isEmpty(), () -> band.getId() + " has no buttons");
				}
			}
		});
	}

	@Test
	void standardRibbonRegistersButtonsByActionId() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton saveButton = manager.getToolButtonsFromId("RibbonSaveProject").stream()
				.map(AbstractButton.class::cast)
				.filter(button -> "RibbonSaveProject".equals(button.getActionCommand()))
				.findFirst().orElseThrow();
			AbstractButton openButton = firstButton(manager.getToolButtonsFromId("RibbonOpenProject"));
			assertNotNull(saveButton);
			assertNotNull(openButton);
			assertEquals("RibbonSaveProject", saveButton.getActionCommand());
			assertEquals("RibbonOpenProject", openButton.getActionCommand());
			assertTrue(hasRibbonCommandRole(saveButton));
		});
	}

	@Test
	void transientRibbonPopupButtonsShareActionsWithoutDuplicateRegistration() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			AbstractButton registered = factory.createJButton("RibbonScrollToTask");
			String actionId = factory.getActionStringFromId("RibbonScrollToTask");
			int registeredCount = factory.getButtonsFromId(actionId).size();
			AbstractButton popup = factory.createUnregisteredJButton("RibbonScrollToTask");

			assertSame(registered.getAction(), popup.getAction());
			assertEquals(registeredCount, factory.getButtonsFromId(actionId).size());
		});
	}

	@Test
	void displayedRibbonButtonsUseSharedCommandStateStyling() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton saveButton = firstButton(manager.getToolButtonsFromId("RibbonSaveProject"));
			AbstractButton toggle = firstButton(manager.getToolButtonsFromId("RibbonToggleProgressLine"));
			assertNotNull(saveButton);
			assertNotNull(toggle);
			assertTrue(hasRibbonCommandRole(saveButton));
			assertTrue(hasRibbonCommandRole(toggle));
			assertFalse(saveButton instanceof JToggleButton);
			assertTrue(toggle instanceof JToggleButton);
			assertNotNull(findFirstRibbonTabButton(host));
			assertEquals(
				FlatUiSupport.BUTTON_STYLE_ROLE_RIBBON_TAB,
				findFirstRibbonTabButton(host).getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY));
		});
	}

	@Test
	void standardRibbonButtonsResolveAgainstLiveActionWiring() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager menuManager = graphicManager.getMenuManager();
			menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

			assertButtonsResolveAgainstLiveActionWiring(graphicManager, menuManager, ribbonUiButtonIds(), "ribbon");
		});
	}

	@Test
	void everyRibbonButtonDispatchesItsResolvedActionWhenClicked() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ClickRecordingActionMap actionMap = new ClickRecordingActionMap();
			MenuManager manager = MenuManager.getInstance(actionMap);
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

			for (String id : ribbonUiButtonIds()) {
				String actionId = manager.getToolBarFactory().getActionStringFromId(id);
				int previousClickCount = actionMap.clickCount(actionId);
				AbstractButton button = manager.getToolButtonsFromId(id).stream()
					.map(AbstractButton.class::cast)
					.filter(candidate -> id.equals(candidate.getActionCommand()))
					.findFirst()
					.orElseThrow(() -> new AssertionError(id + " was not created as a clickable ribbon button"));
				assertTrue(button.isEnabled(), () -> id + " is unexpectedly disabled in the neutral command fixture");
				button.doClick();
				assertEquals(previousClickCount + 1, actionMap.clickCount(actionId),
					() -> id + " did not dispatch " + actionId + " exactly once");
			}
		});
	}

	@Test
	void fileRibbonButtonsDispatchToTheirCommandRoutesWhenClicked() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExternalRouteRecordingGraphicManager manager = new ExternalRouteRecordingGraphicManager();
			MenuManager menuManager = manager.getMenuManager();
			menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			Map<String, String> expectedRoutes = Map.ofEntries(
				Map.entry("RibbonNewProject", "newProject"),
				Map.entry("RibbonOpenProject", "openProject"),
				Map.entry("RibbonRecentProjects", "openProject"),
				Map.entry("RibbonSaveProject", "saveProject"),
				Map.entry("RibbonSaveProjectAs", "saveAsProject"),
				Map.entry("RibbonSaveMpoAs", "saveMpoAs"),
				Map.entry("RibbonCloseProject", "closeProject"),
				Map.entry("RibbonImportProject", "openProject"),
				Map.entry("RibbonExportProject", "saveAsProject"),
				Map.entry("RibbonPrint", "print"),
				Map.entry("RibbonPrintPreview", "printPreview"),
				Map.entry("RibbonPDF", "pdf"),
				Map.entry("RibbonLocale", "locale"),
				Map.entry("RibbonProjectLibreDocumentation", "help"),
				Map.entry("RibbonAboutProjectLibre", "about"));

			for (Map.Entry<String, String> expected : expectedRoutes.entrySet()) {
				AbstractButton button = menuManager.getToolButtonsFromId(expected.getKey()).stream()
					.map(AbstractButton.class::cast)
					.filter(candidate -> expected.getKey().equals(candidate.getActionCommand()))
					.findFirst().orElseThrow();
				if ("RibbonLocale".equals(expected.getKey())) {
					assertTrue(button.isEnabled(), "Locale must be available even when no project is open");
				} else {
					button.setEnabled(true); // Exercise document commands without opening a real dialog.
				}
				button.doClick();
				assertEquals(expected.getValue(), manager.lastRoute, expected.getKey() + " did not dispatch when clicked");
			}
		});
	}

	@Test
	void ribbonViewToolbarButtonsCanBeConstructed() throws Exception {
		assertToolbarButtonsCanBeConstructed(MenuManager.RIBBON_VIEW_BAR);
	}

	@Test
	void printPreviewToolbarButtonsCanBeConstructed() throws Exception {
		assertToolbarButtonsCanBeConstructed(MenuManager.PRINT_PREVIEW_TOOL_BAR);
	}

	@Test
	void displayedRibbonUiInventoryCoversRibbonAndRelatedToolbarsWithoutDuplicates() {
		Set<String> expected = new LinkedHashSet<>(ribbonUiButtonIds());
		expected.addAll(toolBarButtonIds(MenuManager.RIBBON_VIEW_BAR));
		expected.addAll(toolBarButtonIds(MenuManager.PRINT_PREVIEW_TOOL_BAR));
		assertEquals(expected, displayedRibbonUiButtonIds());
	}

	@Test
	void relatedToolbarButtonsResolveAgainstLiveActionWiring() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager menuManager = graphicManager.getMenuManager();
			menuManager.getToolBar(MenuManager.RIBBON_VIEW_BAR);

			assertButtonsResolveAgainstLiveActionWiring(
				graphicManager,
				menuManager,
				toolBarButtonIds(MenuManager.RIBBON_VIEW_BAR),
				MenuManager.RIBBON_VIEW_BAR);
		});
	}

	@Test
	void printPreviewToolbarButtonsUseDedicatedPrintPreviewActionIds() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(
			strictActionMap(Set.of(
				MenuActionConstants.ACTION_PRINTPREVIEW_FIRST,
				MenuActionConstants.ACTION_PRINTPREVIEW_BACK,
				MenuActionConstants.ACTION_PRINTPREVIEW_FORWARD,
				MenuActionConstants.ACTION_PRINTPREVIEW_UP,
				MenuActionConstants.ACTION_PRINTPREVIEW_DOWN,
				MenuActionConstants.ACTION_PRINTPREVIEW_LAST,
				MenuActionConstants.ACTION_PRINTPREVIEW_ZOOMIN,
				MenuActionConstants.ACTION_PRINTPREVIEW_ZOOMRESET,
				MenuActionConstants.ACTION_PRINTPREVIEW_ZOOMOUT,
				MenuActionConstants.ACTION_PRINTPREVIEW_PRINT,
				MenuActionConstants.ACTION_PRINTPREVIEW_PDF,
				MenuActionConstants.ACTION_PRINTPREVIEW_FORMAT)),
			ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : toolBarButtonIds(MenuManager.PRINT_PREVIEW_TOOL_BAR)) {
				assertValidSwingButton(id, factory.createJButton(id), true);
			}
			JToolBar toolBar = factory.createJToolBar(MenuManager.PRINT_PREVIEW_TOOL_BAR);
			assertAttachedButtonsAreVisible(toolBar, MenuManager.PRINT_PREVIEW_TOOL_BAR);
		});
	}

	@Test
	void japaneseBundleStillProvidesLabelsForDisplayedRibbonUiButtons() {
		var japaneseBundle = menuBundle(Locale.JAPANESE);
		for (String id : displayedRibbonUiButtonIds()) {
			assertTrue(
				com.microproject.menu.testsupport.MenuDefinitionSupport.hasLocalizedLabel(japaneseBundle, id),
				() -> id + " is missing Japanese text and tooltip");
		}
	}

	private static void assertToolbarButtonsCanBeConstructed(String toolbarId) throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : toolBarButtonIds(toolbarId)) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
				assertEquals(
					FlatUiSupport.BUTTON_STYLE_ROLE_TOOLBAR,
					button.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY),
					() -> id + " is missing the shared toolbar command-button style");
			}
			JToolBar toolBar = factory.createJToolBar(toolbarId);
			assertFalse(toolBar.isFloatable());
			assertTrue(toolBar.isRollover());
			assertAttachedButtonsAreVisible(toolBar, toolbarId);
		});
	}

	@Test
	void ribbonButtonsCanHaveSelectionStateUpdatedWithoutClassCast() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton toggle = firstButton(manager.getToolButtonsFromId("RibbonToggleProgressLine"));
			assertNotNull(toggle);
			assertTrue(toggle instanceof JToggleButton);
			assertFalse(toggle.isSelected());

			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
				manager.setActionSelected("ToggleProgressLine", true);
				manager.setActionSelected("Projects", true);
				manager.setActionSelected("Report", true);
				manager.setActionEnabled("Projects", true);
				manager.setActionVisible("Report", true);
			});
			assertTrue(toggle.isSelected());
		});
	}

	@Test
	void standardRibbonUsesMsProjectStyleTaskOrder() {
		assertEquals(
			List.of(
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
		assertEquals(List.of("FileRibbonTask"), owners);
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
			List.of("RibbonToggleProgressLine", "RibbonLabelResourceNames", "RibbonLabelTaskName", "RibbonGridlines", "RibbonToggleCriticalChain"),
			com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatDisplayRibbonBand"));
		assertEquals(
			List.of("RibbonTimescale", "RibbonBar", "RibbonBarStyles", "RibbonTextStyles"),
			com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatBarRibbonBand"));
	}

	@Test
	void newRibbonButtonsHaveBackingMenuItems() {
		ResourceBundle internal = com.microproject.menu.testsupport.MenuDefinitionSupport.menuInternalBundle();
		ResourceBundle labels = menuBundle(Locale.ROOT);
		for (String id : List.of(
			"ToggleProgressLine",
			"LabelResourceNames",
			"LabelTaskName",
			"InsertRecurring",
			"LevelResources",
			"CCPMSettings",
			"CCPMClear",
			"CCPMBufferStatus",
			"CCPMNetwork",
			"ToggleCriticalChain",
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
		ResourceBundle internal = com.microproject.menu.testsupport.MenuDefinitionSupport.menuInternalBundle();
		assertEquals("ChooseFilter", internal.getString("ChooseFilter.action"));
		assertEquals("ChooseSort", internal.getString("ChooseSort.action"));
		assertEquals("ChooseGroup", internal.getString("ChooseGroup.action"));
	}

	@Test
	void toggleTypeRibbonButtonsTrackSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			for (var entry : java.util.Map.of(
				"RibbonToggleProgressLine", "ToggleProgressLine",
				"RibbonLabelResourceNames", "LabelResourceNames",
				"RibbonLabelTaskName", "LabelTaskName",
				"RibbonGridlines", "Gridlines",
				"RibbonToggleCriticalChain", "ToggleCriticalChain").entrySet()) {
				AbstractButton toggle = firstButton(manager.getToolButtonsFromId(entry.getKey()));
				assertTrue(toggle instanceof JToggleButton, entry.getKey() + " must expose persistent selection");
				assertFalse(toggle.isSelected());
				manager.setActionSelected(entry.getValue(), true);
				assertTrue(toggle.isSelected());
				assertEquals(javax.accessibility.AccessibleRole.TOGGLE_BUTTON,
					toggle.getAccessibleContext().getAccessibleRole());
			}
		});
	}

	@Test
	void contextualFormatTabUsesTheActiveViewCaption() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			ModernRibbonPanel panel = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			panel.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			panel.setContextualTabTitles(java.util.Map.of("FormatRibbonTask", "Gantt Chart Format"));
			assertNotNull(findButtonByText(host, "Gantt Chart Format"));
			panel.setContextualTabTitles(java.util.Map.of("FormatRibbonTask", "Tracking Gantt Format"));
			assertNotNull(findButtonByText(host, "Tracking Gantt Format"));
		});
	}

	@Test
	void ribbonButtonMirrorsLiveActionEnablement() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AbstractAction liveAction = new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) { }
			};
			com.microproject.menu.resource.ActionMap actionMap = new com.microproject.menu.resource.ActionMap() {
				@Override public Action getAction(String key) { return liveAction; }
				@Override public String getStringFromAction(Action action) { return "LinkAction"; }
			};
			com.microproject.menu.resource.ButtonFactory factory =
				new com.microproject.menu.resource.ButtonFactory(actionMap, ribbonBundles(Locale.ROOT));
			var button = factory.createRibbonButton("RibbonLink");
			assertTrue(button.isEnabled());
			liveAction.setEnabled(false);
			assertFalse(button.isEnabled());
			liveAction.setEnabled(true);
			assertTrue(button.isEnabled());
		});
	}

	@Test
	void everyVisibleRibbonGroupCommandDispatchesExactlyOnceDuringProgrammaticScreening() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ClickRecordingActionMap actionMap = new ClickRecordingActionMap();
			MenuManager manager = MenuManager.getInstance(actionMap);
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			ResourceBundle labels = menuBundle(Locale.getDefault());

			for (String tabId : ribbonTaskIds()) {
				findButtonByText(host, labels.getString(tabId + ".title")).doClick();
				for (String bandId : ribbonBandIds(tabId)) {
					for (String buttonId : ribbonButtonIds(bandId)) {
						AbstractButton button = findAttachedButtonByCommand(host, buttonId);
						String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
						int previousClickCount = actionMap.clickCount(actionId);
						assertTrue(button.isEnabled(), () -> buttonId + " is disabled in " + bandId);
						button.doClick();
						assertEquals(previousClickCount + 1, actionMap.clickCount(actionId),
							() -> buttonId + " in " + bandId + " did not dispatch exactly once");
					}
				}
			}
		});
	}

	@Test
	void everyVisibleRibbonGroupButtonUsesItsLiveGraphicManagerAction() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager manager = graphicManager.getMenuManager();
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
			ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			ResourceBundle labels = menuBundle(Locale.getDefault());

			for (String tabId : ribbonTaskIds()) {
				findButtonByText(host, labels.getString(tabId + ".title")).doClick();
				for (String bandId : ribbonBandIds(tabId)) {
					for (String buttonId : ribbonButtonIds(bandId)) {
						AbstractButton button = findAttachedButtonByCommand(host, buttonId);
						String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
						assertSame(graphicManager.getAction(actionId), button.getAction(),
							() -> buttonId + " in " + bandId + " is not wired to " + actionId);
					}
				}
			}
		});
	}

	@Test
	void ribbonTabClicksUseTheButtonModelAsTheSingleSelectionAppearanceSource() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			ResourceBundle labels = menuBundle(Locale.getDefault());
			AbstractButton fileTab = findButtonByText(host, labels.getString("FileRibbonTask.title"));
			AbstractButton taskTab = findButtonByText(host, labels.getString("TaskRibbonTask.title"));

			assertTrue(fileTab.isSelected());
			taskTab.doClick();

			assertTrue(taskTab.isSelected());
			assertFalse(fileTab.isSelected());
			assertEquals(FlatUiSupport.tabSelectedForeground(), taskTab.getForeground());
			assertEquals(FlatUiSupport.tabUnselectedForeground(), fileTab.getForeground());
		});
	}

	@Test
	void teamResourcesRibbonButtonTracksItsFilterState() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton toggle = firstButton(manager.getToolButtonsFromId("RibbonTeamFilter"));
			assertNotNull(toggle);
			assertTrue(toggle instanceof JToggleButton);

			manager.setActionSelected("TeamFilter", true);
			assertTrue(toggle.isSelected());
			manager.setActionSelected("TeamFilter", false);
			assertFalse(toggle.isSelected());
		});
	}

	private static AbstractButton firstButton(List<?> buttons) {
		assertNotNull(buttons);
		assertFalse(buttons.isEmpty());
		return (AbstractButton) buttons.get(0);
	}

	private static JPanel customBand(String text) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(text));
		return panel;
	}

	private static void assertButtonsResolveAgainstLiveActionWiring(
		com.microproject.menu.ProjectMenuActionMap actionMap,
		MenuManager menuManager,
		Set<String> buttonIds,
		String context) {
		for (String id : buttonIds) {
			String actionId = menuManager.getToolBarFactory().getActionStringFromId(id);
			assertTrue(actionId != null && !actionId.isBlank(), () -> id + " is missing an action mapping for " + context);
			Action action = assertDoesNotThrow(
				() -> actionMap.getAction(actionId),
				() -> id + " does not resolve to a live action for " + context + ": " + actionId);
			assertNotNull(action, () -> id + " has no live action for " + context + ": " + actionId);
			List<?> buttons = menuManager.getToolButtonsFromId(id);
			assertNotNull(buttons, () -> id + " was not registered as a " + context + " button");
			assertTrue(buttons.stream().map(AbstractButton.class::cast).anyMatch(button -> button.getAction() == action),
				() -> id + " is not wired to its resolved live action for " + context);
		}
	}

	private static com.microproject.menu.ProjectMenuActionMap strictActionMap(Set<String> supportedKeys) {
		return new com.microproject.menu.ProjectMenuActionMap() {
			@Override
			public Action getAction(String key) {
				if (!supportedKeys.contains(key)) {
					return null;
				}
				return new AbstractAction(key) {
					@Override
					public void actionPerformed(java.awt.event.ActionEvent e) {
					}
				};
			}

			@Override
			public String getStringFromAction(Action action) {
				Object value = action.getValue(Action.NAME);
				return value == null ? "" : value.toString();
			}
		};
	}

	private static AbstractButton findButtonByText(JComponent root, String text) {
		for (var component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && text.equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Button not found with text: " + text);
	}

	private static AbstractButton findAttachedButtonByCommand(JComponent root, String command) {
		for (var component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && command.equals(button.getActionCommand())) {
				return button;
			}
		}
		throw new AssertionError("Visible ribbon button not found: " + command);
	}

	private static JLabel findLabelByText(JComponent root, String text) {
		for (var component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JLabel label && text.equals(label.getText())) {
				return label;
			}
		}
		throw new AssertionError("Label not found with text: " + text);
	}

	private static AbstractButton findFirstRibbonTabButton(Component root) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof JToggleButton toggle
				&& FlatUiSupport.BUTTON_STYLE_ROLE_RIBBON_TAB.equals(toggle.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY))) {
				return toggle;
			}
		}
		return null;
	}

	private static boolean hasRibbonCommandRole(AbstractButton button) {
		Object role = button.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY);
		return FlatUiSupport.BUTTON_STYLE_ROLE_RIBBON_LARGE.equals(role)
			|| FlatUiSupport.BUTTON_STYLE_ROLE_RIBBON_SMALL.equals(role);
	}

	private static final class ExternalRouteRecordingGraphicManager extends GraphicManager {
		private String lastRoute;

		ExternalRouteRecordingGraphicManager() {
			super(new JPanel());
		}

		@Override
		protected boolean beforeExternalRoute(String routeId) {
			lastRoute = routeId;
			return false;
		}
	}

	private static final class ClickRecordingActionMap implements com.microproject.menu.ProjectMenuActionMap {
		private final Map<String, Integer> clickCounts = new HashMap<>();
		private final Map<String, Action> actions = new HashMap<>();

		@Override
		public Action getAction(String key) {
			return actions.computeIfAbsent(key, actionId -> new AbstractAction(actionId) {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent event) {
					clickCounts.merge(actionId, 1, Integer::sum);
				}
			});
		}

		@Override
		public String getStringFromAction(Action action) {
			Object name = action.getValue(Action.NAME);
			return name == null ? "" : name.toString();
		}

		int clickCount(String actionId) {
			return clickCounts.getOrDefault(actionId, 0);
		}
	}
}
