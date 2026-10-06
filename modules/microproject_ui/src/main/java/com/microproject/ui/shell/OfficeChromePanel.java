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
package com.microproject.ui.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.IconManager;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;

final class OfficeChromePanel extends JPanel {
	static final String NAME = "officeChromePanel";
	static final String AUTO_SAVE_NAME = "officeChromeAutoSave";
	static final String SEARCH_BOX_NAME = "officeChromeSearchBox";
	static final String SEARCH_FIELD_NAME = "officeChromeSearchField";
	static final String DOCUMENT_TITLE_NAME = "officeChromeDocumentTitle";
	static final String QUICK_ACCESS_NAME = "officeChromeQuickAccess";
	static final String QUICK_ACCESS_COMMANDS_NAME = "officeChromeQuickAccessCommands";
	static final String RIGHT_ACTIONS_NAME = "officeChromeRightActions";
	static final String HELP_BUTTON_NAME = "officeChromeHelpButton";
	static final String RIBBON_DISPLAY_OPTIONS_NAME = "officeChromeRibbonDisplayOptions";
	static final String RIBBON_DISPLAY_OPTIONS_POPUP_NAME = "officeChromeRibbonDisplayOptionsPopup";
	static final String RIBBON_SURFACE_NAME = "officeChromeRibbonSurface";
	static final String RIBBON_OPTIONS_ROW_NAME = "officeChromeRibbonOptionsRow";
	static final String WINDOW_BUTTONS_PLACEHOLDER_NAME = "officeChromeWindowButtonsPlaceholder";
	static final String BRAND_ICON_NAME = "officeChromeBrandIcon";

	private static final Color CHROME_BACKGROUND = FlatUiSupport.officeTitleBarBackground();
	private static final Color BORDER_COLOR = FlatUiSupport.ribbonSurfaceBorderColor();
	private static final Color TEXT_COLOR = FlatUiSupport.officeTitleBarForeground();
	private static final Color ACCENT_COLOR = FlatUiSupport.accentColor();
	private static final Dimension ICON_BUTTON_SIZE = new Dimension(
		FlatUiSupport.ribbonQuickAccessButtonSize(),
		FlatUiSupport.ribbonQuickAccessButtonSize());
	private static final int CLUSTER_GAP = 8;
	private static final Dimension AUTOSAVE_SIZE = new Dimension(36, 18);
	private static final int QUICK_ACCESS_ICON_SIZE = 16;
	private enum ActionIconPresentation {
		TITLE_BAR_MONOCHROME,
		PRESERVE_SOURCE_COLORS
	}

	private final MenuManager menuManager;
	private final boolean officeWindow;
	private final Runnable helpAction;
	private final JTextField searchField;
	private final JComponent searchBox;
	private final JLabel documentTitleLabel;
	private final AutoSaveControl autoSaveControl;
	private final OfficeChromeTitleBinding titleBinding;
	private final RibbonController ribbonController;
	private final JComponent header;
	private final JPanel quickAccessCommands;
	private final JPanel ribbonOptionsRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
	private AbstractButton autoHideOptionsButton;
	private boolean autoHideRevealClick;
	private boolean autoHideRevealActionSeen;

	OfficeChromePanel(MenuManager menuManager, JComponent ribbonPanel, Runnable helpAction) {
		this(null, menuManager, ribbonPanel, helpAction, AutoSaveControl.DISABLED);
	}

	OfficeChromePanel(MenuManager menuManager, JComponent ribbonPanel, Runnable helpAction, AutoSaveControl autoSaveControl) {
		this(null, menuManager, ribbonPanel, helpAction, autoSaveControl);
	}

	OfficeChromePanel(JFrame frame, MenuManager menuManager, JComponent ribbonPanel, Runnable helpAction,
		AutoSaveControl autoSaveControl) {
		super(new BorderLayout());
		this.menuManager = menuManager;
		this.officeWindow = frame != null && Environment.isWindows()
			&& FlatLafSupport.isNativeWindowDecorationsEnabled();
		this.helpAction = helpAction;
		this.autoSaveControl = autoSaveControl == null ? AutoSaveControl.DISABLED : autoSaveControl;
		Object ribbonValue = ribbonPanel == null ? null
			: ribbonPanel.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		this.ribbonController = ribbonValue instanceof RibbonController ribbon ? ribbon : null;
		this.quickAccessCommands = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
		this.quickAccessCommands.setName(QUICK_ACCESS_COMMANDS_NAME);
		this.quickAccessCommands.setOpaque(false);
		this.searchField = new JTextField(28);
		this.searchBox = buildSearchBox();
		this.documentTitleLabel = createDocumentTitleLabel(frame == null ? "" : frame.getTitle());
		this.titleBinding = frame == null ? null : OfficeChromeTitleBinding.attach(frame, this::updateDocumentTitle);
		setName(NAME);
		setOpaque(true);
		setBackground(CHROME_BACKGROUND);
		// The header is the only draggable caption surface.  Explicitly exclude
		// the ribbon from FlatLaf's native caption hit testing so physical clicks
		// reach Swing command buttons on the full-window-content shell.
		ribbonPanel.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		header = buildHeader();
		header.setName("officeChromeHeader");
		add(header, BorderLayout.NORTH);
		add(buildRibbonSurface(ribbonPanel), BorderLayout.CENTER);
		if (ribbonController != null) {
			ribbonController.addRibbonDisplayModeListener(mode -> {
				ribbonOptionsRow.setVisible(mode != RibbonDisplayMode.AUTO_HIDE);
				autoHideOptionsButton.setVisible(mode == RibbonDisplayMode.AUTO_HIDE);
			});
			ribbonOptionsRow.setVisible(ribbonController.getRibbonDisplayMode() != RibbonDisplayMode.AUTO_HIDE);
		}
	}

	JComponent getHeaderComponent() {
		return header;
	}

	private JComponent buildRibbonSurface(JComponent ribbonPanel) {
		JLayeredPane surface = new JLayeredPane() {
			@Override public Dimension getPreferredSize() {
				Dimension ribbonSize = ribbonPanel.isVisible() ? ribbonPanel.getPreferredSize() : new Dimension();
				Dimension optionsSize = ribbonOptionsRow.isVisible() ? ribbonOptionsRow.getPreferredSize() : new Dimension();
				return new Dimension(Math.max(ribbonSize.width, optionsSize.width),
					Math.max(ribbonSize.height, optionsSize.height));
			}

			@Override public void doLayout() {
				ribbonPanel.setBounds(0, 0, getWidth(), getHeight());
				Dimension optionsSize = ribbonOptionsRow.getPreferredSize();
				ribbonOptionsRow.setBounds(Math.max(0, getWidth() - optionsSize.width),
					Math.max(0, getHeight() - optionsSize.height), Math.min(getWidth(), optionsSize.width),
					Math.min(getHeight(), optionsSize.height));
			}
		};
		surface.setName(RIBBON_SURFACE_NAME);
		surface.setOpaque(false);
		surface.add(ribbonPanel, JLayeredPane.DEFAULT_LAYER);
		ribbonOptionsRow.setOpaque(false);
		ribbonOptionsRow.setName(RIBBON_OPTIONS_ROW_NAME);
		ribbonOptionsRow.setBorder(new EmptyBorder(0, 0, 2, FlatUiSupport.ribbonHorizontalInset()));
		ribbonOptionsRow.add(createRibbonDisplayOptionsButton());
		surface.add(ribbonOptionsRow, JLayeredPane.PALETTE_LAYER);
		return surface;
	}

	private JComponent buildHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(true);
		header.setBackground(CHROME_BACKGROUND);
		header.setPreferredSize(new Dimension(0, FlatUiSupport.ribbonChromeHeight()));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FlatUiSupport.ribbonTopLineColor()));
		JComponent content = buildHeaderContent();
		// FlatLaf owns caption dragging. Child controls continue to receive their
		// normal mouse events; only the unhandled header surface is draggable.
		header.putClientProperty("JComponent.titleBarCaption", Boolean.TRUE);
		header.add(content, BorderLayout.CENTER);
		return header;
	}

	private JComponent buildHeaderContent() {
		JPanel content = new JPanel(new GridBagLayout()) {
			@Override public void doLayout() {
				// Keep the title/search controls only while the measured preferred
				// widths of all three header clusters fit. This protects native
				// window buttons without a locale- or DPI-specific width threshold.
				JComponent center = (JComponent) getComponent(1);
				documentTitleLabel.setVisible(true);
				center.setVisible(true);
				if (!headerClustersFit(this)) documentTitleLabel.setVisible(false);
				if (!headerClustersFit(this)) center.setVisible(false);
				super.doLayout();
			}
		};
		content.setOpaque(false);
		// Only the document title and brand icon opt into caption dragging.
		// Mark the interactive layout host as non-caption so FlatLaf does not
		// inherit the header's draggable hit area for QAT, search, or Help.
		content.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		content.setBorder(new EmptyBorder(
			FlatUiSupport.ribbonChromeVerticalInset(),
			FlatUiSupport.ribbonHorizontalInset(),
			FlatUiSupport.ribbonChromeVerticalInset(),
			FlatUiSupport.ribbonHorizontalInset()));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 0, 8);
		content.add(buildLeftCluster(), constraints);

		constraints.gridx = 1;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.insets = new Insets(0, 0, 0, 8);
		content.add(buildCenterCluster(), constraints);

		constraints.gridx = 2;
		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(0, 0, 0, 0);
		content.add(buildRightCluster(), constraints);
		return content;
	}

	private static boolean headerClustersFit(JPanel content) {
		int requiredWidth = content.getInsets().left + content.getInsets().right + 16;
		for (java.awt.Component component : content.getComponents())
			if (component.isVisible()) requiredWidth += component.getPreferredSize().width;
		return content.getWidth() >= requiredWidth;
	}

	private JComponent buildLeftCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		cluster.setName(QUICK_ACCESS_NAME);
		cluster.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 0, 6);
		if (officeWindow) {
			cluster.add(createBrandIcon(), constraints);
			constraints.gridx++;
		}
		constraints.insets = new Insets(0, 0, 0, 4);
		cluster.add(createLabel(UsabilityStrings.text("chrome.autoSave"), TEXT_COLOR), constraints);
		constraints.gridx++;
		cluster.add(new OfficeSwitchButton(autoSaveControl), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, CLUSTER_GAP, 0, 0);
		refreshQuickAccessCommands();
		quickAccessCommands.setVisible(RibbonDisplayPreferences.loadQuickAccessVisible());
		cluster.add(quickAccessCommands, constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 12, 0, 0);
		cluster.add(documentTitleLabel, constraints);
		return cluster;
	}

	private JLabel createBrandIcon() {
		ImageIcon icon = new ImageIcon(IconManager.getImage("application.icon.small"));
		JLabel label = new JLabel(icon);
		label.setName(BRAND_ICON_NAME);
		label.setToolTipText("microProject");
		label.setPreferredSize(new Dimension(18, 18));
		label.setMinimumSize(new Dimension(18, 18));
		label.setMaximumSize(new Dimension(18, 18));
		label.putClientProperty("JComponent.titleBarCaption", Boolean.TRUE);
		return label;
	}

	private JLabel createDocumentTitleLabel(String title) {
		JLabel label = createLabel(OfficeChromeTitleBinding.compactDocumentTitle(title), TEXT_COLOR);
		label.setName(DOCUMENT_TITLE_NAME);
		label.setToolTipText(title);
		label.setPreferredSize(new Dimension(220, 22));
		label.setMinimumSize(new Dimension(80, 22));
		label.setMaximumSize(new Dimension(240, 22));
		label.putClientProperty("JComponent.titleBarCaption", Boolean.TRUE);
		return label;
	}

	private void updateDocumentTitle(String title) {
		documentTitleLabel.setText(OfficeChromeTitleBinding.compactDocumentTitle(title));
		documentTitleLabel.setToolTipText(title);
	}

	static String compactDocumentTitle(String title) {
		return OfficeChromeTitleBinding.compactDocumentTitle(title);
	}

	private JComponent buildCenterCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.CENTER;
		cluster.add(searchBox, constraints);
		return cluster;
	}

	private JComponent buildRightCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		cluster.setName(RIGHT_ACTIONS_NAME);
		cluster.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.insets = new Insets(0, 0, 0, 4);
		// Keep a second access point while full-screen mode hides the ribbon surface.
		autoHideOptionsButton = createRibbonDisplayOptionsButton();
		autoHideOptionsButton.setVisible(ribbonController != null
			&& ribbonController.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE);
		cluster.add(autoHideOptionsButton, constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 0, 0, 4);
		cluster.add(createHelpButton(), constraints);
		if (officeWindow) {
			constraints.gridx++;
			constraints.insets = new Insets(0, 0, 0, 0);
			cluster.add(createWindowButtonsPlaceholder(), constraints);
		}
		return cluster;
	}

	private AbstractButton createRibbonDisplayOptionsButton() {
		OfficeIconButton button = new OfficeIconButton(GlyphIcon.ribbonDisplayOptions(), RIBBON_DISPLAY_OPTIONS_NAME, false);
		button.putClientProperty(RibbonController.AUTO_HIDE_REVEAL_CONTROL_PROPERTY, Boolean.TRUE);
		button.setToolTipText(UsabilityStrings.text("chrome.ribbonDisplayOptions"));
		button.addActionListener(event -> {
			if (autoHideRevealClick) {
				revealAutoHiddenRibbon();
				autoHideRevealActionSeen = true;
			} else if (isAutoHideCollapsed()) {
				revealAutoHiddenRibbon();
			} else {
				showRibbonDisplayOptions(button);
			}
		});
		// FlatLaf's full-window-content caption hit testing can consume the
		// release that would normally drive JButton's action listener.  Keep the
		// command on the same canonical popup method and recover only when the
		// physical release did not already open it.
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent event) {
				autoHideRevealClick = javax.swing.SwingUtilities.isLeftMouseButton(event) && isAutoHideCollapsed();
				autoHideRevealActionSeen = false;
			}

			@Override public void mouseReleased(java.awt.event.MouseEvent event) {
				if (autoHideRevealClick) {
					if (!autoHideRevealActionSeen) revealAutoHiddenRibbon();
					clearAutoHideRevealClickAfterDispatch();
					return;
				}
				if (javax.swing.SwingUtilities.isLeftMouseButton(event) && isAutoHideCollapsed()) {
					autoHideRevealClick = true;
					revealAutoHiddenRibbon();
					clearAutoHideRevealClickAfterDispatch();
					return;
				}
				if (javax.swing.SwingUtilities.isLeftMouseButton(event)
					&& !isRibbonDisplayOptionsPopupVisible()) {
					showRibbonDisplayOptions(button);
				}
			}
		});
		return button;
	}

	private boolean isAutoHideCollapsed() {
		return ribbonController != null && ribbonController.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE
			&& !ribbonController.isCommandSurfaceVisible();
	}

	private void clearAutoHideRevealClickAfterDispatch() {
		javax.swing.SwingUtilities.invokeLater(() -> {
			autoHideRevealClick = false;
			autoHideRevealActionSeen = false;
		});
	}

	private void revealAutoHiddenRibbon() {
		if (ribbonController != null) ribbonController.revealAutoHiddenRibbon();
	}

	private void showRibbonDisplayOptions(AbstractButton button) {
		if (isRibbonDisplayOptionsPopupVisible()) return;
		RibbonController ribbon = findRibbonController();
		if (ribbon == null) return;
		JPopupMenu popup = new JPopupMenu();
		popup.setName(RIBBON_DISPLAY_OPTIONS_POPUP_NAME);
		JLabel heading = new JLabel(UsabilityStrings.text("chrome.ribbonShow"));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		heading.setForeground(TEXT_COLOR);
		heading.setBorder(new EmptyBorder(5, 12, 5, 12));
		popup.add(heading);
		addRibbonDisplayItem(popup, ribbon, RibbonDisplayMode.AUTO_HIDE, "chrome.ribbonAutoHide");
		addRibbonDisplayItem(popup, ribbon, RibbonDisplayMode.TABS_ONLY, "chrome.ribbonTabsOnly");
		addRibbonDisplayItem(popup, ribbon, RibbonDisplayMode.ALWAYS_SHOW, "chrome.ribbonAlwaysShow");
		popup.addSeparator();
		boolean quickAccessVisible = RibbonDisplayPreferences.loadQuickAccessVisible();
		JMenuItem quickAccessItem = new JMenuItem(UsabilityStrings.text(quickAccessVisible
			? "chrome.ribbonHideQuickAccess" : "chrome.ribbonShowQuickAccess"));
		quickAccessItem.addActionListener(event -> {
			boolean visible = !quickAccessCommands.isVisible();
			quickAccessCommands.setVisible(visible);
			RibbonDisplayPreferences.saveQuickAccessVisible(visible);
		});
		popup.add(quickAccessItem);
		JMenuItem customizeQuickAccessItem = new JMenuItem(UsabilityStrings.text("chrome.customizeQuickAccess"));
		customizeQuickAccessItem.addActionListener(event -> customizeQuickAccess());
		popup.add(customizeQuickAccessItem);
		JMenuItem resetQuickAccessItem = new JMenuItem(UsabilityStrings.text("chrome.resetQuickAccess"));
		resetQuickAccessItem.addActionListener(event -> resetQuickAccess());
		popup.add(resetQuickAccessItem);
		popup.show(button, button.getWidth() - popup.getPreferredSize().width, button.getHeight());
	}

	private void resetQuickAccess() {
		int result = JOptionPane.showConfirmDialog(this,
			UsabilityStrings.text("chrome.confirmResetQuickAccess"),
			UsabilityStrings.text("chrome.resetQuickAccess"), JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.OK_OPTION) return;
		RibbonDisplayPreferences.resetQuickAccessCommands();
		refreshQuickAccessCommands();
	}

	private void customizeQuickAccess() {
		if (menuManager == null) return;
		java.util.List<String> supported = new java.util.ArrayList<>();
		for (String id : RibbonDisplayPreferences.quickAccessCandidateCommands()) {
			if (menuManager.getActionFromId(id) != null) supported.add(id);
		}
		for (String id : RibbonDisplayPreferences.loadQuickAccessCommands()) {
			if (menuManager.getActionFromId(id) != null && !supported.contains(id)) supported.add(id);
		}
		java.util.List<JCheckBox> checks = new java.util.ArrayList<>(supported.size());
		java.util.Set<String> selected = new java.util.LinkedHashSet<>(RibbonDisplayPreferences.loadQuickAccessCommands());
		java.awt.GridLayout choiceLayout = new java.awt.GridLayout(0, 1, 0, 2);
		JPanel choices = new JPanel(choiceLayout);
		choices.setBorder(new EmptyBorder(6, 8, 6, 8));
		for (String id : supported) {
			JCheckBox check = new JCheckBox(resolveTooltip(id), selected.contains(id));
			check.setName("quickAccessChoice." + id);
			checks.add(check);
			choices.add(check);
		}
		JScrollPane scroll = new JScrollPane(choices);
		int rowHeight = checks.stream().mapToInt(check -> check.getPreferredSize().height).max().orElse(16);
		scroll.getVerticalScrollBar().setUnitIncrement(Math.max(1, rowHeight + choiceLayout.getVgap()));
		scroll.setPreferredSize(new Dimension(380, Math.min(440, Math.max(180, supported.size() * 29))));
		int result = JOptionPane.showConfirmDialog(this, scroll,
			UsabilityStrings.text("chrome.customizeQuickAccess"), JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) return;
		java.util.Set<String> selectedCommands = new java.util.LinkedHashSet<>();
		for (int index = 0; index < checks.size(); index++) {
			if (checks.get(index).isSelected()) selectedCommands.add(supported.get(index));
		}
		java.util.List<String> commands = RibbonDisplayPreferences.orderQuickAccessSelection(
			RibbonDisplayPreferences.loadQuickAccessCommands(), supported, selectedCommands);
		RibbonDisplayPreferences.saveQuickAccessCommands(commands);
		refreshQuickAccessCommands();
	}

	private void refreshQuickAccessCommands() {
		java.util.List<JComponent> previousControls = java.util.Arrays.stream(quickAccessCommands.getComponents())
			.filter(JComponent.class::isInstance).map(JComponent.class::cast).toList();
		if (menuManager != null && menuManager.getRibbonFactory() != null) {
			menuManager.getRibbonFactory().unregisterRibbonControls(previousControls);
		}
		previousControls.stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.forEach(button -> button.setAction(null));
		quickAccessCommands.removeAll();
		java.util.List<String> visibleCommands = RibbonDisplayPreferences.loadQuickAccessCommands().stream()
			.filter(id -> menuManager != null && menuManager.getActionFromId(id) != null).toList();
		if (!visibleCommands.isEmpty()) quickAccessCommands.add(new VerticalDivider());
		for (String id : visibleCommands) quickAccessCommands.add(createActionButton(id));
		quickAccessCommands.setVisible(RibbonDisplayPreferences.loadQuickAccessVisible());
		quickAccessCommands.revalidate();
		quickAccessCommands.repaint();
	}

	private boolean isRibbonDisplayOptionsPopupVisible() {
		return java.util.Arrays.stream(javax.swing.MenuSelectionManager.defaultManager().getSelectedPath())
			.filter(JPopupMenu.class::isInstance)
			.map(JPopupMenu.class::cast)
			.anyMatch(popup -> RIBBON_DISPLAY_OPTIONS_POPUP_NAME.equals(popup.getName()));
	}

	private void addRibbonDisplayItem(JPopupMenu popup, RibbonController ribbon, RibbonDisplayMode mode, String textKey) {
		javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(UsabilityStrings.text(textKey),
			ribbon.getRibbonDisplayMode() == mode);
		item.addActionListener(event -> ribbon.setRibbonDisplayMode(mode));
		popup.add(item);
	}

	private RibbonController findRibbonController() {
		if (ribbonController != null) return ribbonController;
		for (java.awt.Component component : getComponents()) {
			if (component instanceof JComponent child) {
				Object value = child.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
				if (value instanceof RibbonController ribbon) return ribbon;
			}
		}
		return null;
	}

	private JComponent createWindowButtonsPlaceholder() {
		JPanel placeholder = new JPanel();
		placeholder.setName(WINDOW_BUTTONS_PLACEHOLDER_NAME);
		placeholder.setOpaque(false);
		placeholder.putClientProperty("FlatLaf.fullWindowContent.buttonsPlaceholder", "win horizontal");
		return placeholder;
	}

	private JComponent buildSearchBox() {
		JPanel box = new SearchBoxPanel();
		box.setName(SEARCH_BOX_NAME);
		box.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		box.setLayout(new BorderLayout(4, 0));
		box.setBorder(new EmptyBorder(1, 8, 1, 8));
		box.setMinimumSize(new Dimension(180, FlatUiSupport.ribbonSearchHeight()));
		box.setPreferredSize(new Dimension(
			FlatUiSupport.ribbonSearchPreferredWidth(),
			FlatUiSupport.ribbonSearchHeight()));
		box.setMaximumSize(new Dimension(
			FlatUiSupport.ribbonSearchMaxWidth(),
			FlatUiSupport.ribbonSearchHeight()));
		box.setBorder(new EmptyBorder(0, 4, 0, 4));

		AbstractButton searchButton = createGlyphButton("Search", GlyphIcon.search(), false, SEARCH_BOX_NAME + "Button");
		searchButton.addActionListener(event -> triggerFindAction());
		searchButton.setToolTipText(UsabilityStrings.text("chrome.search"));
		int searchButtonSize = Math.max(18, FlatUiSupport.ribbonSearchHeight() - 6);
		searchButton.setPreferredSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMinimumSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMaximumSize(new Dimension(searchButtonSize, searchButtonSize));

		searchField.setName(SEARCH_FIELD_NAME);
		searchField.putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
		searchField.setFocusable(true);
		searchField.setRequestFocusEnabled(true);
		searchField.putClientProperty("JTextField.placeholderText", UsabilityStrings.text("chrome.search"));
		searchField.setBorder(BorderFactory.createEmptyBorder());
		searchField.setOpaque(false);
		searchField.setFont(FlatUiSupport.ribbonChromeLabelFont());
		searchField.addActionListener(event -> triggerFindAction());
		searchField.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override public void focusGained(java.awt.event.FocusEvent event) { box.repaint(); }
			@Override public void focusLost(java.awt.event.FocusEvent event) { box.repaint(); }
		});
		box.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent event) { focusSearchField(); }
		});
		searchField.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent event) { focusSearchField(); }
		});

		box.add(searchButton, BorderLayout.WEST);
		box.add(searchField, BorderLayout.CENTER);
		return box;
	}

	private void focusSearchField() {
		searchField.requestFocusInWindow();
	}

	private JLabel createLabel(String text, Color color) {
		JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setFont(FlatUiSupport.ribbonChromeLabelFont());
		label.setBorder(new EmptyBorder(0, 0, 0, 0));
		return label;
	}

	private AbstractButton createActionButton(String actionId) {
		Icon icon = resolveActionIcon(actionId, QUICK_ACCESS_ICON_SIZE);
		OfficeIconButton button = new OfficeIconButton(icon, actionId, false);
		Action action = menuManager == null ? null : menuManager.getActionFromId(actionId);
		if (action != null) {
			button.setAction(action);
			// AbstractButton#setAction copies the Action's icon property and may
			// clear the QAT icon when the command action has no icon of its own.
			// The Office title-bar QAT owns this presentation, so restore it after
			// wiring the shared action.
			button.setIcon(icon);
		}
		button.setText("");
		button.setName(actionId);
		button.setActionCommand(actionId);
		button.setToolTipText(resolveTooltip(actionId));
		if (menuManager != null && menuManager.getRibbonFactory() != null)
			menuManager.getRibbonFactory().registerRibbonControl(actionId, button);
		return button;
	}

	private AbstractButton createGlyphButton(String tooltip, Icon icon, boolean active, String name) {
		OfficeIconButton button = new OfficeIconButton(icon, tooltip, active);
		button.setName(name);
		button.setToolTipText(tooltip);
		return button;
	}

	private AbstractButton createHelpButton() {
		String help = UsabilityStrings.text("chrome.help");
		OfficeIconButton button = new OfficeIconButton(resolveActionIcon(
			"RibbonProjectLibreDocumentation", QUICK_ACCESS_ICON_SIZE, ActionIconPresentation.PRESERVE_SOURCE_COLORS), help, false);
		button.setName(HELP_BUTTON_NAME);
		button.setToolTipText(help);
		button.addActionListener(event -> {
			if (helpAction != null) {
				helpAction.run();
			}
		});
		return button;
	}

	private void triggerFindAction() {
		if (menuManager == null) {
			return;
		}
		Action action = menuManager.getActionFromId("RibbonFind");
		if (action != null) {
			action.actionPerformed(new ActionEvent(searchField, ActionEvent.ACTION_PERFORMED, searchField.getText()));
		}
	}

	private Icon resolveActionIcon(String actionId, int iconSize) {
		return resolveActionIcon(actionId, iconSize, ActionIconPresentation.TITLE_BAR_MONOCHROME);
	}

	private Icon resolveActionIcon(String actionId, int iconSize, ActionIconPresentation presentation) {
		if (menuManager != null) {
			String iconName = menuManager.getStringOrNull(actionId + ".icon");
			if (iconName != null) {
				Icon icon = presentation == ActionIconPresentation.PRESERVE_SOURCE_COLORS
					? IconManager.getRibbonIcon(iconName, iconSize, iconSize)
					: IconManager.getRibbonIconTinted(iconName, iconSize, iconSize,
						FlatUiSupport.officeTitleBarForeground());
				if (icon == null)
					icon = IconManager.getRibbonIcon(iconName, iconSize, iconSize);
				if (icon != null) {
					return icon;
				}
			}
		}
		return GlyphIcon.fallback(actionId);
	}

	private String resolveTooltip(String actionId) {
		if (menuManager == null) {
			return actionId;
		}
		String tooltip = menuManager.getFullTipText(actionId);
		return tooltip == null ? actionId : tooltip;
	}

	private final class SearchBoxPanel extends JPanel {
		private SearchBoxPanel() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				int inset = 0;
				int arc = FlatUiSupport.ribbonCornerRadius();
				RoundRectangle2D.Double outline = new RoundRectangle2D.Double(
					inset + 0.5d, inset + 0.5d, getWidth() - 1d, getHeight() - 1d, arc, arc);
				g2.setColor(FlatUiSupport.ribbonSurfaceColor());
				g2.fill(outline);
				g2.setColor(searchField.isFocusOwner() ? ACCENT_COLOR : BORDER_COLOR);
				g2.draw(outline);
			} finally {
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}

	private static final class VerticalDivider extends JComponent {
		private VerticalDivider() {
			setPreferredSize(new Dimension(1, 16));
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(BORDER_COLOR);
			int x = getWidth() / 2;
			g.drawLine(x, 2, x, getHeight() - 2);
		}
	}

	private static final class OfficeIconButton extends JButton {
		private final boolean active;

		private OfficeIconButton(Icon icon, String name, boolean active) {
			super();
			this.active = active;
			setIcon(icon);
			setName(name);
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setRolloverEnabled(true);
			// The enclosing header is a draggable title-bar caption. Explicitly
			// opt interactive controls out so native/FlatLaf hit testing delivers
			// physical clicks to the button instead of the window caption.
			putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
			setMargin(new Insets(0, 0, 0, 0));
			setFocusable(false);
			setHorizontalAlignment(SwingConstants.CENTER);
			setPreferredSize(ICON_BUTTON_SIZE);
			setMinimumSize(ICON_BUTTON_SIZE);
			setMaximumSize(ICON_BUTTON_SIZE);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				boolean selected = isSelected() || getAction() != null
					&& Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY));
				if (getModel().isPressed()) {
					g2.setColor(FlatUiSupport.chromeButtonPressedBackground());
					g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, FlatUiSupport.ribbonButtonArc(), FlatUiSupport.ribbonButtonArc());
				} else if (getModel().isRollover() || active || selected) {
					g2.setColor(active || selected ? FlatUiSupport.chromeButtonActiveBackground() : FlatUiSupport.chromeButtonHoverBackground());
					g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, FlatUiSupport.ribbonButtonArc(), FlatUiSupport.ribbonButtonArc());
				}
			} finally {
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}

	private static final class OfficeSwitchButton extends JToggleButton {
		private OfficeSwitchButton(AutoSaveControl control) {
			super();
			setName(AUTO_SAVE_NAME);
			setSelected(control.isEnabled());
			addActionListener(event -> control.setEnabled(isSelected()));
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setRolloverEnabled(true);
			putClientProperty("JComponent.titleBarCaption", Boolean.FALSE);
			setFocusable(false);
			setPreferredSize(AUTOSAVE_SIZE);
			setMinimumSize(AUTOSAVE_SIZE);
			setMaximumSize(AUTOSAVE_SIZE);
			setToolTipText(UsabilityStrings.text("chrome.autoSave"));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				Color track = isSelected() ? ACCENT_COLOR : FlatUiSupport.switchTrackBackground();
				g2.setColor(track);
				g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
				int knobDiameter = 14;
				int x = isSelected() ? getWidth() - knobDiameter - 3 : 3;
				int y = (getHeight() - knobDiameter) / 2;
				g2.setColor(Color.WHITE);
				g2.fillOval(x, y, knobDiameter, knobDiameter);
				g2.setColor(new Color(0, 0, 0, 32));
				g2.drawOval(x, y, knobDiameter, knobDiameter);
			} finally {
				g2.dispose();
			}
		}
	}

	private static final class GlyphIcon implements Icon {
		private enum Kind {
			COMMENT,
			SHARE,
			PROFILE,
			RIBBON_DISPLAY_OPTIONS
		}

		private final Kind kind;
		private final int size;

		private GlyphIcon(Kind kind, int size) {
			this.kind = kind;
			this.size = size;
		}

		static Icon search() {
			Icon icon = IconManager.getRibbonIcon("ribbon.find", 16, 16);
			return icon == null ? fallback("search") : icon;
		}

		static Icon comment() {
			return new GlyphIcon(Kind.COMMENT, 16);
		}

		static Icon share() {
			return new GlyphIcon(Kind.SHARE, 16);
		}

		static Icon profile() {
			return new GlyphIcon(Kind.PROFILE, 16);
		}

		static Icon ribbonDisplayOptions() {
			return new GlyphIcon(Kind.RIBBON_DISPLAY_OPTIONS, 16);
		}

		static Icon fallback(String hint) {
			Icon icon = IconManager.getIcon("question");
			if (icon != null) {
				return icon;
			}
			return new GlyphIcon(Kind.COMMENT, 16);
		}

		@Override
		public int getIconWidth() {
			return size;
		}

		@Override
		public int getIconHeight() {
			return size;
		}

		@Override
		public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				g2.setColor(FlatUiSupport.ribbonIconColor());
				switch (kind) {
					case COMMENT -> paintComment(g2, x, y);
					case SHARE -> paintShare(g2, x, y);
					case PROFILE -> paintProfile(g2, x, y);
					case RIBBON_DISPLAY_OPTIONS -> paintRibbonDisplayOptions(g2, x, y);
				}
			} finally {
				g2.dispose();
			}
		}

		private void paintComment(Graphics2D g2, int x, int y) {
			Path2D bubble = new Path2D.Double();
			bubble.moveTo(x + 2, y + 4);
			bubble.lineTo(x + 14, y + 4);
			bubble.curveTo(x + 15, y + 4, x + 15, y + 5, x + 15, y + 6);
			bubble.lineTo(x + 15, y + 10);
			bubble.curveTo(x + 15, y + 11, x + 14, y + 12, x + 13, y + 12);
			bubble.lineTo(x + 8, y + 12);
			bubble.lineTo(x + 5, y + 15);
			bubble.lineTo(x + 5, y + 12);
			bubble.lineTo(x + 2, y + 12);
			bubble.curveTo(x + 1, y + 12, x + 1, y + 11, x + 1, y + 10);
			bubble.lineTo(x + 1, y + 6);
			bubble.curveTo(x + 1, y + 5, x + 1, y + 4, x + 2, y + 4);
			g2.draw(bubble);
		}

		private void paintShare(Graphics2D g2, int x, int y) {
			g2.drawLine(x + 3, y + 12, x + 11, y + 12);
			g2.drawLine(x + 11, y + 12, x + 11, y + 7);
			g2.drawLine(x + 11, y + 7, x + 8, y + 10);
			g2.drawLine(x + 11, y + 7, x + 14, y + 10);
			g2.drawLine(x + 7, y + 4, x + 12, y + 4);
			g2.drawLine(x + 12, y + 4, x + 12, y + 9);
			g2.drawLine(x + 12, y + 9, x + 10, y + 7);
		}

		private void paintProfile(Graphics2D g2, int x, int y) {
			g2.drawOval(x + 3, y + 2, 10, 10);
			g2.drawArc(x + 1, y + 8, 14, 7, 0, 180);
		}

		private void paintRibbonDisplayOptions(Graphics2D g2, int x, int y) {
			g2.fillOval(x + 3, y + 7, 2, 2);
			g2.fillOval(x + 7, y + 7, 2, 2);
			g2.fillOval(x + 11, y + 7, 2, 2);
		}
	}
}
