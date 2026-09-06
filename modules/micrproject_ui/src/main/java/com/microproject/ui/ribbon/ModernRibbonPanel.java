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
package com.microproject.ui.ribbon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.util.FlatUiSupport;

public final class ModernRibbonPanel extends JPanel {
	/** Client-property key on the ribbon host for view-context coordination. */
	public static final String CONTEXTUAL_TABS_PROPERTY = "microproject.ribbon.contextualTabs";
	static final String RIBBON_SURFACE_COMPONENT_NAME = "projectLibreRibbonSurface";
	static final String RIBBON_BAND_COMPONENT_NAME = "projectLibreRibbonBand";
	static final String COLLAPSED_POPUP_PROPERTY = "MicroProject.ribbonCollapsedPopup";
	public static final String COLLAPSED_TAB_LAUNCHER_PROPERTY = "MicroProject.ribbonCollapsedTabLauncher";
	public static final String BAND_PROXY_PROPERTY = "MicroProject.ribbonBandProxy";
	public static final String SCROLL_PREVIOUS_PROPERTY = "MicroProject.ribbonScrollPrevious";
	public static final String SCROLL_NEXT_PROPERTY = "MicroProject.ribbonScrollNext";
	private static final int BAND_MIN_WIDTH = 72;
	private static final int BAND_INNER_GAP = 4;
	private static final int BAND_SIDE_PADDING = 4;
	// A band has both its content padding and the extra breathing room that
	// separates command groups.  Keep this value shared by the border and the
	// width calculation: using only BAND_SIDE_PADDING in the calculation gave a
	// 64px large Gantt button just 56px of usable width, clipping its icon.
	private static final int BAND_HORIZONTAL_INSET = BAND_SIDE_PADDING + 4;
	private static final int BAND_HORIZONTAL_INSETS = BAND_HORIZONTAL_INSET * 2;
	private static final int INLINE_COLUMN_GAP = 4;
	private static final int LARGE_BUTTON_GAP = 2;
	private static final int BAND_GAP = 8;
	// The standard desktop window is typically 1024-1200 px wide.  Treating
	// every width below 1200 as compact hides too many commands even when the
	// full band set still fits.  Reserve compact density for genuinely narrow
	// work areas and let the measured band widths handle the final fit decision.
	private enum RibbonDensity {
		FULL(Integer.MIN_VALUE, false),
		COMPACT(Integer.MIN_VALUE, true),
		/** Keeps primary commands direct while moving secondary commands into one band overflow. */
		PRIORITY_COMPACT(50, true),
		/** A proxy represents one command group; it never represents a whole tab. */
		COLLAPSED(Integer.MAX_VALUE, false);

		private final int collapsedPriority;
		private final boolean compacted;

		RibbonDensity(int collapsedPriority, boolean compacted) {
			this.collapsedPriority = collapsedPriority;
			this.compacted = compacted;
		}

		static RibbonDensity forWidth(int width) {
			// Width buckets are not an Office ribbon policy.  Locale, DPI and the
			// actual command set determine the required width, so buildTabBody()
			// selects the first presentation that measures within the client area.
			return FULL;
		}

		boolean isCompacted() {
			return compacted;
		}

		boolean collapses(int priority) {
			return priority <= collapsedPriority;
		}

	}
	private final SwingRibbonModel model;
	private final ExtToolBarFactory buttonFactory;
	private final ResourceBundle[] bundles;
	private final JPanel cards;
	private final ButtonGroup tabGroup;
	private final Map<String, JPanel> tabBodies;
	private final Map<String, Integer> tabBodyBuildWidths;
	private final Map<String, JToggleButton> tabButtons;
	private final Map<String, javax.swing.Action> commandActions = new LinkedHashMap<>();
	private final RibbonButtonStyler buttonStyler;
	private final Map<String, Integer> bandHeights;
	private final java.util.Set<String> visibleContextualTabs = new LinkedHashSet<>();
	private final Map<String, String> contextualTabTitles = new LinkedHashMap<>();
	private RibbonDensity density = RibbonDensity.FULL;
	private String activeTabId;
	private boolean rebuildingDensity;
	private JRootPane shortcutRoot;

	ModernRibbonPanel(SwingRibbonModel model, ExtToolBarFactory buttonFactory, ResourceBundle[] bundles, Runnable helpAction) {
		super(new BorderLayout());
		this.model = Objects.requireNonNull(model);
		this.buttonFactory = Objects.requireNonNull(buttonFactory);
		this.bundles = Objects.requireNonNull(bundles);
		this.cards = new JPanel(new BorderLayout());
		this.tabGroup = new ButtonGroup();
		this.tabBodies = new LinkedHashMap<>();
		this.tabBodyBuildWidths = new LinkedHashMap<>();
		this.tabButtons = new LinkedHashMap<>();
		this.buttonStyler = new RibbonButtonStyler();
		this.bandHeights = new LinkedHashMap<>();
		setOpaque(true);
		setBackground(FlatUiSupport.ribbonChromeBackground());
		this.cards.setOpaque(true);
		this.cards.setBackground(FlatUiSupport.ribbonChromeBackground());
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FlatUiSupport.ribbonTopLineColor()));
		setPreferredSize(new Dimension(0, FlatUiSupport.ribbonTabHeight() + FlatUiSupport.ribbonSurfaceHeight() + 1));
	}

	void build() {
		// The ribbon owns only the tab strip and command bands.  Window chrome and
		// document/workspace navigation are deliberately outside this component.
		add(buildTabRow(), BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			tabBodies.computeIfAbsent(tab.getId(), this::createTabBody);
		}
		if (!model.getTabs().isEmpty()) {
			showTab(model.getTabs().get(0).getId());
		}
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				updateResponsiveMode();
			}
		});
		updateResponsiveMode();
	}

	/** Selects the first document-oriented ribbon tab. */
	public void showProjectTab() {
		model.getTabs().stream()
			.filter(tab -> !"FileRibbonTask".equals(tab.getId()))
			.map(SwingRibbonModel.RibbonTab::getId)
			.findFirst()
			.ifPresent(this::showTab);
	}

	/**
	 * Shows only the supplied contextual tabs. Normal tabs are unaffected.
	 * Hiding a tab never unregisters commands: menu, shortcut and ribbon keep
	 * resolving to the same Action instance.
	 */
	public void setVisibleContextualTabs(Collection<String> tabIds) {
		visibleContextualTabs.clear();
		if (tabIds != null) visibleContextualTabs.addAll(tabIds);
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			if (!tab.isContextual()) continue;
			JToggleButton button = tabButtons.get(tab.getId());
			if (button != null) button.setVisible(visibleContextualTabs.contains(tab.getId()));
		}
		if (activeTabId != null && !isTabVisible(activeTabId)) firstVisibleTabId().ifPresent(this::showTab);
		revalidate();
		repaint();
	}

	public boolean isContextualTabVisible(String tabId) {
		return visibleContextualTabs.contains(tabId);
	}

	/** Labels contextual tabs with the active view, for example Gantt Chart Format. */
	public void setContextualTabTitles(Map<String, String> titles) {
		contextualTabTitles.clear();
		if (titles != null) contextualTabTitles.putAll(titles);
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			if (!tab.isContextual()) continue;
			JToggleButton button = tabButtons.get(tab.getId());
			if (button != null) button.setText(tabTitle(tab));
		}
		revalidate();
		repaint();
	}

	@Override
	public void addNotify() {
		super.addNotify();
		installTabAccessKeys();
		updateResponsiveMode();
	}

	@Override
	public void removeNotify() {
		uninstallTabAccessKeys();
		super.removeNotify();
	}

	private void installTabAccessKeys() {
		JRootPane root = SwingUtilities.getRootPane(this);
		if (root == null || root == shortcutRoot) return;
		uninstallTabAccessKeys();
		shortcutRoot = root;
		InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = root.getActionMap();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			String accessKey = tab.getAccessKey();
			if (accessKey == null || accessKey.length() != 1) continue;
			KeyStroke keyStroke = KeyStroke.getKeyStroke(Character.toUpperCase(accessKey.charAt(0)), InputEvent.ALT_DOWN_MASK);
			String actionId = accessActionId(tab.getId());
			inputMap.put(keyStroke, actionId);
				actionMap.put(actionId, new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) {
					showTab(tab.getId());
				}
			});
		}
	}

	private void uninstallTabAccessKeys() {
		if (shortcutRoot == null) return;
		InputMap inputMap = shortcutRoot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = shortcutRoot.getActionMap();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			String accessKey = tab.getAccessKey();
			if (accessKey != null && accessKey.length() == 1)
				inputMap.remove(KeyStroke.getKeyStroke(Character.toUpperCase(accessKey.charAt(0)), InputEvent.ALT_DOWN_MASK));
			actionMap.remove(accessActionId(tab.getId()));
		}
		shortcutRoot = null;
	}

	private static String accessActionId(String tabId) { return "microproject.ribbon.select." + tabId; }

	@Override
	public void doLayout() {
		super.doLayout();
		// Off-screen rendering and first layout do not necessarily dispatch a
		// component-resized event.  Re-evaluate here so the visible density never
		// depends on the component having been realized first.
		updateResponsiveMode();
	}

	private JComponent buildTabRow() {
		JPanel row = new JPanel(new GridBagLayout());
		row.setOpaque(true);
		row.setBackground(FlatUiSupport.ribbonChromeBackground());
		row.setPreferredSize(new Dimension(0, FlatUiSupport.ribbonTabHeight()));
		row.setBorder(BorderFactory.createEmptyBorder(
			0,
			FlatUiSupport.ribbonHorizontalInset(),
			0,
			FlatUiSupport.ribbonHorizontalInset()));

		JPanel tabs = buildTabsStrip();
		GridBagConstraints rowConstraints = new GridBagConstraints();
		rowConstraints.gridx = 0;
		rowConstraints.gridy = 0;
		rowConstraints.weightx = 1.0;
		rowConstraints.fill = GridBagConstraints.HORIZONTAL;
		rowConstraints.anchor = GridBagConstraints.WEST;
		row.add(tabs, rowConstraints);
		// Keep the command registrations owned by the ribbon factory for legacy
		// action-map consumers, but do not render a second QAT here.  The visible
		// QAT is owned by OfficeChromePanel, matching the MSP/Office title-bar
		// convention and preventing two competing locations.
		registerQuickAccessActions();

		return row;
	}

	private void registerQuickAccessActions() {
		for (String buttonId : model.getTaskBarButtons()) {
			SwingRibbonModel.RibbonButton specification = new SwingRibbonModel.RibbonButton(
				buttonId,
				SwingRibbonModel.ButtonPriority.LOW);
			// Keep the registered proxy styled like a ribbon command so shared
			// action-state tests and enablement propagation see the same contract.
			buttonStyler.styleActionButton(createButton(specification, true), "small");
		}
	}

	private JPanel buildTabsStrip() {
		JPanel tabs = new JPanel(new GridBagLayout());
		tabs.setOpaque(false);

		GridBagConstraints tabConstraints = new GridBagConstraints();
		tabConstraints.gridx = 0;
		tabConstraints.gridy = 0;
		tabConstraints.anchor = GridBagConstraints.WEST;
		tabConstraints.insets = new Insets(0, 0, 0, 0);
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			tabs.add(createTabButton(tab), tabConstraints);
			tabConstraints.gridx++;
		}

		tabs.add(createTabTrailingGlue(), createTrailingGlueConstraints(tabConstraints.gridx));
		return tabs;
	}

	private Component createTabTrailingGlue() {
		return Box.createHorizontalGlue();
	}

	private GridBagConstraints createTrailingGlueConstraints(int gridX) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = gridX;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return constraints;
	}

	private AbstractButton createTabButton(SwingRibbonModel.RibbonTab tab) {
		JToggleButton button = new JToggleButton(tabTitle(tab));
		tabButtons.put(tab.getId(), button);
		tabGroup.add(button);
		FlatUiSupport.styleRibbonTabButton(button);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.getModel().addChangeListener(event -> updateTabButtonAppearance(button, button.isSelected()));
		button.addActionListener(e -> showTab(tab.getId()));
		button.setVisible(!tab.isContextual() || visibleContextualTabs.contains(tab.getId()));
		if (tabBodies.isEmpty()) {
			button.setSelected(true);
		}
		updateTabButtonAppearance(button, button.isSelected());
		return button;
	}

	private void updateTabButtonAppearance(AbstractButton button, boolean selected) {
		button.setBackground(FlatUiSupport.ribbonChromeBackground());
		button.setForeground(selected ? FlatUiSupport.tabSelectedForeground() : FlatUiSupport.tabUnselectedForeground());
		button.setFont(FlatUiSupport.ribbonTabFont());
		button.setHorizontalAlignment(SwingConstants.LEFT);
	}

	private void showTab(String tabId) {
		if (!isTabVisible(tabId)) return;
		activeTabId = tabId;
		JPanel tabBody = tabBodies.get(tabId);
		if (tabBody == null || needsWidthAwareRebuild(tabId)) {
			if (tabBody != null) {
				unregisterButtons(tabBody, buttonFactory);
			}
			tabBody = createTabBody(tabId);
			tabBodies.put(tabId, tabBody);
		}
		JToggleButton tabButton = tabButtons.get(tabId);
		if (tabButton != null && !tabButton.isSelected()) {
			tabButton.setSelected(true);
		}
		cards.removeAll();
		cards.add(tabBody, BorderLayout.CENTER);
		cards.revalidate();
		cards.repaint();
		updatePreferredHeight();
	}

	private boolean isTabVisible(String tabId) {
		return model.getTabs().stream()
			.filter(tab -> tab.getId().equals(tabId))
			.findFirst()
			.map(tab -> !tab.isContextual() || visibleContextualTabs.contains(tabId))
			.orElse(false);
	}

	private String tabTitle(SwingRibbonModel.RibbonTab tab) {
		return contextualTabTitles.getOrDefault(tab.getId(), tab.getTitle());
	}

	private java.util.Optional<String> firstVisibleTabId() {
		return model.getTabs().stream().map(SwingRibbonModel.RibbonTab::getId)
			.filter(this::isTabVisible).findFirst();
	}

	private void updateResponsiveMode() {
		if (rebuildingDensity) {
			return;
		}
		RibbonDensity nextDensity = RibbonDensity.forWidth(getWidth());
		if (activeTabId == null) {
			return;
		}
		if (nextDensity == density) {
			if (needsWidthAwareRebuild(activeTabId)) {
				showTab(activeTabId);
			}
			return;
		}
		rebuildingDensity = true;
		try {
			unregisterButtons(new ArrayList<>(tabBodies.values()));
			density = nextDensity;
			tabBodies.clear();
			tabBodyBuildWidths.clear();
			bandHeights.clear();
			showTab(activeTabId);
		} finally {
			rebuildingDensity = false;
		}
	}

	private void unregisterButtons(Iterable<? extends Component> bodies) {
		if (!(buttonFactory instanceof com.microproject.menu.ExtToolBarFactory extFactory)) {
			return;
		}
		List<AbstractButton> buttons = new ArrayList<>();
		for (Component body : bodies) {
			collectButtons(body, buttons);
		}
		extFactory.unregisterButtons(buttons);
	}

	private void unregisterButtons(Component component, com.microproject.menu.ExtToolBarFactory factory) {
		List<AbstractButton> buttons = new ArrayList<>();
		collectButtons(component, buttons);
		factory.unregisterButtons(buttons);
	}

	private void collectButtons(Component component, List<AbstractButton> buttons) {
		if (component instanceof AbstractButton button) {
			buttons.add(button);
		}
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				collectButtons(child, buttons);
			}
		}
	}

	private JPanel buildTabBody(String tabId) {
		SwingRibbonModel.RibbonTab tab = model.getTabs().stream()
			.filter(candidate -> candidate.getId().equals(tabId))
			.findFirst()
			.orElseThrow();

		JPanel shell = new JPanel(new BorderLayout());
		shell.setOpaque(true);
		shell.setBackground(FlatUiSupport.panelBackground());
		// Keep the command surface visually compact.  The former asymmetric shell
		// padding made the ribbon look tall even when every band contained only
		// inline commands.
		shell.setBorder(BorderFactory.createEmptyBorder(1, 8, 2, 8));

		JPanel bandRow = new OfficeRibbonSurfacePanel();
		bandRow.setLayout(new GridBagLayout());
		bandRow.setBackground(FlatUiSupport.ribbonSurfaceColor());
		bandRow.setBorder(BorderFactory.createEmptyBorder(
			3,
			8,
			3,
			8));

		int tallestContent = 0;
		int tallestBand = 0;
		GridBagConstraints bandConstraints = new GridBagConstraints();
		bandConstraints.gridx = 0;
		bandConstraints.gridy = 0;
		bandConstraints.anchor = GridBagConstraints.NORTHWEST;
		// A ribbon is a left-to-right sequence of groups.  Do not give each group
		// elastic width: that was the source of the centred single-launcher cheat.
		bandConstraints.fill = GridBagConstraints.NONE;
		bandConstraints.weightx = 0.0;
		bandConstraints.insets = new Insets(0, 0, 0, BAND_GAP);
		List<RibbonDensity> presentations = uniformPresentation(tab.getBands().size(), RibbonDensity.FULL);
		List<RibbonBandPanel> bandPanels = addBands(bandRow, bandConstraints, tab.getBands(), presentations);
		for (RibbonDensity candidate : List.of(RibbonDensity.COMPACT)) {
			if (getWidth() <= 0 || fitsInWidth(bandPanels, getWidth(), bandRow)) break;
			presentations = uniformPresentation(tab.getBands().size(), candidate);
			bandPanels = rebuildBands(bandRow, bandConstraints, tab.getBands(), presentations, bandPanels);
		}
		// Office collapses only as many command groups as necessary.  Replacing
		// every group with a proxy wasted the available ribbon width and could leave
		// a single lonely button visible.  Preserve the left-hand groups and replace
		// trailing groups one at a time until the row fits.
		for (int index = tab.getBands().size() - 1;
			index > 0 && getWidth() > 0 && !fitsInWidth(bandPanels, getWidth(), bandRow);
			index--) {
			presentations.set(index, RibbonDensity.COLLAPSED);
			bandPanels = rebuildBands(bandRow, bandConstraints, tab.getBands(), presentations, bandPanels);
		}
		// If a remaining primary group is still too wide, reduce only that group's
		// secondary commands.  Never apply this to every group at once: that was
		// the source of the row of icon-and-ellipsis placeholders.
		for (int index = tab.getBands().size() - 1;
			index >= 0 && getWidth() > 0 && !fitsInWidth(bandPanels, getWidth(), bandRow);
			index--) {
			if (presentations.get(index) == RibbonDensity.COLLAPSED) continue;
			presentations.set(index, RibbonDensity.PRIORITY_COMPACT);
			bandPanels = rebuildBands(bandRow, bandConstraints, tab.getBands(), presentations, bandPanels);
		}
		// At an extremely narrow width no direct group can remain usable.  Collapse
		// the left-most group only as the final fallback; RibbonBandViewport keeps
		// the resulting group menus reachable from the visible left edge.
		if (!tab.getBands().isEmpty() && getWidth() > 0 && !fitsInWidth(bandPanels, getWidth(), bandRow)) {
			presentations.set(0, RibbonDensity.COLLAPSED);
			bandPanels = rebuildBands(bandRow, bandConstraints, tab.getBands(), presentations, bandPanels);
		}
		for (RibbonBandPanel bandPanel : bandPanels) {
			tallestContent = Math.max(tallestContent, bandPanel.getContentPreferredHeight());
			tallestBand = Math.max(tallestBand, bandPanel.getPreferredSize().height);
		}
		for (RibbonBandPanel bandPanel : bandPanels) {
			bandPanel.applyContentHeight(tallestContent);
			tallestBand = Math.max(tallestBand, bandPanel.getPreferredSize().height);
		}
		for (int index = 0; index < bandPanels.size(); index++) {
			bandPanels.get(index).setShowSeparator(index < bandPanels.size() - 1);
		}
		int bandRowHeight = Math.max(
			FlatUiSupport.ribbonSurfaceHeight(),
			tallestBand + 6);
		int shellHeight = bandRowHeight + 3;
		bandHeights.put(tabId, shellHeight);
		int requiredWidth = requiredWidth(bandPanels, bandRow);
		bandRow.setPreferredSize(new Dimension(requiredWidth, bandRowHeight));
		JComponent commandSurface = getWidth() > 0 && requiredWidth > getWidth()
			? new RibbonBandViewport(bandRow)
			: bandRow;
		shell.add(commandSurface, BorderLayout.CENTER);
		shell.setPreferredSize(new Dimension(0, shellHeight));
		return shell;
	}

	private JPanel createTabBody(String tabId) {
		JPanel body = buildTabBody(tabId);
		tabBodyBuildWidths.put(tabId, getWidth());
		return body;
	}

	private boolean needsWidthAwareRebuild(String tabId) {
		return getWidth() > 0 && !Objects.equals(tabBodyBuildWidths.get(tabId), Integer.valueOf(getWidth()));
	}

	private static boolean fitsInWidth(List<RibbonBandPanel> bandPanels, int availableWidth, Container container) {
		return requiredWidth(bandPanels, container) <= availableWidth;
	}

	private static int requiredWidth(List<RibbonBandPanel> bandPanels, Container container) {
		int result = container.getInsets().left + container.getInsets().right;
		for (int index = 0; index < bandPanels.size(); index++) {
			result += bandPanels.get(index).getPreferredSize().width;
			if (index > 0) result += BAND_GAP;
		}
		return result;
	}

	private static List<RibbonDensity> uniformPresentation(int bandCount, RibbonDensity presentation) {
		List<RibbonDensity> result = new ArrayList<>(bandCount);
		for (int index = 0; index < bandCount; index++) result.add(presentation);
		return result;
	}

	private List<RibbonBandPanel> rebuildBands(JPanel row, GridBagConstraints constraints,
		List<SwingRibbonModel.RibbonBand> bands, List<RibbonDensity> presentations, List<RibbonBandPanel> previous) {
		unregisterButtons(previous);
		row.removeAll();
		constraints.gridx = 0;
		return addBands(row, constraints, bands, presentations);
	}

	private List<RibbonBandPanel> addBands(Container row, GridBagConstraints constraints,
		List<SwingRibbonModel.RibbonBand> bands, List<RibbonDensity> presentations) {
		List<RibbonBandPanel> result = new ArrayList<>(bands.size());
		for (int index = 0; index < bands.size(); index++) {
			SwingRibbonModel.RibbonBand band = bands.get(index);
			RibbonBandPanel panel = buildBand(band, presentations.get(index));
			result.add(panel);
			row.add(panel, constraints);
			constraints.gridx++;
		}
		// GridBagLayout centres a grid with no weighted column.  A trailing glue
		// column consumes the unused width and keeps the complete band sequence at
		// the Office-style left origin as the window grows.
		GridBagConstraints glueConstraints = (GridBagConstraints)constraints.clone();
		glueConstraints.weightx = 1.0;
		glueConstraints.fill = GridBagConstraints.HORIZONTAL;
		glueConstraints.insets = new Insets(0, 0, 0, 0);
		row.add(Box.createHorizontalGlue(), glueConstraints);
		return result;
	}

	private RibbonBandPanel buildBand(SwingRibbonModel.RibbonBand band, RibbonDensity ribbonDensity) {
		RibbonBandPanel panel = new RibbonBandPanel();
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(1, BAND_HORIZONTAL_INSET, 0, BAND_HORIZONTAL_INSET));
		if (ribbonDensity == RibbonDensity.COLLAPSED) {
			return buildBandProxy(panel, band);
		}

		if (band.isCustomBand()) {
			return buildCustomBand(panel, band);
		}

		// The band's content occupies BorderLayout.CENTER and must expand with
		// the band.  A preferred-size-capped content panel would center the whole
		// command group when the band is widened.
		JPanel content = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		content.setOpaque(false);

		int buttonCount = band.getButtons().size();
		List<AbstractButton> smallButtonList = new ArrayList<>(buttonCount);
		List<AbstractButton> largeButtonList = new ArrayList<>(buttonCount);
		List<SwingRibbonModel.RibbonButton> overflowButtonSpecs = new ArrayList<>(buttonCount);
		for (SwingRibbonModel.RibbonButton buttonSpec : band.getButtons()) {
			if (ribbonDensity.collapses(buttonSpec.getCollapsePriority())) {
				overflowButtonSpecs.add(buttonSpec);
				continue;
			}
			AbstractButton button = createButton(buttonSpec, true);
			// The compact presentation keeps every command's label and target.  Only
			// its geometry changes: primary commands become inline controls before a
			// whole trailing group is replaced by a proxy.
			boolean keepLargePresentation = buttonSpec.getButtonSize() == SwingRibbonModel.ButtonSize.LARGE
				&& ribbonDensity == RibbonDensity.FULL;
			if (keepLargePresentation) {
				// Office keeps the primary command prominent while secondary commands
				// compress first.  At TIGHT density the same primary commands remain
				// direct targets, but use inline icons so the default desktop does not
				// collapse into a single tab launcher at high DPI.
				largeButtonList.add(buttonStyler.styleActionButton(button, "large"));
			} else if (ribbonDensity.isCompacted() || buttonSpec.getButtonSize() == SwingRibbonModel.ButtonSize.SMALL) {
				smallButtonList.add(buttonStyler.styleActionButton(button, "small"));
			} else {
				smallButtonList.add(buttonStyler.styleActionButton(button, "medium"));
			}
		}
		if (!overflowButtonSpecs.isEmpty()) {
			smallButtonList.add(buildOverflowButton(band, overflowButtonSpecs));
		}
		GridBagConstraints contentConstraints = new GridBagConstraints();
		contentConstraints.gridx = 0;
		contentConstraints.gridy = 0;
		contentConstraints.anchor = GridBagConstraints.NORTHWEST;
		contentConstraints.fill = GridBagConstraints.NONE;
		contentConstraints.insets = new Insets(0, 0, 0, 0);
		int referenceContentHeight = 0;

		if (!largeButtonList.isEmpty()) {
			JComponent largeButtonsSection = buildLargeButtonsSection(largeButtonList);
			referenceContentHeight = Math.max(referenceContentHeight, largeButtonsSection.getPreferredSize().height);
			content.add(largeButtonsSection, contentConstraints);
			contentConstraints.gridx++;
			contentConstraints.insets = new Insets(0, BAND_INNER_GAP, 0, 0);
		}
		if (!smallButtonList.isEmpty()) {
			content.add(buildSmallButtonColumns(smallButtonList, referenceContentHeight), contentConstraints);
		}
		if (largeButtonList.isEmpty() && smallButtonList.isEmpty()) {
			content.add(Box.createRigidArea(new Dimension(8, FlatUiSupport.ribbonLargeButtonHeight())), contentConstraints);
		}

		JLabel title = new JLabel(band.getTitle(), SwingConstants.CENTER);
		title.setOpaque(false);
		title.setForeground(FlatUiSupport.ribbonBandTitleForeground());
		title.setFont(FlatUiSupport.ribbonBandTitleFont());
		title.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		panel.bind(content, title, computeBandWidth(content, title));
		return panel;
	}

	private RibbonBandPanel buildCustomBand(RibbonBandPanel panel, SwingRibbonModel.RibbonBand band) {
		JPanel content = new JPanel(new BorderLayout());
		content.setOpaque(false);
		JComponent customComponent = band.getCustomBandProvider() == null ? null : band.getCustomBandProvider().createComponent();
		if (customComponent != null) {
			content.add(customComponent, BorderLayout.CENTER);
		} else {
			content.add(Box.createRigidArea(new Dimension(8, 42)), BorderLayout.CENTER);
		}

		JLabel title = new JLabel(band.getTitle(), SwingConstants.CENTER);
		title.setOpaque(false);
		title.setForeground(FlatUiSupport.ribbonBandTitleForeground());
		title.setFont(FlatUiSupport.ribbonBandTitleFont());
		title.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

		int preferredWidth = content.getPreferredSize().width;
		if (band.getPreferredWidthHint() > 0) {
			preferredWidth = Math.max(preferredWidth, band.getPreferredWidthHint());
		}
		preferredWidth = Math.max(preferredWidth, title.getPreferredSize().width + 6);
		panel.bind(content, title, Math.max(BAND_MIN_WIDTH, preferredWidth + BAND_HORIZONTAL_INSETS));
		return panel;
	}

	/**
	 * The final responsive representation is deliberately a proxy for one band,
	 * never for the selected tab.  All commands remain reachable from its popup.
	 */
	private RibbonBandPanel buildBandProxy(RibbonBandPanel panel, SwingRibbonModel.RibbonBand band) {
		JButton trigger = new JButton(band.getTitle() + " …");
		trigger.setFocusable(false);
		trigger.setToolTipText(band.getTitle());
		trigger.getAccessibleContext().setAccessibleName(band.getTitle());
		band.getButtons().stream()
			.map(SwingRibbonModel.RibbonButton::getIconKey)
			.filter(iconKey -> iconKey != null && !iconKey.isBlank())
			.findFirst().ifPresent(iconKey -> trigger.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, iconKey));
		buttonStyler.styleActionButton(trigger, "small");
		JPopupMenu popup = new JPopupMenu();
		if (band.isCustomBand()) {
			JComponent custom = band.getCustomBandProvider() == null ? null : band.getCustomBandProvider().createComponent();
			if (custom != null) popup.add(custom);
		} else {
			addTransientCommandButtons(popup, band.getButtons(), true);
		}
		trigger.putClientProperty(COLLAPSED_POPUP_PROPERTY, popup);
		trigger.putClientProperty(BAND_PROXY_PROPERTY, Boolean.TRUE);
		trigger.addActionListener(event -> popup.show(trigger, 0, trigger.getHeight()));
		JPanel content = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		content.setOpaque(false);
		content.add(trigger);
		JLabel title = new JLabel(band.getTitle(), SwingConstants.CENTER);
		title.setForeground(FlatUiSupport.ribbonBandTitleForeground());
		title.setFont(FlatUiSupport.ribbonBandTitleFont());
		panel.bind(content, title, Math.max(BAND_MIN_WIDTH,
			Math.max(content.getPreferredSize().width, title.getPreferredSize().width) + BAND_HORIZONTAL_INSETS));
		return panel;
	}

	private int computeBandWidth(JComponent content, JLabel title) {
		int contentWidth = content.getPreferredSize().width;
		int titleWidth = title.getPreferredSize().width;
		return Math.max(BAND_MIN_WIDTH, Math.max(
			contentWidth + BAND_HORIZONTAL_INSETS,
			titleWidth + BAND_HORIZONTAL_INSETS));
	}

	/**
	 * Builds commands used only while a responsive ribbon popup is open.  They
	 * must share the registered command Action without becoming another entry in
	 * the toolbar button registry.
	 */
	private void addTransientCommandButtons(Container popup, List<SwingRibbonModel.RibbonButton> specifications,
		boolean constrainWidth) {
		for (SwingRibbonModel.RibbonButton specification : specifications) {
			AbstractButton command = createButton(specification, false);
			buttonStyler.styleActionButton(command, "small");
			if (constrainWidth) {
				command.setMaximumSize(new Dimension(260, command.getPreferredSize().height));
			}
			popup.add(command);
		}
	}

	private int computeBandHeight(JComponent content, JLabel title) {
		int contentHeight = content.getPreferredSize().height;
		int titleHeight = Math.max(FlatUiSupport.ribbonBandTitleHeight(), title.getPreferredSize().height);
		int computedHeight = contentHeight + titleHeight + 1;
		return Math.max(FlatUiSupport.ribbonSurfaceHeight(), computedHeight);
	}

	private JComponent buildSmallButtonColumns(List<AbstractButton> buttons, int referenceHeight) {
		// This wrapper is placed in the band BorderLayout.CENTER.  It must be
		// allowed to expand so GridBagLayout's WEST anchor can keep the command
		// columns at the band's left edge instead of BorderLayout centering the
		// preferred-size wrapper.
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setOpaque(false);

		JPanel columns = new RibbonBandContentPanel();
		columns.setOpaque(false);
		columns.setLayout(new GridBagLayout());
		GridBagConstraints columnConstraints = new GridBagConstraints();
		columnConstraints.gridx = 0;
		columnConstraints.gridy = 0;
		columnConstraints.anchor = GridBagConstraints.NORTHWEST;
		columnConstraints.insets = new Insets(0, 0, 0, 0);

		for (int start = 0; start < buttons.size(); start += 3) {
			columns.add(buildInlineButtonColumn(buttons.subList(start, Math.min(start + 3, buttons.size()))), columnConstraints);
			columnConstraints.gridx++;
			columnConstraints.insets = new Insets(0, INLINE_COLUMN_GAP, 0, 0);
		}

		int preferredHeight = Math.max(referenceHeight, columns.getPreferredSize().height);

		GridBagConstraints wrapperConstraints = new GridBagConstraints();
		wrapperConstraints.gridx = 0;
		wrapperConstraints.gridy = 0;
		// Bands may expand to consume the ribbon width, but their command columns
		// must remain left-aligned.  Centering here makes a sparse ribbon look as
		// if the commands were detached from the left-aligned ribbon origin.
		wrapperConstraints.anchor = GridBagConstraints.WEST;
		wrapper.add(columns, wrapperConstraints);
		if (preferredHeight > 0) {
			wrapper.setPreferredSize(new Dimension(columns.getPreferredSize().width, preferredHeight));
			wrapper.setMinimumSize(new Dimension(columns.getPreferredSize().width, preferredHeight));
		}
		return wrapper;
	}

	private JComponent buildInlineButtonColumn(List<AbstractButton> buttons) {
		JPanel column = new RibbonBandContentPanel();
		column.setOpaque(false);
		column.setLayout(new GridBagLayout());
		GridBagConstraints rowConstraints = new GridBagConstraints();
		rowConstraints.gridx = 0;
		rowConstraints.gridy = 0;
		rowConstraints.anchor = GridBagConstraints.NORTHWEST;
		rowConstraints.fill = GridBagConstraints.HORIZONTAL;
		rowConstraints.insets = new Insets(0, 0, 0, 0);
		for (int index = 0; index < buttons.size(); index++) {
			AbstractButton button = buttons.get(index);
			if (buttons.size() == 1) {
				emphasizeSingleButtonColumn(button);
			}
			column.add(button, rowConstraints);
			rowConstraints.gridy++;
		}
		return column;
	}

	private JComponent buildLargeButtonsSection(List<AbstractButton> buttons) {
		JPanel section = new RibbonBandContentPanel();
		section.setOpaque(false);
		section.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(0, 0, 0, LARGE_BUTTON_GAP);
		for (AbstractButton button : buttons) {
			section.add(button, constraints);
			constraints.gridx++;
		}
		return section;
	}

	private void emphasizeSingleButtonColumn(AbstractButton button) {
		if (button == null) {
			return;
		}
		Object sizeProperty = button.getClientProperty(RibbonButtonStyler.SIZE_PROPERTY);
		if (!"small".equals(sizeProperty)) {
			return;
		}
		buttonStyler.styleActionButton(button, "medium");
	}

	private AbstractButton buildOverflowButton(SwingRibbonModel.RibbonBand band, List<SwingRibbonModel.RibbonButton> specifications) {
		JButton overflow = new JButton("…");
		overflow.setFocusable(false);
		overflow.setToolTipText(band.getTitle());
		overflow.getAccessibleContext().setAccessibleName(band.getTitle());
		FlatUiSupport.styleRibbonSmallButton(overflow);
		JPopupMenu popup = new JPopupMenu();
		addTransientCommandButtons(popup, specifications, false);
		overflow.putClientProperty(COLLAPSED_POPUP_PROPERTY, popup);
		overflow.addActionListener(event -> popup.show(overflow, 0, overflow.getHeight()));
		return overflow;
	}

	private AbstractButton createButton(SwingRibbonModel.RibbonButton specification, boolean register) {
		String buttonId = specification.getId();
		AbstractButton button;
		try {
			button = register || !(buttonFactory instanceof com.microproject.menu.ExtToolBarFactory extFactory)
				? buttonFactory.createJButton(buttonId)
				: extFactory.createUnregisteredJButton(buttonId);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to create ribbon button " + buttonId, ex);
		}
		String text = getStringOrNull(buttonId + ".text");
		if (text != null) {
			button.setText(text);
		}
		if (specification.getPresentation() == SwingRibbonModel.ButtonPresentation.SPLIT && text != null) {
			// The action already opens its chooser.  Painting the chevron in its own
			// hit-area keeps the label clean and matches Office split-button geometry.
			button.putClientProperty("MicroProject.ribbonSplit", Boolean.TRUE);
		}
		String tooltip = getStringOrNull(buttonId + ".tooltip");
		String accelerator = getStringOrNull(buttonId + ".accelerator");
		if (tooltip != null) {
			button.setToolTipText(accelerator == null || accelerator.isBlank()
				? tooltip
				: tooltip + " (" + accelerator.trim() + ")");
		}
		button.setAlignmentY(Component.TOP_ALIGNMENT);
		button.setActionCommand(buttonId);
		if (button.getAction() != null) commandActions.putIfAbsent(buttonId, button.getAction());
		if (specification.getIconKey() != null) {
			button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, specification.getIconKey());
		}
		button.putClientProperty("MicroProject.ribbonToggle", specification.isToggle());
		button.getAccessibleContext().setAccessibleName(button.getText() == null ? buttonId : button.getText());
		return button;
	}

	private String getStringOrNull(String key) {
		for (ResourceBundle bundle : bundles) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException ex) {
			}
		}
		return null;
	}

	private void updatePreferredHeight() {
		int maxBodyHeight = bandHeights.values().stream().mapToInt(Integer::intValue).max().orElse(FlatUiSupport.ribbonSurfaceHeight());
		setPreferredSize(new Dimension(0, FlatUiSupport.ribbonTabHeight() + maxBodyHeight + 1));
	}

	private static final class OfficeRibbonSurfacePanel extends JPanel {
		private OfficeRibbonSurfacePanel() {
			setOpaque(false);
			setName(RIBBON_SURFACE_COMPONENT_NAME);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				Color background = FlatUiSupport.ribbonSurfaceColor();
				Color border = FlatUiSupport.ribbonSurfaceBorderColor();
				int width = Math.max(0, getWidth() - 1);
				int height = Math.max(0, getHeight() - 3);
				int arc = Math.max(6, FlatUiSupport.ribbonCornerRadius());
				g2.setColor(new Color(0, 0, 0, 18));
				g2.fillRoundRect(1, 2, width - 1, height, arc, arc);
				g2.setColor(background);
				g2.fillRoundRect(0, 0, width, height, arc, arc);
				g2.setColor(border);
				g2.drawRoundRect(0, 0, width, height, arc, arc);
			} finally {
				g2.dispose();
			}
		}
	}

	/** Keeps the final group-proxy row reachable instead of clipping it or replacing it with a tab launcher. */
	private static final class RibbonBandViewport extends JPanel {
		private final JScrollPane scrollPane;

		private RibbonBandViewport(JComponent contents) {
			super(new BorderLayout(2, 0));
			setOpaque(false);
			scrollPane = new JScrollPane(contents, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			scrollPane.getViewport().setOpaque(false);
			JButton previous = scrollButton("‹", -1, SCROLL_PREVIOUS_PROPERTY);
			JButton next = scrollButton("›", 1, SCROLL_NEXT_PROPERTY);
			add(previous, BorderLayout.WEST);
			add(scrollPane, BorderLayout.CENTER);
			add(next, BorderLayout.EAST);
		}

		private JButton scrollButton(String label, int direction, String property) {
			JButton button = new JButton(label);
			button.setFocusable(false);
			button.putClientProperty(property, Boolean.TRUE);
			FlatUiSupport.styleRibbonSmallButton(button);
			button.addActionListener(event -> {
				int extent = scrollPane.getViewport().getExtentSize().width;
				java.awt.Point position = scrollPane.getViewport().getViewPosition();
				position.x = Math.max(0, position.x + direction * Math.max(48, extent * 3 / 4));
				scrollPane.getViewport().setViewPosition(position);
			});
			return button;
		}
	}

	private static final class RibbonBandContentPanel extends JPanel {
		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}
	}

	private static final class RibbonBandPanel extends JPanel {
		private JComponent content;
		private JLabel title;
		private int targetWidth;
		private int contentPreferredHeight;
		private boolean showSeparator;

		private RibbonBandPanel() {
			super(new BorderLayout());
			setOpaque(false);
			setName(RIBBON_BAND_COMPONENT_NAME);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			if (!showSeparator) return;
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setColor(FlatUiSupport.ribbonBandSeparatorColor());
				int x = getWidth() - 1;
				g2.drawLine(x, 5, x, Math.max(5, getHeight() - 18));
			} finally {
				g2.dispose();
			}
		}

		private void setShowSeparator(boolean showSeparator) {
			this.showSeparator = showSeparator;
		}

		private void bind(JComponent content, JLabel title, int targetWidth) {
			this.content = content;
			this.title = title;
			this.targetWidth = targetWidth;
			this.contentPreferredHeight = content.getPreferredSize().height;
			add(content, BorderLayout.CENTER);
			add(title, BorderLayout.SOUTH);
			updatePreferredSize(contentPreferredHeight);
		}

		private int getContentPreferredHeight() {
			return contentPreferredHeight;
		}

		private void applyContentHeight(int height) {
			if (content == null || title == null) {
				return;
			}
			contentPreferredHeight = Math.max(contentPreferredHeight, height);
			content.setPreferredSize(new Dimension(content.getPreferredSize().width, contentPreferredHeight));
			updatePreferredSize(contentPreferredHeight);
		}

		private void updatePreferredSize(int targetContentHeight) {
			int titleHeight = Math.max(FlatUiSupport.ribbonBandTitleHeight(), title == null ? 0 : title.getPreferredSize().height);
			int width = Math.max(BAND_MIN_WIDTH, targetWidth);
			// The outer ribbon owns the surface height.  A band only contributes its
			// content and caption; giving every band the full surface height caused
			// nested padding and the oversized card-strip appearance.
			int height = targetContentHeight + titleHeight + 2;
			setPreferredSize(new Dimension(width, height));
			setMinimumSize(new Dimension(width, height));
		}
	}
}
