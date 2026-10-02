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
import java.awt.Container;
import java.awt.Insets;
import java.awt.Rectangle;
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
import com.microproject.ribbon.CustomRibbonBandGenerator;
import com.microproject.ribbon.RibbonCommandInvocation;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ui.ribbon.SwingRibbonFactory;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.ui.ribbon.RibbonController;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.api.ribbon.RibbonTask;
import org.pushingpixels.flamingo.api.ribbon.JRibbonBand;

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
			((RibbonController) ribbon.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY))
				.setVisibleContextualTabs(Set.of("FormatRibbonTask", "NetworkFormatRibbonTask", "CalendarFormatRibbonTask"));
			assertAttachedButtonsAreVisible(ribbon, MenuManager.STANDARD_RIBBON);
		});
	}

	@Test
	void standardRibbonBuildsAStructuredMsProjectLikeModel() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			SwingRibbonFactory factory = new SwingRibbonFactory(
				new MenuRibbonCommandSource(manager.getToolBarFactory()), ribbonBundles(Locale.getDefault()));
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
			((RibbonController) panel.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY))
				.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			String formatTitle = com.microproject.menu.testsupport.MenuDefinitionSupport
				.menuBundle(Locale.getDefault())
				.getString("FormatRibbonTask.title");
			assertEquals(formatTitle, ribbonTask(panel, "FormatRibbonTask").getTitle());
			assertEquals(formatTitle, ribbonTask(panel, "FormatRibbonTask").getTitle());
			assertNotNull(findLabelByText(panel, "Layout from generator"));
		});
	}

	@Test
	void japaneseRibbonBandsReserveEnoughWidthForBandTitles() throws Exception {
			SwingRibbonFactory factory = new SwingRibbonFactory(
				new MenuRibbonCommandSource(new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.JAPANESE))),
				ribbonBundles(Locale.JAPANESE));
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
			AbstractCommandButton saveButton = firstRibbonButton(manager, "RibbonSaveProject");
			AbstractCommandButton openButton = firstRibbonButton(manager, "RibbonOpenProject");
			assertNotNull(saveButton);
			assertNotNull(openButton);
			assertEquals("RibbonSaveProject", saveButton.getName());
			assertEquals("RibbonOpenProject", openButton.getName());
			assertTrue(host.isVisible());
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
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractCommandButton saveButton = firstRibbonButton(manager, "RibbonSaveProject");
			AbstractCommandButton toggle = firstRibbonButton(manager, "RibbonToggleProgressLine");
			assertNotNull(saveButton);
			assertNotNull(toggle);
			assertFalse(saveButton instanceof JCommandToggleButton);
			assertTrue(toggle instanceof JCommandToggleButton);
		});
	}

	@Test
	void standardRibbonButtonsResolveAgainstLiveActionWiring() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager menuManager = graphicManager.getMenuManager();
			menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

			Set<String> ribbonCommands = new LinkedHashSet<>(ribbonUiButtonIds());
			ribbonCommands.removeIf(id -> id.startsWith("RibbonTopBar"));
			assertButtonsResolveAgainstLiveActionWiring(graphicManager, menuManager, ribbonCommands, "ribbon");
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
			AbstractCommandButton toggle = firstRibbonButton(manager, "RibbonToggleProgressLine");
			assertNotNull(toggle);
			assertTrue(toggle instanceof JCommandToggleButton);
			assertFalse(toggle.getActionModel().isSelected());

			org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
				manager.setActionSelected("ToggleProgressLine", true);
				manager.setActionSelected("Projects", true);
				manager.setActionSelected("Report", true);
				manager.setActionEnabled("Projects", true);
				manager.setActionVisible("Report", true);
			});
			assertTrue(toggle.getActionModel().isSelected());
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
				"FormatRibbonTask",
				"NetworkFormatRibbonTask",
				"CalendarFormatRibbonTask"),
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
			List.of("RibbonArrangeAll", "RibbonDetails", "RibbonPrivacyMask"),
			com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("ViewWindowRibbonBand"));
		assertEquals(
			List.of("RibbonTimescale", "RibbonBarStyles", "RibbonTextStyles"),
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
			"Layout",
			"RibbonPrivacyMask")) {
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
					"RibbonToggleCriticalChain", "ToggleCriticalChain").entrySet()) {
				AbstractCommandButton toggle = firstRibbonButton(manager, entry.getKey());
				assertTrue(toggle instanceof JCommandToggleButton, entry.getKey() + " must expose persistent selection");
				assertFalse(toggle.getActionModel().isSelected());
				manager.setActionSelected(entry.getValue(), true);
				assertTrue(toggle.getActionModel().isSelected());
				assertTrue(toggle.isFocusable());
			}
		});
	}

	@Test
	void contextualFormatTabUsesTheActiveViewCaption() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			RibbonController panel = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			panel.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			panel.setContextualTabTitles(java.util.Map.of("FormatRibbonTask", "Gantt Chart Format"));
			assertEquals("Gantt Chart Format", ribbonTask(host, "FormatRibbonTask").getTitle());
			panel.setContextualTabTitles(java.util.Map.of("FormatRibbonTask", "Tracking Gantt Format"));
			assertEquals("Tracking Gantt Format", ribbonTask(host, "FormatRibbonTask").getTitle());
		});
	}

	@Test
	void ribbonDispatchPreservesCompleteSemanticCommandResult() throws Exception {
		ClickRecordingActionMap actions = new ClickRecordingActionMap();
		RibbonCommandResult expected = new RibbonCommandResult("RibbonHideSelectedTasks",
			RibbonCommandResult.Status.CHANGED, "", List.of(7L), List.of(41L), "task");
		actions.reportResultOnce(expected);
		MenuRibbonCommandSource source = new MenuRibbonCommandSource(
			new ExtToolBarFactory(actions, ribbonBundles(Locale.ROOT)));
		SwingUtilities.invokeAndWait(() -> {
			AbstractButton button = source.createButton("RibbonHideSelectedTasks");
			RibbonCommandResult actual = source.dispatch(new RibbonCommandInvocation("RibbonHideSelectedTasks",
				RibbonCommandInvocation.Origin.RIBBON_BUTTON, this));
			assertEquals(expected, actual);
			assertEquals(List.of(7L), button.getAction().getValue(RibbonCommandResult.SELECTED_TASK_IDS_ACTION_PROPERTY));
			assertEquals(List.of(41L), button.getAction().getValue(RibbonCommandResult.AFFECTED_TASK_IDS_ACTION_PROPERTY));
			assertEquals(expected, button.getAction().getValue(RibbonCommandResult.RESULT_ACTION_PROPERTY));
		});
	}

	@Test
	void ribbonCanCreateOneSharedDispatcherWithoutRegisteringAButton() throws Exception {
		java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		AbstractAction command = new AbstractAction("LinkAction") {
			@Override public void actionPerformed(java.awt.event.ActionEvent event) { calls.incrementAndGet(); }
		};
		ProjectMenuActionMap actions = new ProjectMenuActionMap() {
			@Override public Action getAction(String key) { return command; }
			@Override public String getStringFromAction(Action action) { return "LinkAction"; }
		};
		MenuRibbonCommandSource source = new MenuRibbonCommandSource(
			new ExtToolBarFactory(actions, ribbonBundles(Locale.ROOT)));
		SwingUtilities.invokeAndWait(() -> {
			Action sharedAction = source.createAction("RibbonLink");
			assertTrue(source.getButtons("LinkAction").isEmpty(), "Action lookup must not register a hidden Swing button");
			assertTrue(sharedAction.isEnabled());
			AbstractButton swingButton = source.createButton("RibbonLink");
			assertSame(sharedAction, swingButton.getAction(), "all ribbon surfaces use the shared dispatcher Action");
			actions.getAction("LinkAction").setEnabled(false);
			assertFalse(sharedAction.isEnabled(), "native adapters observe command enablement changes");
			actions.getAction("LinkAction").setEnabled(true);
			assertTrue(sharedAction.isEnabled());
			swingButton.doClick(0);
			sharedAction.actionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED,
				"RibbonLink"));
			assertEquals(2, calls.get(), "both Swing and native-component adapters reach one command");
		});
	}

	@Test
	void buttonLookupReturnsStableSnapshotDuringRibbonRebuild() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.ROOT));
		AbstractButton first = factory.createJButton("RibbonScrollToTask");
		List<AbstractButton> snapshot = factory.getButtonsFromId("RibbonScrollToTask");
		factory.createJButton("RibbonScrollToTask");
		factory.unregisterButton(first);

		assertEquals(1, snapshot.size(), "lookup must be a stable snapshot");
		assertEquals(1, factory.getButtonsFromId("RibbonScrollToTask").size());
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
	void menuItemUsesTheSameDebugActionContract() throws Exception {
		String previous = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		try {
			SwingUtilities.invokeAndWait(() -> {
				com.microproject.menu.resource.ActionMap actionMap = new com.microproject.menu.resource.ActionMap() {
					@Override public Action getAction(String key) { return new AbstractAction() {
						@Override public void actionPerformed(java.awt.event.ActionEvent event) { }
					}; }
					@Override public String getStringFromAction(Action action) { return "LinkAction"; }
				};
				com.microproject.menu.resource.MenuFactory factory =
					new com.microproject.menu.resource.MenuFactory(actionMap, ribbonBundles(Locale.ROOT));
				assertTrue(factory.createJMenuItem("RibbonLink").getAction().getClass().getName()
					.contains("UiButtonDiagnostics"));
			});
		} finally {
			if (previous == null) System.clearProperty("microproject.ui.debug");
			else System.setProperty("microproject.ui.debug", previous);
		}
	}

	@Test
	void everyVisibleRibbonGroupButtonUsesTheCanonicalGraphicManagerCommandRoute() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager manager = graphicManager.getMenuManager();
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			RibbonController ribbon = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			ResourceBundle labels = menuBundle(Locale.getDefault());

			for (String tabId : ribbonTaskIds()) {
				JRibbon nativeRibbon = findRibbon(host);
				assertEquals(labels.getString(tabId + ".title"), ribbonTask(host, tabId).getTitle());
				if (!tabId.contains("Format")) nativeRibbon.setSelectedTask(ribbonTask(host, tabId));
				for (String bandId : ribbonBandIds(tabId)) {
					for (String buttonId : ribbonButtonIds(bandId)) {
						AbstractCommandButton button = firstRibbonButton(manager, buttonId);
						String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
						assertEquals(buttonId, button.getName(), () -> buttonId + " in " + bandId + " has an incorrect command id");
						assertNotNull(graphicManager.getAction(actionId), () -> buttonId + " has no canonical action");
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
			JRibbon ribbon = findRibbon(host);
			var fileTab = ribbonTask(host, "FileRibbonTask");
			var taskTab = ribbonTask(host, "TaskRibbonTask");

			assertEquals(labels.getString("FileRibbonTask.title"), fileTab.getTitle());
			ribbon.setSelectedTask(taskTab);

			assertEquals(taskTab, ribbon.getSelectedTask());
			assertFalse(ribbon.getSelectedTask() == fileTab);
		});
	}

	@Test
	void largeGanttButtonFitsInsideItsRibbonBandContentArea() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			host.setSize(1200, host.getPreferredSize().height);
			layoutRecursively(host);

			// Use the same locale as the ribbon factory.  The unit-test task does not
			// force Japanese (CI uses English), so a fixed Japanese lookup can fail
			// before the layout invariant is even exercised.
			ResourceBundle labels = menuBundle(Locale.getDefault());
			findRibbon(host).setSelectedTask(ribbonTask(host, "ViewRibbonTask"));
			host.setSize(1200, host.getPreferredSize().height);
			layoutRecursively(host);

			AbstractCommandButton gantt = firstRibbonButton(manager, "RibbonGantt");
			Component band = findRibbonBand(gantt);
			Rectangle ganttBounds = SwingUtilities.convertRectangle(gantt.getParent(), gantt.getBounds(), band);
			Insets insets = ((JComponent) band).getInsets();
			Rectangle contentBounds = new Rectangle(
				insets.left,
				insets.top,
				band.getWidth() - insets.left - insets.right,
				band.getHeight() - insets.top - insets.bottom);

			assertTrue(contentBounds.contains(ganttBounds),
				() -> "RibbonGantt is clipped by its band: button=" + ganttBounds + " content=" + contentBounds);
		});
	}

	@Test
	void teamResourcesRibbonButtonTracksItsFilterState() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractCommandButton toggle = firstRibbonButton(manager, "RibbonTeamFilter");
			assertNotNull(toggle);
			assertTrue(toggle instanceof JCommandToggleButton);

			manager.setActionSelected("TeamFilter", true);
			assertTrue(toggle.getActionModel().isSelected());
			manager.setActionSelected("TeamFilter", false);
			assertFalse(toggle.getActionModel().isSelected());
		});
	}

	private static AbstractCommandButton firstRibbonButton(MenuManager manager, String id) {
		List<javax.swing.JComponent> buttons = manager.getRibbonFactory().getRibbonControlsFromId(id);
		assertNotNull(buttons);
		assertFalse(buttons.isEmpty(), "No Flamingo command control registered for " + id);
		return (AbstractCommandButton) buttons.getFirst();
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
			boolean ribbon = "ribbon".equals(context);
			if (ribbon) {
				List<javax.swing.JComponent> controls = menuManager.getRibbonFactory().getRibbonControlsFromId(id);
				assertTrue(controls.stream().anyMatch(control -> control instanceof AbstractCommandButton button
					&& id.equals(button.getName())), () -> id + " has no Flamingo control in the ribbon");
			} else {
				List<?> buttons = menuManager.getToolButtonsFromId(id);
				assertNotNull(buttons, () -> id + " was not registered as a " + context + " button");
				assertTrue(buttons.stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.anyMatch(button -> button.getAction() != null && button.getAction() == action),
					() -> id + " is not wired to its resolved live action for " + context);
			}
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

	private static JRibbon findRibbon(JComponent root) {
		return com.microproject.menu.testsupport.UiComponentWalker.flatten(root).stream()
			.filter(JRibbon.class::isInstance).map(JRibbon.class::cast).findFirst().orElseThrow();
	}

	@Test
	void nativeFlamingoButtonSharesDispatchAndLiveCommandState() throws Exception {
		ClickRecordingActionMap actions = new ClickRecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		java.util.concurrent.atomic.AtomicReference<AbstractCommandButton> control = new java.util.concurrent.atomic.AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			control.set(firstRibbonButton(manager, "RibbonSaveProject"));
			control.get().setSize(control.get().getPreferredSize());
			assertTrue(control.get().isEnabled());
			control.get().doActionClick();
			assertEquals(1, actions.clickCount("SaveProjectAction"), "native click reaches the canonical command once");
			manager.setActionEnabled("SaveProject", false);
			assertFalse(control.get().isEnabled());
			manager.setActionEnabled("SaveProject", true);
			assertTrue(control.get().isEnabled());
			manager.setActionVisible("SaveProject", false);
			assertFalse(control.get().isVisible());
			manager.setActionVisible("SaveProject", true);
			assertTrue(control.get().isVisible());
		});
	}

	private static RibbonTask ribbonTask(JComponent root, String id) {
		String title = menuBundle(Locale.getDefault()).getString(id + ".title");
		JRibbon ribbon = findRibbon(root);
		for (int index = 0; index < ribbon.getTaskCount(); index++) {
			RibbonTask task = ribbon.getTask(index);
			if (title.equals(task.getTitle())) return task;
		}
		for (int group = 0; group < ribbon.getContextualTaskGroupCount(); group++) {
			var contextual = ribbon.getContextualTaskGroup(group);
			for (int index = 0; index < contextual.getTaskCount(); index++) {
				RibbonTask task = contextual.getTask(index);
				if (title.equals(task.getTitle()) || id.contains("Format") && task.getTitle().contains("Format")) return task;
			}
		}
		throw new AssertionError("Ribbon task not found: " + id);
	}

	private static Component findRibbonBand(Component component) {
		for (Component current = component; current != null; current = current.getParent()) {
			if (current instanceof JRibbonBand) {
				return current;
			}
		}
		throw new AssertionError("Ribbon band not found for " + component);
	}

	private static void layoutRecursively(Component component) {
		if (!(component instanceof Container container)) {
			return;
		}
		container.doLayout();
		for (Component child : container.getComponents()) {
			layoutRecursively(child);
		}
	}

	private static JLabel findLabelByText(JComponent root, String text) {
		for (var component : com.microproject.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JLabel label && text.equals(label.getText())) {
				return label;
			}
		}
		throw new AssertionError("Label not found with text: " + text);
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
		private RibbonCommandResult.Status outcomeOnce;
		private RibbonCommandResult resultOnce;

		@Override
		public Action getAction(String key) {
			return actions.computeIfAbsent(key, actionId -> new AbstractAction(actionId) {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent event) {
					clickCounts.merge(actionId, 1, Integer::sum);
					if (outcomeOnce != null) {
						putValue(RibbonCommandResult.STATUS_ACTION_PROPERTY, outcomeOnce);
						outcomeOnce = null;
					}
					if (resultOnce != null) {
						putValue(RibbonCommandResult.RESULT_ACTION_PROPERTY, resultOnce);
						resultOnce = null;
					}
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

		void reportOutcomeOnce(RibbonCommandResult.Status outcome) {
			outcomeOnce = outcome;
		}

		void reportResultOnce(RibbonCommandResult result) {
			resultOnce = result;
		}
	}
}
