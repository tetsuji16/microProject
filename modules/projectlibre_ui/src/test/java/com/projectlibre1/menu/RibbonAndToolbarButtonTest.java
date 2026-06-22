package com.projectlibre1.menu;

import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertAttachedButtonsAreVisible;
import static com.projectlibre1.menu.testsupport.ButtonVisibilityValidator.assertValidSwingButton;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.menuBundle;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBandsByTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIdsForTask;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonBundles;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.stubActionMap;
import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.toolBarButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.awt.BorderLayout;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.junit.jupiter.api.Test;

import com.projectlibre.ui.ribbon.SwingRibbonFactory;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.util.UiLinkTargets;

class RibbonAndToolbarButtonTest {
	@Test
	void standardRibbonButtonsCanBeConstructedInDefaultLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonButtonsCanBeConstructedInJapaneseLocale() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() -> {
			for (String id : ribbonButtonIds()) {
				AbstractButton button = factory.createJButton(id);
				assertValidSwingButton(id, button, true);
			}
		});
	}

	@Test
	void standardRibbonCreatesAttachedVisibleButtons() throws Exception {
		SwingRibbonFactory factory = new SwingRibbonFactory(
			new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.JAPANESE)),
			ribbonBundles(Locale.JAPANESE));
		SwingUtilities.invokeAndWait(() ->
			assertAttachedButtonsAreVisible(factory.createPanel(MenuManager.STANDARD_RIBBON, null), MenuManager.STANDARD_RIBBON));
	}

	@Test
	void topPriorityButtonsBecomeLargeRibbonButtons() throws Exception {
		SwingRibbonFactory factory = new SwingRibbonFactory(
			new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.ROOT)),
			ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			JPanel ribbon = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
			AbstractButton saveButton = findButton(ribbon, "RibbonSaveProject");
			AbstractButton openButton = findButton(ribbon, "RibbonOpenProject");
			assertEquals("large", saveButton.getClientProperty("ProjectLibre.ribbonButtonSize"));
			assertEquals("small", openButton.getClientProperty("ProjectLibre.ribbonButtonSize"));
		});
	}

	@Test
	void standardRibbonPlacesBrandingOnTheRightWithProjectHomeTarget() throws Exception {
		SwingRibbonFactory factory = new SwingRibbonFactory(
			new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.ROOT)),
			ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			JPanel ribbon = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
			ribbon.setSize(1280, 120);
			ribbon.doLayout();
			ribbon.validate();
			JComponent brand = findNamedComponent(ribbon, "projectLibreRibbonBrand", JComponent.class);
			JLabel logo = findNamedComponent(brand, "projectLibreRibbonBrand", JLabel.class);
			JComponent tabRow = (JComponent) ribbon.getComponent(0);
			BorderLayout layout = (BorderLayout) tabRow.getLayout();
			assertEquals(UiLinkTargets.PROJECT_HOME, brand.getClientProperty("ProjectLibre.ribbonBrandTarget"));
			assertEquals(UiLinkTargets.PROJECT_HOME, logo.getClientProperty("ProjectLibre.ribbonBrandTarget"));
			assertTrue(logo.getMouseListeners().length > 0, "Brand logo should expose a click handler");
			assertTrue(logo.getMinimumSize().width >= 144, "Brand logo width should respect attribution minimums");
			assertTrue(logo.getMinimumSize().height >= 31, "Brand logo height should respect attribution minimums");
			assertEquals(brand, layout.getLayoutComponent(BorderLayout.EAST),
				"Brand area should be docked to the right edge of the ribbon header row");
			assertTrue(tabRow.getBorder() instanceof EmptyBorder, "Tab row should not draw a gray separator line");
		});
	}

	@Test
	void ribbonTabsUseUnderlineInsteadOfVerticalSeparators() throws Exception {
		SwingRibbonFactory factory = new SwingRibbonFactory(
			new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.ROOT)),
			ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			JPanel ribbon = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
			JToggleButton fileTab = findToggleButton(ribbon, "File");
			JToggleButton taskTab = findToggleButton(ribbon, "Task");
			assertTrue(fileTab.isSelected(), "The File tab should start selected");
			assertEquals(fileTab.getForeground(), taskTab.getForeground(), "Selected tab text should not change color");
			assertTrue(fileTab.getBorder() instanceof CompoundBorder, "Selected tab should use a compound border");
			CompoundBorder selectedBorder = (CompoundBorder) fileTab.getBorder();
			assertTrue(selectedBorder.getOutsideBorder() instanceof EmptyBorder, "Selected tab should not use a vertical separator");
			assertTrue(selectedBorder.getInsideBorder() instanceof MatteBorder, "Selected tab should render an underline");
			MatteBorder underline = (MatteBorder) selectedBorder.getInsideBorder();
			assertEquals(2, underline.getBorderInsets(fileTab).bottom, "Selected tab underline should be 2px tall");

			assertTrue(taskTab.getBorder() instanceof CompoundBorder, "Unselected tab should also use a compound border");
			CompoundBorder unselectedBorder = (CompoundBorder) taskTab.getBorder();
			assertTrue(unselectedBorder.getOutsideBorder() instanceof EmptyBorder, "Unselected tab should not use a vertical separator");
		});
	}

	@Test
	void standardRibbonButtonsResolveAgainstLiveActionWiring() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			MenuManager menuManager = graphicManager.getMenuManager();
			menuManager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);

			for (String id : ribbonButtonIds()) {
				String actionId = menuManager.getToolBarFactory().getActionStringFromId(id);
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
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
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

	@Test
	void toggleTypeRibbonButtonsAreSwingToggleButtonsAndTrackSelection() throws Exception {
		ExtToolBarFactory factory = new ExtToolBarFactory(stubActionMap(), ribbonBundles(Locale.ROOT));
		SwingUtilities.invokeAndWait(() -> {
			AbstractButton button = factory.createJButton("RibbonToggleProgressLine");
			assertInstanceOf(JToggleButton.class, button, "RibbonToggleProgressLine should be JToggleButton");
			assertFalse(((JToggleButton) button).isSelected());

			MenuManager manager = MenuManager.getInstance(stubActionMap());
			manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			manager.setActionSelected("ToggleProgressLine", true);

			ArrayList buttons = manager.getToolButtonsFromId("ToggleProgressLine");
			assertTrue(buttons != null && !buttons.isEmpty());
			boolean foundToggle = false;
			for (Object candidate : buttons) {
				if (candidate instanceof JToggleButton toggleButton) {
					assertTrue(toggleButton.isSelected());
					foundToggle = true;
				}
			}
			assertTrue(foundToggle, "Should have found the JToggleButton");
		});
	}

	private static AbstractButton findButton(JPanel root, String actionCommand) {
		for (var component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && actionCommand.equals(button.getActionCommand())) {
				return button;
			}
		}
		throw new AssertionError("Button not found: " + actionCommand);
	}

	private static <T> T findNamedComponent(java.awt.Component root, String name, Class<T> type) {
		for (var component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (name.equals(component.getName()) && type.isInstance(component)) {
				return type.cast(component);
			}
		}
		throw new AssertionError("Named component not found: " + name);
	}

	private static JToggleButton findToggleButton(JPanel root, String text) {
		for (var component : com.projectlibre1.menu.testsupport.UiComponentWalker.flatten(root)) {
			if (component instanceof JToggleButton button && text.equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Toggle button not found: " + text);
	}
}
