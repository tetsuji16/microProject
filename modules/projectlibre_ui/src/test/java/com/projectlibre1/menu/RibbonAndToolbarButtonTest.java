package com.projectlibre1.menu;

import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertAttachedButtonsAreVisible;
import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertValidSwingButton;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.displayedRibbonUiButtonIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuBundle;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandsByTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIdsForTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBundles;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.toolBarButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.ArrayList;
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

import com.projectlibre1.menu.MenuActionMapSupport;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.testsupport.UiComponentWalker;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre.ui.ribbon.CustomRibbonBandGenerator;
import com.projectlibre.ui.ribbon.SwingRibbonFactory;
import com.projectlibre.ui.ribbon.SwingRibbonModel;

class RibbonAndToolbarButtonTest {
	@Test
	void standardRibbonButtonsCanBeConstructedInDefaultLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonButtonsCanBeConstructedInJapaneseLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonCreatesAttachedVisibleButtons() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() ->
			assertAttachedButtonsAreVisible(manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null), MenuManager.STANDARD_RIBBON));
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
			String formatTitle = com.projectlibre1.menu.testsupport.MenuDefinitionSupport
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
			AbstractButton saveButton = firstButton(manager.getToolButtonsFromId("RibbonSaveProject"));
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

			assertButtonsResolveAgainstLiveActionWiring(graphicManager, menuManager, ribbonButtonIds(), "ribbon");
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
		Set<String> expected = new LinkedHashSet<>(ribbonButtonIds());
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
				com.projectlibre1.menu.testsupport.MenuDefinitionSupport.hasLocalizedLabel(japaneseBundle, id),
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
			List.of("RibbonToggleProgressLine", "RibbonLabelResourceNames", "RibbonLabelTaskName", "RibbonGridlines"),
			com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatDisplayRibbonBand"));
		assertEquals(
			List.of("RibbonTimescale", "RibbonBar", "RibbonBarStyles", "RibbonTextStyles"),
			com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds("FormatBarRibbonBand"));
	}

	@Test
	void newRibbonButtonsHaveBackingMenuItems() {
		ResourceBundle internal = com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuInternalBundle();
		ResourceBundle labels = menuBundle(Locale.ROOT);
		for (String id : List.of(
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

	@Test
	void toggleTypeRibbonButtonsTrackSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton toggle = firstButton(manager.getToolButtonsFromId("RibbonToggleProgressLine"));
			assertNotNull(toggle);
			assertTrue(toggle instanceof JToggleButton);
			assertFalse(toggle.isSelected());

			manager.setActionSelected("ToggleProgressLine", true);
			assertTrue(toggle.isSelected());
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
		com.projectlibre1.menu.ProjectMenuActionMap actionMap,
		MenuManager menuManager,
		Set<String> buttonIds,
		String context) {
		for (String id : buttonIds) {
			String actionId = menuManager.getToolBarFactory().getActionStringFromId(id);
			assertTrue(actionId != null && !actionId.isBlank(), () -> id + " is missing an action mapping for " + context);
			assertDoesNotThrow(
				() -> actionMap.getAction(actionId),
				() -> id + " does not resolve to a live action for " + context + ": " + actionId);
		}
	}

	private static com.projectlibre1.menu.ProjectMenuActionMap strictActionMap(Set<String> supportedKeys) {
		return new com.projectlibre1.menu.ProjectMenuActionMap() {
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
		for (var component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && text.equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Button not found with text: " + text);
	}

	private static JLabel findLabelByText(JComponent root, String text) {
		for (var component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
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
}
