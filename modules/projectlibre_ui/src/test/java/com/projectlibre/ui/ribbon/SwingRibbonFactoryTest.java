package com.projectlibre.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.junit.jupiter.api.Test;

import com.projectlibre1.menu.ExtToolBarFactory;
import com.projectlibre1.menu.MenuActionMapSupport;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.menu.testsupport.MenuDefinitionSupport;
import com.projectlibre1.menu.testsupport.UiComponentWalker;
import com.projectlibre1.util.FlatUiSupport;

class SwingRibbonFactoryTest {
	@Test
	void standardRibbonModelMatchesTheResourceStructure() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
		assertEquals(MenuDefinitionSupport.ribbonTaskIds().size(), model.getTabs().size());
		assertEquals("File", model.getTabs().get(0).getTitle());
		assertFalse(model.getTabs().get(0).getBands().isEmpty());
		assertNotNull(model.getTabs().get(0).getBands().get(0).getTitle());
		assertEquals(List.of("RibbonTopBarSaveProject", "RibbonTopBarUndo", "RibbonTopBarRedo"),
			model.getTaskBarButtons());
	}

	@Test
	void modelPreservesButtonPriorityMetadata() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
		List<SwingRibbonModel.RibbonTab> tabs = model.getTabs();
		SwingRibbonModel.RibbonTab formatTab = tabs.stream()
			.filter(tab -> tab.getId().equals("FormatRibbonTask"))
			.findFirst()
			.orElseThrow();
		SwingRibbonModel.RibbonBand displayBand = formatTab.getBands().stream()
			.filter(band -> band.getId().equals("FormatDisplayRibbonBand"))
			.findFirst()
			.orElseThrow();

		assertTrue(displayBand.getButtons().stream().anyMatch(button -> button.getPriority() == SwingRibbonModel.ButtonPriority.TOP));
		assertTrue(displayBand.getButtons().stream().anyMatch(button -> button.getId().equals("RibbonGridlines")));
	}

	@Test
	void modelCarriesExplicitIconAndToggleMetadata() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
		SwingRibbonModel.RibbonButton gantt = model.getTabs().stream()
			.flatMap(tab -> tab.getBands().stream())
			.flatMap(band -> band.getButtons().stream())
			.filter(button -> button.getId().equals("RibbonGantt"))
			.findFirst()
			.orElseThrow();

		assertEquals("view.gantt", gantt.getIconKey());
		assertTrue(gantt.isToggle());
	}

	@Test
	void modelCarriesExplicitSizeSplitAndCollapseMetadata() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);

		SwingRibbonModel.RibbonButton filter = model.getTabs().stream()
			.flatMap(tab -> tab.getBands().stream())
			.flatMap(band -> band.getButtons().stream())
			.filter(button -> button.getId().equals("RibbonChooseFilter"))
			.findFirst().orElseThrow();
		assertEquals(SwingRibbonModel.ButtonSize.LARGE, filter.getButtonSize());
		assertTrue(filter.isSplit());
		assertEquals(100, filter.getCollapsePriority());
	}

	@Test
	void activeRibbonUsesOneOuterSurfaceAndSeparatorOnlyBands() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);

		long surfaces = UiComponentWalker.flatten(panel).stream()
			.filter(component -> ModernRibbonPanel.RIBBON_SURFACE_COMPONENT_NAME.equals(component.getName()))
			.count();
		assertEquals(1, surfaces);
		assertTrue(UiComponentWalker.flatten(panel).stream()
			.filter(component -> ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME.equals(component.getName()))
			.filter(javax.swing.JComponent.class::isInstance)
			.map(javax.swing.JComponent.class::cast)
			.allMatch(component -> component.getBorder() instanceof EmptyBorder));
	}

	@Test
	void everyRibbonTabRendersAtOfficeReferenceWidths() {
		for (int width : List.of(720, 760, 1024, 1200, 1440)) {
			ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
				MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
			JPanel host = factory.createPanel(model, null);
			ModernRibbonPanel ribbon = UiComponentWalker.flatten(host).stream()
				.filter(ModernRibbonPanel.class::isInstance)
				.map(ModernRibbonPanel.class::cast)
				.findFirst().orElseThrow();
			ribbon.setSize(width, 190);

			for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
				findButtonByText(ribbon, tab.getTitle()).doClick();
				layoutRecursively(ribbon);
				BufferedImage image = new BufferedImage(width, 190, BufferedImage.TYPE_INT_ARGB);
				var graphics = image.createGraphics();
				try {
					ribbon.paint(graphics);
				} finally {
					graphics.dispose();
				}
				assertTrue(hasVisiblePixel(image), () -> tab.getTitle() + " rendered blank at " + width + "px");
			}
		}
	}

	@Test
	void collapsedRibbonUsesOneReachableMenuForEveryBandAndCustomContent() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON, bandId ->
			"FormatLayoutRibbonBand".equals(bandId) ? customBand("Collapsed custom layout") : null);
		JPanel host = factory.createPanel(model, null);
		ModernRibbonPanel ribbon = UiComponentWalker.flatten(host).stream()
			.filter(ModernRibbonPanel.class::isInstance)
			.map(ModernRibbonPanel.class::cast)
			.findFirst().orElseThrow();

		ribbon.setSize(600, 190);
		ribbon.dispatchEvent(new ComponentEvent(ribbon, ComponentEvent.COMPONENT_RESIZED));
		findButtonByText(ribbon, "Task").doClick();
		layoutRecursively(ribbon);
		List<Component> taskBands = UiComponentWalker.flatten(ribbon).stream()
			.filter(component -> ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME.equals(component.getName()))
			.toList();
		assertEquals(1, taskBands.size(), "collapsed mode must not lay every band out horizontally");
		AbstractButton taskTrigger = findButtonByText(ribbon, "Task …");
		JPopupMenu taskPopup = (JPopupMenu) ((JComponent) taskTrigger)
			.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		assertNotNull(taskPopup);
		assertEquals(7, taskPopup.getComponentCount());
		int taskCommandCount = java.util.Arrays.stream(taskPopup.getComponents())
			.map(JMenu.class::cast)
			.mapToInt(JMenu::getMenuComponentCount)
			.sum();
		assertEquals(MenuDefinitionSupport.ribbonButtonIdsForTask("TaskRibbonTask").size(), taskCommandCount);

		findButtonByText(ribbon, "Format").doClick();
		layoutRecursively(ribbon);
		AbstractButton formatTrigger = findButtonByText(ribbon, "Format …");
		JPopupMenu formatPopup = (JPopupMenu) ((JComponent) formatTrigger)
			.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		JMenu layoutMenu = java.util.Arrays.stream(formatPopup.getComponents())
			.map(JMenu.class::cast)
			.filter(menu -> "Layout".equals(menu.getText()))
			.findFirst().orElseThrow();
		assertTrue(java.util.Arrays.stream(layoutMenu.getMenuComponents())
			.flatMap(component -> UiComponentWalker.flatten(component).stream())
			.anyMatch(component -> component instanceof JLabel label
				&& "Collapsed custom layout".equals(label.getText())));
	}

	@Test
	void actionStateSurvivesResponsiveRebuildBeforeAnInactiveTabIsOpened() {
		Map<String, Action> actions = new LinkedHashMap<>();
		var actionMap = new com.projectlibre1.menu.ProjectMenuActionMap() {
			@Override
			public Action getAction(String key) {
				return actions.computeIfAbsent(key, actionId -> new AbstractAction(actionId) {
					@Override
					public void actionPerformed(ActionEvent event) {
					}
				});
			}

			@Override
			public String getStringFromAction(Action action) {
				return String.valueOf(action.getValue(Action.NAME));
			}
		};
		MenuManager manager = MenuManager.getInstance(actionMap);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = UiComponentWalker.flatten(host).stream()
			.filter(ModernRibbonPanel.class::isInstance)
			.map(ModernRibbonPanel.class::cast)
			.findFirst().orElseThrow();

		ribbon.setSize(1000, 190);
		ribbon.dispatchEvent(new ComponentEvent(ribbon, ComponentEvent.COMPONENT_RESIZED));
		manager.setActionEnabled("RibbonResourceInformation", false);
		findButtonByText(ribbon, manager.getString("ResourceRibbonTask.title")).doClick();
		AbstractButton resourceInformation = manager.getToolButtonsFromId("RibbonResourceInformation").stream()
			.map(AbstractButton.class::cast)
			.filter(button -> "RibbonResourceInformation".equals(button.getActionCommand()))
			.findFirst().orElseThrow();
		assertFalse(resourceInformation.isEnabled());
	}

	@Test
	void fileTabKeepsOfficeStylePrimaryCommandsLarge() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON);
		SwingRibbonModel.RibbonTab fileTab = model.getTabs().stream()
			.filter(tab -> tab.getId().equals("FileRibbonTask"))
			.findFirst()
			.orElseThrow();

		SwingRibbonModel.RibbonBand fileBand = fileTab.getBands().stream()
			.filter(band -> band.getId().equals("FileRibbonBand"))
			.findFirst()
			.orElseThrow();
		SwingRibbonModel.RibbonBand printBand = fileTab.getBands().stream()
			.filter(band -> band.getId().equals("PrintRibbonBand"))
			.findFirst()
			.orElseThrow();

		assertPriority(fileBand, "RibbonNewProject", SwingRibbonModel.ButtonPriority.TOP);
		assertPriority(fileBand, "RibbonOpenProject", SwingRibbonModel.ButtonPriority.TOP);
		assertPriority(fileBand, "RibbonSaveProject", SwingRibbonModel.ButtonPriority.TOP);
		assertPriority(fileBand, "RibbonSaveProjectAs", SwingRibbonModel.ButtonPriority.MEDIUM);
		assertPriority(fileBand, "RibbonCloseProject", SwingRibbonModel.ButtonPriority.MEDIUM);
		assertPriority(printBand, "RibbonPrint", SwingRibbonModel.ButtonPriority.TOP);
		assertPriority(printBand, "RibbonPrintPreview", SwingRibbonModel.ButtonPriority.MEDIUM);
		assertPriority(printBand, "RibbonPDF", SwingRibbonModel.ButtonPriority.MEDIUM);
		assertSize(fileBand, "RibbonNewProject", SwingRibbonModel.ButtonSize.LARGE);
		assertSize(fileBand, "RibbonOpenProject", SwingRibbonModel.ButtonSize.LARGE);
		assertSize(fileBand, "RibbonSaveProject", SwingRibbonModel.ButtonSize.SMALL);
		assertSize(fileBand, "RibbonSaveProjectAs", SwingRibbonModel.ButtonSize.SMALL);
		assertSize(fileBand, "RibbonCloseProject", SwingRibbonModel.ButtonSize.SMALL);
	}

	@Test
	void customRibbonGeneratorBecomesACustomBandInTheModel() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON, bandId ->
			"FormatLayoutRibbonBand".equals(bandId) ? customBand("Layout via model") : null);

		SwingRibbonModel.RibbonTab formatTab = model.getTabs().stream()
			.filter(tab -> tab.getId().equals("FormatRibbonTask"))
			.findFirst()
			.orElseThrow();
		SwingRibbonModel.RibbonBand layoutBand = formatTab.getBands().stream()
			.filter(band -> band.getId().equals("FormatLayoutRibbonBand"))
			.findFirst()
			.orElseThrow();

		assertTrue(layoutBand.isCustomBand());
		assertEquals(SwingRibbonModel.RibbonBandKind.CUSTOM, layoutBand.getKind());
		assertNotNull(layoutBand.getCustomBandProvider());
	}

	@Test
	void createPanelUsesTheProvidedModelInsteadOfRebuildingFromResources() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		SwingRibbonModel model = new SwingRibbonModel(
			"CustomRibbon",
			List.of(
				new SwingRibbonModel.RibbonTab(
					"CustomTab",
					"Custom Title",
					List.of(
						new SwingRibbonModel.RibbonBand("CustomBand", "Custom Band", () -> customBand("Model driven"))))),
			List.of());

		JPanel panel = factory.createPanel(model, null);
		assertNotNull(findButtonByText(panel, "Custom Title"));
		assertNotNull(findLabelByText(panel, "Model driven"));
	}

	@Test
	void narrowRibbonReplacesBandsWithPopupTriggers() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = UiComponentWalker.flatten(host).stream()
			.filter(ModernRibbonPanel.class::isInstance)
			.map(ModernRibbonPanel.class::cast)
			.findFirst()
			.orElseThrow();
		ribbon.setSize(700, 180);
		for (var listener : ribbon.getComponentListeners()) {
			listener.componentResized(new ComponentEvent(ribbon, ComponentEvent.COMPONENT_RESIZED));
		}
		ribbon.doLayout();

		assertTrue(UiComponentWalker.flatten(ribbon).stream()
			.filter(JButton.class::isInstance)
			.map(JButton.class::cast)
			.anyMatch(button -> button.getText().endsWith(" …")));
	}

	@Test
	void compactRibbonUsesSmallButtonsAndOverflowTriggersBeforeFullCollapse() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
				MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
			ModernRibbonPanel ribbon = UiComponentWalker.flatten(host).stream()
				.filter(ModernRibbonPanel.class::isInstance)
				.map(ModernRibbonPanel.class::cast)
				.findFirst()
				.orElseThrow();
			findButtonByText(ribbon, "Task").doClick();
			ribbon.setSize(1300, 180);
			for (var listener : ribbon.getComponentListeners()) {
				listener.componentResized(new ComponentEvent(ribbon, ComponentEvent.COMPONENT_RESIZED));
			}
			ribbon.setSize(900, 180);
			for (var listener : ribbon.getComponentListeners()) {
				listener.componentResized(new ComponentEvent(ribbon, ComponentEvent.COMPONENT_RESIZED));
			}
			ribbon.doLayout();

			assertTrue(UiComponentWalker.flatten(ribbon).stream()
				.filter(AbstractButton.class::isInstance)
				.map(AbstractButton.class::cast)
				.anyMatch(button -> "small".equals(button.getClientProperty(RibbonButtonStyler.SIZE_PROPERTY))));
			assertTrue(UiComponentWalker.flatten(ribbon).stream()
				.filter(JButton.class::isInstance)
				.map(JButton.class::cast)
				.anyMatch(button -> "…".equals(button.getText())));
		});
	}

	@Test
	void ribbonPreferredHeightCanContainItsTallestLargeButton() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		int tallestLargeButton = UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> "large".equals(button.getClientProperty(RibbonButtonStyler.SIZE_PROPERTY)))
			.mapToInt(button -> button.getPreferredSize().height)
			.max()
			.orElse(0);

		assertTrue(panel.getPreferredSize().height >= com.projectlibre1.util.FlatUiSupport.ribbonTabHeight() + tallestLargeButton);
	}

	@Test
	void largeButtonsShareAStablePreferredHeightAcrossTheRibbon() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		List<Integer> largeButtonHeights = UiComponentWalker.flatten(panel).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> "large".equals(button.getClientProperty(RibbonButtonStyler.SIZE_PROPERTY)))
			.map(button -> button.getPreferredSize().height)
			.distinct()
			.sorted(Comparator.naturalOrder())
			.collect(Collectors.toList());

		assertEquals(List.of(com.projectlibre1.util.FlatUiSupport.ribbonLargeButtonHeight()), largeButtonHeights);
	}

	@Test
	void ribbonPreferredHeightStaysCloseToTheCompressedSurfaceHeight() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		int maxExpectedHeight = com.projectlibre1.util.FlatUiSupport.ribbonTabHeight()
			+ com.projectlibre1.util.FlatUiSupport.ribbonSurfaceHeight()
			+ 32; // outer rounded-surface insets and shadow

		assertTrue(panel.getPreferredSize().height <= maxExpectedHeight,
			() -> "preferred=" + panel.getPreferredSize().height + ", expected<=" + maxExpectedHeight);
	}

	@Test
	void selectedTabIsLeftAlignedAndShowsUnderlineBorder() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		AbstractButton fileTab = findButtonByText(panel, "File");

		assertEquals(SwingConstants.LEFT, fileTab.getHorizontalAlignment());
		assertTrue(fileTab.isBorderPainted());
		assertFalse(fileTab.getBorder() instanceof EmptyBorder);
		assertEquals(
			FlatUiSupport.BUTTON_STYLE_ROLE_RIBBON_TAB,
			fileTab.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY));
		assertNotNull(FlatUiSupport.resolveRibbonTabUnderlineColor(fileTab));
	}

	@Test
	void hoveredTabKeepsItsLabelVisible() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		AbstractButton taskTab = findButtonByText(panel, "Task");
		taskTab.getModel().setRollover(true);
		assertNotNull(taskTab.getForeground());
		assertTrue(taskTab.getForeground().getRGB() != taskTab.getBackground().getRGB(),
			"hovering a tab must not paint its label with the background color");
	}

	@Test
	void tabRowUsesTrailingGlueSoTabsStayLeftAligned() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);

		assertTrue(UiComponentWalker.flatten(panel).stream()
			.anyMatch(component -> component instanceof Box.Filler));
	}

	@Test
	void taskInsertBandStacksAllTaskInsertionCommandsAlongsideThePrimaryInsertButton() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.JAPAN));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		AbstractButton taskTab = findButtonByText(panel, "タスク");
		taskTab.doClick();
		panel.doLayout();
		layoutRecursively(panel);

		AbstractButton insertButton = findButtonByText(panel, "挿入");
		AbstractButton recurringButton = findButtonByText(panel, "繰り返しタスク");
		AbstractButton subprojectButton = findButtonByText(panel, "サブプロジェクト");
		int insertCenterY = SwingUtilities.convertPoint(insertButton.getParent(), insertButton.getLocation(), panel).y
			+ insertButton.getHeight() / 2;
		int recurringCenterY = SwingUtilities.convertPoint(recurringButton.getParent(), recurringButton.getLocation(), panel).y
			+ recurringButton.getHeight() / 2;
		int subprojectCenterY = SwingUtilities.convertPoint(subprojectButton.getParent(), subprojectButton.getLocation(), panel).y
			+ subprojectButton.getHeight() / 2;

		assertTrue(recurringCenterY < insertCenterY);
		assertTrue(insertCenterY < subprojectCenterY);
		assertTrue(subprojectCenterY - insertCenterY < 100);
	}

	@Test
	void ribbonTabAndBandTitleFontsFollowThemeHierarchy() {
		ExtToolBarFactory buttonFactory = new ExtToolBarFactory(
			MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonFactory factory = new SwingRibbonFactory(buttonFactory, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));

		JPanel panel = factory.createPanel(MenuManager.STANDARD_RIBBON, null);
		AbstractButton fileTab = findButtonByText(panel, "File");
		JLabel fileBandTitle = findLabelByText(panel, "File");

		assertEquals(FlatUiSupport.ribbonTabFont().getSize2D(), fileTab.getFont().getSize2D());
		assertEquals(FlatUiSupport.ribbonBandTitleFont().getSize2D(), fileBandTitle.getFont().getSize2D());
	}

	private static JPanel customBand(String text) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(text));
		return panel;
	}

	private static AbstractButton findButtonByText(Component root, String text) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof AbstractButton button && text.equals(button.getText())) {
				return button;
			}
		}
		throw new AssertionError("Button not found with text: " + text);
	}

	private static JLabel findLabelByText(Component root, String text) {
		for (Component component : UiComponentWalker.flatten(root)) {
			if (component instanceof JLabel label && text.equals(label.getText())) {
				return label;
			}
		}
		throw new AssertionError("Label not found with text: " + text);
	}

	private static void assertPriority(
		SwingRibbonModel.RibbonBand band,
		String buttonId,
		SwingRibbonModel.ButtonPriority expectedPriority) {
		SwingRibbonModel.ButtonPriority actualPriority = band.getButtons().stream()
			.filter(button -> button.getId().equals(buttonId))
			.map(SwingRibbonModel.RibbonButton::getPriority)
			.findFirst()
			.orElseThrow();
		assertEquals(expectedPriority, actualPriority);
	}

	private static void assertSize(
		SwingRibbonModel.RibbonBand band,
		String buttonId,
		SwingRibbonModel.ButtonSize expectedSize) {
		SwingRibbonModel.ButtonSize actualSize = band.getButtons().stream()
			.filter(button -> button.getId().equals(buttonId))
			.map(SwingRibbonModel.RibbonButton::getButtonSize)
			.findFirst()
			.orElseThrow();
		assertEquals(expectedSize, actualSize);
	}

	private static void layoutRecursively(Component component) {
		component.doLayout();
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				layoutRecursively(child);
			}
		}
	}

	private static boolean hasVisiblePixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (((image.getRGB(x, y) >>> 24) & 0xff) != 0) return true;
			}
		}
		return false;
	}
}
