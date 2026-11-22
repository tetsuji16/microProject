/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.preference.GlobalPreferences;
import com.microproject.testsupport.DialogScreenshotArtifacts;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiEnvironment;
import com.microproject.ui.ribbon.ModernRibbonPanel;
import com.microproject.ui.ribbon.RibbonCustomization;
import com.microproject.ui.ribbon.RibbonCustomizationCatalog;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.ui.shell.MicroProjectShell;
import com.microproject.util.Environment;

/** Physical coverage of the Customize Ribbon and Quick Access Toolbar categories of the preferences dialog. */
class PreferencesCustomizeGuiAcceptanceTest {
	private static final String CUSTOMIZATION_KEY = "ribbonCustomization";
	private static final String QUICK_ACCESS_KEY = "ribbonQuickAccessCommands";
	/** The dialog width, in dp, that the evidence screenshots use for the narrow layout. */
	private static final int NARROW_DIALOG_WIDTH = 560;
	private static final String QUICK_ACCESS_PANEL_NAME = "officeChromeQuickAccessCommands";

	private String originalCustomization;
	private String originalQuickAccess;
	private boolean originalRibbonUi;
	private boolean originalNewLook;
	private MainRibbonFrame ribbonFrame;
	private GlobalPreferences preferences;
	private Robot robot;

	@BeforeEach
	void prepare() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		java.util.prefs.Preferences node = node();
		originalCustomization = node.get(CUSTOMIZATION_KEY, null);
		originalQuickAccess = node.get(QUICK_ACCESS_KEY, null);
		originalRibbonUi = Environment.isRibbonUI();
		originalNewLook = Environment.isNewLook();
		RibbonGuiEnvironment.initialize();
		RibbonDisplayPreferences.saveRibbonCustomization(RibbonCustomization.none());
		RibbonDisplayPreferences.resetQuickAccessCommands();
		preferences = new GlobalPreferences();
		robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
	}

	@AfterEach
	void restore() throws Exception {
		if (robot == null) return;
		// A test must not leave input state behind: release the modifiers and the space key, close popups,
		// and dispose every window it opened, so the next test starts with a clean desktop.
		for (int key : new int[] { KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_SPACE })
			robot.keyRelease(key);
		SwingUtilities.invokeAndWait(() -> {
			javax.swing.MenuSelectionManager.defaultManager().clearSelectedPath();
			for (Window window : Window.getWindows())
				if (window instanceof PreferencesDialogBox) window.dispose();
			if (ribbonFrame != null) ribbonFrame.dispose();
			for (Window window : Window.getWindows())
				if (window.isShowing()) window.dispose();
		});
		java.util.prefs.Preferences node = node();
		if (originalCustomization == null) node.remove(CUSTOMIZATION_KEY);
		else node.put(CUSTOMIZATION_KEY, originalCustomization);
		if (originalQuickAccess == null) node.remove(QUICK_ACCESS_KEY);
		else node.put(QUICK_ACCESS_KEY, originalQuickAccess);
		Environment.setRibbonUI(originalRibbonUi);
		Environment.setNewLook(originalNewLook);
	}

	@Test
	void createRenameAndApplyShowsTheNewTabGroupAndCommandOnTheOpenRibbon() throws Exception {
		openRibbonFrame();
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		JTree tree = awaitShowing("ribbonCustomizeTree", JTree.class);
		JList<?> commands = awaitShowing("ribbonCustomizeCommands", JList.class);
		AbstractButton newTab = button("ribbonCustomizeNewTab");
		AbstractButton newGroup = button("ribbonCustomizeNewGroup");
		AbstractButton rename = button("ribbonCustomizeRename");
		AbstractButton remove = button("ribbonCustomizeRemove");
		AbstractButton add = button("ribbonCustomizeAdd");

		// The File tab, default groups and default commands cannot be edited.
		clickRow(tree, RibbonCustomizationCatalog.tabTitle(RibbonCustomization.FILE_TAB_ID), 2);
		clickListCommand(commands, "RibbonFind");
		assertDisabled("File tab", remove, rename, newGroup, add);
		// Space only reaches the tree's binding when the tree owns the focus, so select the File row again first.
		String fileTitle = RibbonCustomizationCatalog.tabTitle(RibbonCustomization.FILE_TAB_ID);
		clickRow(tree, fileTitle, 2);
		GuiAcceptanceSupport.await(tree::hasFocus, "the ribbon tree did not take the focus");
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.waitForIdle();
		assertTrue(fileRowChecked(tree, fileTitle), "Space must not hide the File tab");
		String taskTab = "TaskRibbonTask";
		RibbonCustomizationCatalog.DefaultGroup defaultGroup = RibbonCustomizationCatalog.defaultGroups(taskTab).stream()
			.filter(group -> !group.commandIds().isEmpty()).findFirst().orElseThrow();
		expandRow(tree, RibbonCustomizationCatalog.tabTitle(taskTab), 2);
		clickRow(tree, defaultGroup.title(), 3);
		assertDisabled("default group", remove, rename, add);
		expandRow(tree, defaultGroup.title(), 3);
		clickRow(tree, RibbonCustomizationCatalog.commandName(defaultGroup.commandIds().get(0)), 4);
		assertDisabled("default command", remove, rename, add);

		// A new tab starts with one group; both are renamed, and a command is added to the group.
		click(newTab);
		String newTabLabel = UsabilityStrings.text("ribbonCustomize.newTabName");
		String newGroupLabel = UsabilityStrings.text("ribbonCustomize.newGroupName");
		awaitSelectedLabel(tree, newTabLabel);
		click(rename);
		answerInput("Mine Tab");
		awaitSelectedLabel(tree, "Mine Tab");
		expandRow(tree, "Mine Tab", 2);
		clickRow(tree, newGroupLabel, 3);
		click(rename);
		answerInput("Mine Group");
		awaitSelectedLabel(tree, "Mine Group");
		clickListCommand(commands, "RibbonFind");
		GuiAcceptanceSupport.await(add::isEnabled, "Add did not become enabled for a custom group");
		click(add);
		awaitSelectedLabel(tree, RibbonCustomizationCatalog.commandName("RibbonFind"));
		assertEquals(RibbonCustomization.none(), RibbonDisplayPreferences.loadRibbonCustomization(),
			"nothing may be stored before Apply");

		click(findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
		RibbonCustomization saved = RibbonDisplayPreferences.loadRibbonCustomization();
		assertEquals(1, saved.customTabs().size());
		RibbonCustomization.CustomTab tab = saved.customTabs().get(0);
		assertEquals("Mine Tab", tab.name());
		assertEquals("Mine Group", tab.groups().get(0).name());
		assertEquals(List.of("RibbonFind"), tab.groups().get(0).commandIds());
		assertFalse(saved.hiddenDefaultTabs().contains(RibbonCustomization.FILE_TAB_ID), "File must stay visible");
		GuiAcceptanceSupport.await(() -> hasRibbonTab(tab.id()), "the open ribbon did not show the applied tab");
		assertEquals("Mine Tab", ribbonTab(tab.id()).getText());
	}

	@Test
	void cancelLeavesTheRibbonAndTheQuickAccessToolbarUnchanged() throws Exception {
		openRibbonFrame();
		String customizationBefore = node().get(CUSTOMIZATION_KEY, null);
		String quickAccessBefore = node().get(QUICK_ACCESS_KEY, null);
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		click(awaitShowing("ribbonCustomizeNewTab", AbstractButton.class));
		awaitSelectedLabel(awaitShowing("ribbonCustomizeTree", JTree.class),
			UsabilityStrings.text("ribbonCustomize.newTabName"));
		selectCategory(dialog, "preferences.quickAccess");
		JList<?> available = awaitShowing("quickAccessAvailable", JList.class);
		clickListCommand(available, "RibbonFind");
		click(button("quickAccessAdd"));
		click(findButton(dialog, UsabilityStrings.text("preferences.cancel")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Cancel");
		assertEquals(customizationBefore, node().get(CUSTOMIZATION_KEY, null), "Cancel must not store the ribbon");
		assertEquals(quickAccessBefore, node().get(QUICK_ACCESS_KEY, null), "Cancel must not store the toolbar");
		assertEquals(RibbonCustomization.none(), RibbonDisplayPreferences.loadRibbonCustomization());
		assertEquals(RibbonDisplayPreferences.defaultQuickAccessCommands(), RibbonDisplayPreferences.loadQuickAccessCommands());
		assertEquals(0, customTabCount(), "Cancel must not add a tab to the open ribbon");
	}

	@Test
	void resetSelectedTabAndResetAllRestoreOnlyTheirOwnScope() throws Exception {
		RibbonCustomization.CustomTab first = new RibbonCustomization.CustomTab("gui-tab-a", "Tab A", List.of(
			new RibbonCustomization.CustomGroup("gui-group-a", "Group A", List.of("RibbonFind"))));
		RibbonCustomization.CustomTab second = new RibbonCustomization.CustomTab("gui-tab-b", "Tab B", List.of(
			new RibbonCustomization.CustomGroup("gui-group-b", "Group B", List.of("RibbonFind"))));
		RibbonDisplayPreferences.saveRibbonCustomization(new RibbonCustomization(List.of(), Set.of("HelpRibbonTask"),
			List.of(first, second), java.util.Map.of()));
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		JTree tree = awaitShowing("ribbonCustomizeTree", JTree.class);

		// Reset of the selected tab: Cancel changes nothing, OK removes only that tab.
		clickRow(tree, "Tab A", 2);
		click(button("ribbonCustomizeReset"));
		click(menuItem("ribbonCustomizeResetTab"));
		answerConfirm(false);
		assertTrue(tabLabels(tree).containsAll(List.of("Tab A", "Tab B")), "Cancel must keep both tabs");
		click(button("ribbonCustomizeReset"));
		click(menuItem("ribbonCustomizeResetTab"));
		answerConfirm(true);
		GuiAcceptanceSupport.await(() -> !tabLabels(tree).contains("Tab A"), "Reset tab did not remove the selected tab");
		assertTrue(tabLabels(tree).contains("Tab B"), "Reset tab must not touch other tabs");
		click(findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
		RibbonCustomization afterTab = RibbonDisplayPreferences.loadRibbonCustomization();
		assertEquals(List.of("gui-tab-b"), afterTab.customTabs().stream().map(RibbonCustomization.CustomTab::id).toList());
		assertTrue(afterTab.hiddenDefaultTabs().contains("HelpRibbonTask"), "Reset tab must keep another tab's hiding");

		// Reset all: Cancel changes nothing, OK restores the default ribbon.
		openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		JTree again = awaitShowing("ribbonCustomizeTree", JTree.class);
		click(button("ribbonCustomizeReset"));
		click(menuItem("ribbonCustomizeResetAll"));
		answerConfirm(false);
		assertTrue(tabLabels(again).contains("Tab B"), "Cancel on Reset all must keep the custom tab");
		click(button("ribbonCustomizeReset"));
		click(menuItem("ribbonCustomizeResetAll"));
		answerConfirm(true);
		GuiAcceptanceSupport.await(() -> !tabLabels(again).contains("Tab B"), "Reset all did not remove the custom tab");
		click(findButton(findDialog(), UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
		assertEquals(RibbonCustomization.none(), RibbonDisplayPreferences.loadRibbonCustomization(),
			"Reset all must restore the default ribbon");
	}

	@Test
	void quickAccessReorderAndApplyShowTheNewOrderOnTheToolbar() throws Exception {
		openRibbonFrame();
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_QUICK_ACCESS);
		JList<?> chosen = awaitShowing("quickAccessChosen", JList.class);
		JList<?> available = awaitShowing("quickAccessAvailable", JList.class);
		List<String> defaults = RibbonDisplayPreferences.defaultQuickAccessCommands();
		assertEquals(defaults, chosenIds(chosen));
		clickListCommand(available, "RibbonFind");
		click(button("quickAccessAdd"));
		GuiAcceptanceSupport.await(() -> chosenIds(chosen).contains("RibbonFind"), "Add did not append the command");
		clickListCommand(chosen, "RibbonFind");
		click(button("quickAccessUp"));
		click(button("quickAccessUp"));
		List<String> expected = List.of(defaults.get(0), "RibbonFind", defaults.get(1), defaults.get(2));
		GuiAcceptanceSupport.await(() -> chosenIds(chosen).equals(expected), "Up did not reorder: " + chosenIds(chosen));

		// Reset asks first; Cancel keeps the order.
		click(button("quickAccessReset"));
		answerConfirm(false);
		assertEquals(expected, chosenIds(chosen), "Cancel on Reset must keep the working order");
		assertEquals(defaults, RibbonDisplayPreferences.loadQuickAccessCommands(), "nothing may be stored before Apply");

		click(findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
		assertEquals(expected, RibbonDisplayPreferences.loadQuickAccessCommands());
		GuiAcceptanceSupport.await(() -> expected.equals(toolbarOrder(expected)),
			"the rebuilt Quick Access Toolbar does not show the new order: " + toolbarOrder(expected));
	}

	@Test
	void exportResetImportAndApplyRestoreTheCustomization() throws Exception {
		RibbonCustomization original = new RibbonCustomization(List.of(), Set.of("HelpRibbonTask"),
			List.of(new RibbonCustomization.CustomTab("gui-tab-x", "Tab X", List.of(
				new RibbonCustomization.CustomGroup("gui-group-x", "Group X", List.of("RibbonFind"))))),
			java.util.Map.of());
		List<String> toolbar = List.of("RibbonFind", RibbonDisplayPreferences.defaultQuickAccessCommands().get(0));
		RibbonDisplayPreferences.saveRibbonCustomization(original);
		RibbonDisplayPreferences.saveQuickAccessCommands(toolbar);
		java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("mpui-gui");
		java.nio.file.Path file = directory.resolve("shared.mpui");
		CustomizationTransfer.FileChoice previousChoice = CustomizationTransfer.fileChoice;
		CustomizationTransfer.fileChoice = (parent, forSave) -> file;
		try {
			PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
			awaitShowing("ribbonCustomizeTree", JTree.class);
			click(button("ribbonCustomizeImportExport"));
			click(menuItem("ribbonCustomizeExport"));
			GuiAcceptanceSupport.await(() -> java.nio.file.Files.exists(file), "Export did not write the file");
			click(findButton(dialog, UsabilityStrings.text("preferences.cancel")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Cancel");

			// Reset the stored settings, then bring them back from the file.
			RibbonDisplayPreferences.saveRibbonCustomization(RibbonCustomization.none());
			RibbonDisplayPreferences.resetQuickAccessCommands();
			PreferencesDialogBox second = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
			JTree tree = awaitShowing("ribbonCustomizeTree", JTree.class);
			assertFalse(tabLabels(tree).contains("Tab X"));
			click(button("ribbonCustomizeImportExport"));
			click(menuItem("ribbonCustomizeImport"));
			GuiAcceptanceSupport.await(() -> tabLabels(tree).contains("Tab X"), "Import did not restore the custom tab");
			assertEquals(RibbonCustomization.none(), RibbonDisplayPreferences.loadRibbonCustomization(),
				"Import must not store anything before Apply");
			assertEquals(RibbonDisplayPreferences.defaultQuickAccessCommands(), RibbonDisplayPreferences.loadQuickAccessCommands(),
				"Import must not store the toolbar before Apply");
			click(findButton(second, UsabilityStrings.text("preferences.apply")));
			GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");
			assertEquals(original, RibbonDisplayPreferences.loadRibbonCustomization());
			assertEquals(toolbar, RibbonDisplayPreferences.loadQuickAccessCommands());
		} finally {
			CustomizationTransfer.fileChoice = previousChoice;
			java.nio.file.Files.deleteIfExists(file);
			java.nio.file.Files.deleteIfExists(directory);
		}
	}

	// ----- evidence: screenshots, keyboard journey, accessible names -----

	/** Captures the evidence matrix screens; the locale and scale come from the GUI test run (visual matrix legs). */
	@Test
	void capturesTheCustomizedRibbonOverflowMenuAndBothCategoriesAtNormalAndNarrowWidth() throws Exception {
		RibbonDisplayPreferences.saveRibbonCustomization(RibbonGuiEnvironment.customizedRibbon());
		openRibbonFrame();
		assertTrue(hasRibbonTab(RibbonGuiEnvironment.FIXTURE_CUSTOM_TAB_ID), "the fixture's custom tab must be on the ribbon");
		assertFalse(hasRibbonTab(RibbonGuiEnvironment.FIXTURE_HIDDEN_TAB_ID), "the fixture's hidden tab must not be on the ribbon");
		bringToFront(ribbonFrame);
		// Show the custom tab's groups in both the normal and the narrow capture.
		ModernRibbonPanel ribbonPanel = (ModernRibbonPanel) ribbonFrame.getRibbonPanel()
			.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		assertTrue(onEdt(() -> ribbonPanel.selectTab(RibbonGuiEnvironment.FIXTURE_CUSTOM_TAB_ID)),
			"the custom tab must be selectable");
		java.nio.file.Path ribbon = DialogScreenshotArtifacts.captureScreen(frameBounds(), "ribbon-customization-ribbon-normal");
		assertTrue(java.nio.file.Files.size(ribbon) > 0);

		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		awaitShowing("ribbonCustomizeTree", JTree.class);
		DialogScreenshotArtifacts.capture(dialog, "ribbon-customization-customize-ribbon-normal");
		// The page takes the dialog width: a page that keeps its preferred width needs a horizontal scroll bar
		// and hides the Up/Down buttons and the action row, which users report as an insufficient window width.
		JScrollPane ribbonPage = pageScroll(dialog, "ribbonCustomizationPanel");
		assertNotNull(ribbonPage, "the customize page must be shown");
		assertFalse(onEdt(() -> ribbonPage.getHorizontalScrollBar().isShowing()),
			"the customize page must fit the dialog width instead of scrolling sideways");
		AbstractButton upButton = find("ribbonCustomizeUp", AbstractButton.class);
		java.awt.Rectangle upVisible = new java.awt.Rectangle();
		SwingUtilities.invokeAndWait(() -> upVisible.setBounds(upButton.getVisibleRect()));
		assertEquals(upButton.getWidth(), upVisible.width,
			"the Up button must be fully visible inside the customize page: " + upVisible
				+ " of width " + upButton.getWidth());
		selectCategory(dialog, "preferences.quickAccess");
		awaitShowing("quickAccessAvailable", JList.class);
		DialogScreenshotArtifacts.capture(dialog, "ribbon-customization-quick-access-normal");
		narrow(dialog);
		DialogScreenshotArtifacts.capture(dialog, "ribbon-customization-quick-access-narrow");
		// The narrowed dialog is driven by the keyboard back to the Customize Ribbon category.
		JList<?> categories = awaitShowing("preferencesCategories", JList.class);
		String ribbonCategory = UsabilityStrings.text("preferences.customizeRibbon");
		focusByTab(categories);
		for (int step = 0; step < 20 && !ribbonCategory.equals(categories.getSelectedValue()); step++) tap(KeyEvent.VK_UP);
		assertEquals(ribbonCategory, categories.getSelectedValue());
		awaitShowing("ribbonCustomizeTree", JTree.class);
		DialogScreenshotArtifacts.capture(dialog, "ribbon-customization-customize-ribbon-narrow");
		click(findButton(dialog, UsabilityStrings.text("preferences.cancel")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Cancel");

		// Narrow width: the tab strip overflows into a menu that lists the custom tab and omits the hidden one.
		SwingUtilities.invokeAndWait(() -> ribbonFrame.setSize(320, 300));
		bringToFront(ribbonFrame);
		AbstractButton overflow = tabOverflowButton();
		GuiAcceptanceSupport.await(overflow::isShowing, "the tab overflow control is not showing at narrow width");
		DialogScreenshotArtifacts.captureScreen(frameBounds(), "ribbon-customization-ribbon-narrow");
		// The active tab is pinned to the tab strip, so select a default tab to make the custom tab overflow.
		assertTrue(onEdt(() -> ribbonPanel.selectTab("TaskRibbonTask")), "the Home tab must be selectable");
		GuiAcceptanceSupport.await(() -> tabOverflowButton().isShowing(), "the tab overflow control is not showing after selecting Home");
		// Wait for the strip to relayout: the custom tab leaves the strip once Home is the pinned active tab.
		GuiAcceptanceSupport.await(() -> UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream()
			.noneMatch(c -> ("microProjectRibbonTab-" + RibbonGuiEnvironment.FIXTURE_CUSTOM_TAB_ID).equals(c.getName()) && c.isShowing()),
			"the custom tab did not move into the overflow menu");
		robot.waitForIdle();
		AbstractButton overflowAfter = tabOverflowButton();
		// Open the overflow menu from the keyboard: focus the control and press Space.
		SwingUtilities.invokeAndWait(overflowAfter::requestFocusInWindow);
		GuiAcceptanceSupport.await(overflowAfter::isFocusOwner, "the tab overflow control did not take the keyboard focus");
		tap(KeyEvent.VK_SPACE);
		GuiAcceptanceSupport.await(() -> overflowAfter.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY) instanceof JPopupMenu popup
			&& popup.isShowing(), "the tab overflow menu did not open");
		JPopupMenu popup = (JPopupMenu) overflowAfter.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
		List<String> items = new ArrayList<>();
		for (Component component : popup.getComponents())
			if (component instanceof AbstractButton item) items.add(item.getText());
		assertTrue(items.contains("Evidence Tab"), "the custom tab must be listed in the overflow menu: " + items);
		assertFalse(items.contains(RibbonCustomizationCatalog.tabTitle(RibbonGuiEnvironment.FIXTURE_HIDDEN_TAB_ID)),
			"the hidden default tab must not appear in the overflow menu: " + items);
		Rectangle[] area = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> area[0] = new Rectangle(popup.getLocationOnScreen(), popup.getSize()));
		area[0] = area[0].union(frameBounds());
		DialogScreenshotArtifacts.captureScreen(area[0], "ribbon-customization-overflow-menu-narrow");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		robot.waitForIdle();
	}

	/** The whole journey is driven by the keyboard once the dialog is open: Tab, arrows, Space and Alt, no mouse. */
	@Test
	void keyboardOnlyJourneyCreatesATabAddsACommandAppliesItAndRunsTheCommandByKeyTip() throws Exception {
		openRibbonFrame();
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_GENERAL);
		JList<?> categories = awaitShowing("preferencesCategories", JList.class);
		String target = UsabilityStrings.text("preferences.customizeRibbon");
		focusByTab(categories);
		for (int step = 0; step < 20 && !target.equals(categories.getSelectedValue()); step++) tap(KeyEvent.VK_DOWN);
		assertEquals(target, categories.getSelectedValue(), "arrow keys did not reach the Customize Ribbon category");

		JTree tree = awaitShowing("ribbonCustomizeTree", JTree.class);
		activate(button("ribbonCustomizeNewTab"));
		awaitSelectedLabel(tree, UsabilityStrings.text("ribbonCustomize.newTabName"));
		focusByTab(tree);
		tap(KeyEvent.VK_RIGHT);
		tap(KeyEvent.VK_DOWN);
		awaitSelectedLabel(tree, UsabilityStrings.text("ribbonCustomize.newGroupName"));

		JList<?> commands = awaitShowing("ribbonCustomizeCommands", JList.class);
		focusByTab(commands);
		tap(KeyEvent.VK_HOME);
		for (int step = 0; step < commands.getModel().getSize()
			&& !(commands.getSelectedValue() instanceof RibbonCustomizationCatalog.Command c && c.id().equals("RibbonCut")); step++)
			tap(KeyEvent.VK_DOWN);
		assertTrue(commands.getSelectedValue() instanceof RibbonCustomizationCatalog.Command c && c.id().equals("RibbonCut"),
			"arrow keys did not reach the Cut command in the command list");
		activate(button("ribbonCustomizeAdd"));
		awaitSelectedLabel(tree, RibbonCustomizationCatalog.commandName("RibbonCut"));
		activate(findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> findDialog() == null, "Preferences dialog did not close after Apply");

		RibbonCustomization saved = RibbonDisplayPreferences.loadRibbonCustomization();
		assertEquals(1, saved.customTabs().size());
		assertEquals(List.of("RibbonCut"), saved.customTabs().get(0).groups().get(0).commandIds());
		GuiAcceptanceSupport.await(() -> hasRibbonTab(saved.customTabs().get(0).id()), "the open ribbon did not show the applied tab");

		// The custom tab takes Office's fallback tab KeyTip (Y1), so the route is Alt, Y, 1 for the tab and then
		// X for the Cut command inside its custom group.
		String customTabId = saved.customTabs().get(0).id();
		bringToFront(ribbonFrame);
		tap(KeyEvent.VK_ALT);
		tap(KeyEvent.VK_Y);
		tap(KeyEvent.VK_1);
		assertTrue(onEdt(() -> ribbonTab(customTabId).isSelected()),
			"the KeyTip sequence Alt, Y, 1 did not select the custom tab");

		// The panel builds a tab body when the tab is first shown, so the observer goes on the Cut button that the
		// custom tab now shows.
		int[] runs = new int[1];
		List<AbstractButton> cutButtons = new ArrayList<>();
		// At 125% and 150% the window is narrower than requested and the Clipboard group may
		// collapse; its commands then live in the collapsed group's popup.
		List<Component> candidates = new ArrayList<>(UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()));
		for (Component component : List.copyOf(candidates))
			if (component instanceof javax.swing.JComponent swing
				&& swing.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof javax.swing.JPopupMenu popup)
				candidates.addAll(UiComponentWalker.flatten(popup));
		for (Component component : candidates)
			if (component instanceof AbstractButton button
				&& ("RibbonCut".equals(button.getActionCommand()) || "RibbonCut".equals(button.getName()))) cutButtons.add(button);
		assertFalse(cutButtons.isEmpty(), "the custom tab must show the Cut command it was given");
		cutButtons.forEach(button -> button.addActionListener(event -> runs[0]++));
		tap(KeyEvent.VK_X);
		GuiAcceptanceSupport.await(() -> runs[0] > 0,
			"the KeyTip sequence Alt, Y, 1, X did not run the Cut command of the custom tab");
	}

	@Test
	void everyControlOfBothCategoriesHasAnAccessibleName() throws Exception {
		PreferencesDialogBox dialog = openDialog(PreferencesDialogBox.CATEGORY_CUSTOMIZE_RIBBON);
		awaitShowing("ribbonCustomizeTree", JTree.class);
		assertAccessibleNames("ribbonCustomizationPanel", 8);
		selectCategory(dialog, "preferences.quickAccess");
		awaitShowing("quickAccessAvailable", JList.class);
		assertAccessibleNames("quickAccessCustomizationPanel", 6);
	}

	private void assertAccessibleNames(String panelName, int minimumControls) throws Exception {
		List<String> unnamed = new ArrayList<>();
		int[] checked = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			Component panel = null;
			for (Window window : Window.getWindows())
				if (window.isShowing())
					for (Component component : UiComponentWalker.flatten(window))
						if (panelName.equals(component.getName()) && component.isShowing()) panel = component;
			assertNotNull(panel, panelName + " is not showing");
			for (Component component : UiComponentWalker.flatten(panel)) {
				if (!(component instanceof AbstractButton || component instanceof JList || component instanceof JTree
					|| component instanceof javax.swing.text.JTextComponent || component instanceof javax.swing.JComboBox
					|| component instanceof javax.swing.JSpinner || component instanceof javax.swing.JTable)) continue;
				if (javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JScrollBar.class, component) != null) continue;
				checked[0]++;
				javax.accessibility.AccessibleContext context = ((javax.accessibility.Accessible) component).getAccessibleContext();
				String name = context == null ? null : context.getAccessibleName();
				if (name == null || name.isBlank())
					unnamed.add(component.getClass().getSimpleName() + "[" + component.getName() + "]");
			}
		});
		assertTrue(checked[0] >= minimumControls, panelName + " exposed only " + checked[0] + " controls");
		assertTrue(unnamed.isEmpty(), panelName + " controls without an accessible name: " + unnamed);
	}

	/** The scroll pane of the preferences page whose contents are named {@code pageName}. */
	private static JScrollPane pageScroll(Component root, String pageName) {
		for (Component component : UiComponentWalker.flatten(root))
			if (component instanceof JScrollPane scroll && scroll.getViewport() != null
				&& scroll.getViewport().getView() != null && pageName.equals(scroll.getViewport().getView().getName()))
				return scroll;
		return null;
	}

	private AbstractButton tabOverflowButton() {
		return UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_PROPERTY)))
			.findFirst().orElseThrow(() -> new AssertionError("the narrow ribbon has no tab overflow control"));
	}

	private Rectangle frameBounds() {
		Rectangle[] bounds = new Rectangle[1];
		try {
			SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(ribbonFrame.getLocationOnScreen(), ribbonFrame.getSize()));
		} catch (Exception error) {
			throw new IllegalStateException(error);
		}
		return bounds[0];
	}

	private static void bringToFront(Window window) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			window.toFront();
			window.requestFocus();
		});
		GuiAcceptanceSupport.await(window::isActive, "the window did not become active");
	}

	private static void narrow(PreferencesDialogBox dialog) throws Exception {
		SwingUtilities.invokeAndWait(() -> dialog.setSize(Math.max(NARROW_DIALOG_WIDTH, dialog.getMinimumSize().width), dialog.getHeight()));
	}

	// ----- keyboard input -----

	private void tap(int keyCode) {
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		robot.waitForIdle();
	}

	/** Moves the focus with Tab until {@code component} owns it. */
	private void focusByTab(Component component) throws Exception {
		for (int step = 0; step < 60 && !onEdt(component::isFocusOwner); step++) tap(KeyEvent.VK_TAB);
		assertTrue(onEdt(component::isFocusOwner), "Tab did not reach " + component.getName());
	}

	private void activate(AbstractButton button) throws Exception {
		focusByTab(button);
		tap(KeyEvent.VK_SPACE);
	}

	private static boolean onEdt(java.util.function.BooleanSupplier supplier) throws Exception {
		boolean[] result = new boolean[1];
		SwingUtilities.invokeAndWait(() -> result[0] = supplier.getAsBoolean());
		return result[0];
	}

	// ----- fixture -----

	private static java.util.prefs.Preferences node() {
		return java.util.prefs.Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
	}

	private void openRibbonFrame() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		SwingUtilities.invokeAndWait(() -> {
			ribbonFrame = new MainRibbonFrame("Customize ribbon acceptance", "");
			MicroProjectShell.installRibbonShell(ribbonFrame, manager, () -> { });
			ribbonFrame.getContentPane().add(new JPanel(), java.awt.BorderLayout.CENTER);
			ribbonFrame.setSize(1000, 300);
			ribbonFrame.setLocation(0, 0);
			ribbonFrame.setVisible(true);
		});
		GuiAcceptanceSupport.await(ribbonFrame::isShowing, "ribbon window did not become visible");
	}

	private PreferencesDialogBox openDialog(String category) throws Exception {
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, preferences, null, null, category));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		return findDialog();
	}

	private static PreferencesDialogBox findDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof PreferencesDialogBox dialog && dialog.isVisible()) return dialog;
		return null;
	}

	private boolean hasRibbonTab(String tabId) {
		return UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream().anyMatch(component ->
			component instanceof AbstractButton button
				&& tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)));
	}

	private AbstractButton ribbonTab(String tabId) {
		return UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)))
			.findFirst().orElseThrow();
	}

	private int customTabCount() {
		return (int) UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream().filter(component ->
			component instanceof AbstractButton button
				&& String.valueOf(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)).startsWith("custom-")).count();
	}

	/** The toolbar command IDs found on the live toolbar, in layout order, among the expected ones. */
	private List<String> toolbarOrder(List<String> candidates) {
		Component panel = UiComponentWalker.flatten(ribbonFrame.getRibbonPanel()).stream()
			.filter(component -> QUICK_ACCESS_PANEL_NAME.equals(component.getName())).findFirst().orElse(null);
		List<String> order = new ArrayList<>();
		if (panel == null) return order;
		for (Component component : UiComponentWalker.flatten(panel))
			if (component.getName() != null && candidates.contains(component.getName()) && !order.contains(component.getName()))
				order.add(component.getName());
		return order;
	}

	// ----- component lookup -----

	private static <T extends Component> T find(String name, Class<T> type) {
		Object[] found = new Object[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (Window window : Window.getWindows()) {
					if (!window.isShowing()) continue;
					for (Component component : UiComponentWalker.flatten(window)) {
						if (name.equals(component.getName()) && type.isInstance(component) && component.isShowing()) {
							found[0] = component;
							return;
						}
					}
				}
			});
		} catch (Exception error) {
			throw new IllegalStateException(error);
		}
		return type.cast(found[0]);
	}

	private static <T extends Component> T awaitShowing(String name, Class<T> type) throws Exception {
		GuiAcceptanceSupport.await(() -> find(name, type) != null, "Component " + name + " did not become visible");
		return find(name, type);
	}

	private static AbstractButton button(String name) throws Exception {
		return awaitShowing(name, AbstractButton.class);
	}

	private static AbstractButton menuItem(String name) throws Exception {
		return awaitShowing(name, AbstractButton.class);
	}

	private static AbstractButton findButton(Component root, String text) {
		for (Component component : UiComponentWalker.flatten(root))
			if (component instanceof AbstractButton button && text.equals(button.getText())) return button;
		throw new AssertionError("No button labelled " + text);
	}

	// ----- physical input -----

	private void click(Component component) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = component.getLocationOnScreen();
			center[0] = new Point(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		});
		clickAt(center[0]);
	}

	private void clickAt(Point target) {
		robot.mouseMove(target.x, target.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void clickRow(JTree tree, String label, int depth) throws Exception {
		Point[] target = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			int row = rowOf(tree, label, depth);
			assertTrue(row >= 0, "No tree row " + label);
			tree.scrollRowToVisible(row);
			Rectangle bounds = tree.getRowBounds(row);
			Rectangle visible = tree.getVisibleRect();
			Point origin = tree.getLocationOnScreen();
			int x = Math.min(bounds.x + bounds.width - 4, visible.x + visible.width - 4);
			target[0] = new Point(origin.x + x, origin.y + bounds.y + bounds.height / 2);
		});
		clickAt(target[0]);
		awaitSelectedLabel(tree, label);
	}

	private static void expandRow(JTree tree, String label, int depth) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			int row = rowOf(tree, label, depth);
			assertTrue(row >= 0, "No tree row " + label);
			tree.expandRow(row);
		});
	}

	private static int rowOf(JTree tree, String label, int depth) {
		for (int row = 0; row < tree.getRowCount(); row++) {
			TreePath path = tree.getPathForRow(row);
			if (path.getPathCount() == depth && label.equals(String.valueOf(path.getLastPathComponent()))) return row;
		}
		return -1;
	}

	private static void awaitSelectedLabel(JTree tree, String label) throws Exception {
		GuiAcceptanceSupport.await(() -> {
			TreePath path = tree.getSelectionPath();
			return path != null && label.equals(String.valueOf(path.getLastPathComponent()));
		}, "Tree row " + label + " was not selected");
	}

	private static List<String> tabLabels(JTree tree) {
		List<String> labels = new ArrayList<>();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		for (int index = 0; index < root.getChildCount(); index++) labels.add(String.valueOf(root.getChildAt(index)));
		return labels;
	}

	private void clickListCommand(JList<?> list, String commandId) throws Exception {
		Point[] target = new Point[1];
		String name = RibbonCustomizationCatalog.commandName(commandId);
		SwingUtilities.invokeAndWait(() -> {
			int index = -1;
			for (int i = 0; i < list.getModel().getSize(); i++) {
				if (list.getModel().getElementAt(i) instanceof RibbonCustomizationCatalog.Command command
					&& command.id().equals(commandId)) index = i;
			}
			assertTrue(index >= 0, "No list entry for " + commandId);
			list.ensureIndexIsVisible(index);
			Rectangle cell = list.getCellBounds(index, index);
			Point origin = list.getLocationOnScreen();
			target[0] = new Point(origin.x + cell.x + Math.min(cell.width, list.getVisibleRect().width) / 2,
				origin.y + cell.y + cell.height / 2);
		});
		clickAt(target[0]);
		GuiAcceptanceSupport.await(() -> list.getSelectedValue() instanceof RibbonCustomizationCatalog.Command command
			&& command.id().equals(commandId), "List entry " + name + " was not selected");
	}

	private static List<String> chosenIds(JList<?> list) {
		List<String> ids = new ArrayList<>();
		for (int index = 0; index < list.getModel().getSize(); index++)
			ids.add(((RibbonCustomizationCatalog.Command) list.getModel().getElementAt(index)).id());
		return ids;
	}

	private void selectCategory(PreferencesDialogBox dialog, String key) throws Exception {
		JList<?> categories = awaitShowing("preferencesCategories", JList.class);
		String name = UsabilityStrings.text(key);
		Point[] row = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			int index = -1;
			for (int i = 0; i < categories.getModel().getSize(); i++)
				if (name.equals(String.valueOf(categories.getModel().getElementAt(i)))) index = i;
			assertTrue(index >= 0, "Category " + name + " is not listed");
			Rectangle cell = categories.getCellBounds(index, index);
			Point origin = categories.getLocationOnScreen();
			row[0] = new Point(origin.x + cell.x + cell.width / 2, origin.y + cell.y + cell.height / 2);
		});
		// The first click on a window that is not yet the foreground window can be used up by the activation,
		// so wait for the dialog to be active and click again, after the multi-click interval, if the row stayed unselected.
		GuiAcceptanceSupport.await(dialog::isActive, "Preferences dialog did not become the active window");
		for (int attempt = 0; attempt < 3 && !onEdt(() -> name.equals(categories.getSelectedValue())); attempt++) {
			GuiAcceptanceSupport.awaitMultiClickIntervalElapsed(robot);
			clickAt(row[0]);
			for (int wait = 0; wait < 10 && !onEdt(() -> name.equals(categories.getSelectedValue())); wait++) robot.delay(100);
		}
		GuiAcceptanceSupport.await(() -> name.equals(categories.getSelectedValue()), "Category " + name + " was not selected");
	}

	private static boolean fileRowChecked(JTree tree, String fileTitle) throws Exception {
		boolean[] checked = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			int row = rowOf(tree, fileTitle, 2);
			Component renderer = tree.getCellRenderer().getTreeCellRendererComponent(tree,
				tree.getPathForRow(row).getLastPathComponent(), true, false, false, row, true);
			checked[0] = renderer instanceof javax.swing.JCheckBox box && box.isSelected() && !box.isEnabled();
		});
		return checked[0];
	}

	private static void assertDisabled(String what, AbstractButton... buttons) {
		for (AbstractButton button : buttons)
			assertFalse(button.isEnabled(), button.getName() + " must be disabled for a " + what);
	}

	// ----- option panes -----

	private JOptionPane awaitOptionPane() throws Exception {
		JOptionPane[] found = new JOptionPane[1];
		GuiAcceptanceSupport.await(() -> {
			try {
				SwingUtilities.invokeAndWait(() -> {
					found[0] = null;
					for (Window window : Window.getWindows()) {
						if (!window.isShowing()) continue;
						for (Component component : UiComponentWalker.flatten(window))
							if (component instanceof JOptionPane pane && pane.isShowing()) found[0] = pane;
					}
				});
			} catch (Exception error) {
				throw new IllegalStateException(error);
			}
			return found[0] != null;
		}, "No option dialog became visible");
		return found[0];
	}

	private void clickOptionButton(JOptionPane pane, boolean ok) throws Exception {
		String text = UIManager.getString(ok ? "OptionPane.okButtonText" : "OptionPane.cancelButtonText", pane.getLocale());
		List<AbstractButton> buttons = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(pane))
			if (component instanceof javax.swing.JButton button && button.isShowing()) buttons.add(button);
		AbstractButton chosen = buttons.stream().filter(button -> button.getText() != null
			&& button.getText().replace("&", "").equals(text)).findFirst()
			.orElse(buttons.get(ok ? 0 : buttons.size() - 1));
		click(chosen);
		GuiAcceptanceSupport.await(() -> !pane.isShowing(), "Option dialog did not close");
	}

	private void answerConfirm(boolean ok) throws Exception {
		clickOptionButton(awaitOptionPane(), ok);
	}

	private void answerInput(String value) throws Exception {
		JOptionPane pane = awaitOptionPane();
		JTextField field = UiComponentWalker.flatten(pane).stream().filter(JTextField.class::isInstance)
			.map(JTextField.class::cast).findFirst().orElseThrow();
		click(field);
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
			new java.awt.datatransfer.StringSelection(value), null);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> value.equals(field.getText()), "Input text was not entered: " + field.getText());
		assertNotNull(pane);
		clickOptionButton(pane, true);
	}
}
