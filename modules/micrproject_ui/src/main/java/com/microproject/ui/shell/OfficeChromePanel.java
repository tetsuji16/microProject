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
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.IconManager;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.util.FlatUiSupport;

final class OfficeChromePanel extends JPanel {
	static final String NAME = "officeChromePanel";
	static final String APPLICATION_ICON_NAME = "officeChromeApplicationIcon";
	static final String AUTO_SAVE_NAME = "officeChromeAutoSave";
	static final String SEARCH_BOX_NAME = "officeChromeSearchBox";
	static final String SEARCH_FIELD_NAME = "officeChromeSearchField";
	static final String DOCUMENT_TITLE_NAME = "officeChromeDocumentTitle";
	static final String QUICK_ACCESS_NAME = "officeChromeQuickAccess";
	static final String RIGHT_ACTIONS_NAME = "officeChromeRightActions";
	static final String HELP_BUTTON_NAME = "officeChromeHelpButton";
	static final String WINDOW_BUTTONS_PLACEHOLDER_NAME = "officeChromeWindowButtonsPlaceholder";

	private static final Color CHROME_BACKGROUND = FlatUiSupport.ribbonChromeBackground();
	private static final Color BORDER_COLOR = FlatUiSupport.ribbonSurfaceBorderColor();
	private static final Color TEXT_COLOR = FlatUiSupport.labelForeground();
	private static final Color ACCENT_COLOR = FlatUiSupport.accentColor();
	private static final Dimension ICON_BUTTON_SIZE = new Dimension(
		FlatUiSupport.ribbonQuickAccessButtonSize(),
		FlatUiSupport.ribbonQuickAccessButtonSize());
	private static final int CLUSTER_GAP = 8;
	private static final Dimension AUTOSAVE_SIZE = new Dimension(36, 18);
	private static final int QUICK_ACCESS_ICON_SIZE = 16;

	private final MenuManager menuManager;
	private final Runnable helpAction;
	private final JTextField searchField;
	private final JLabel documentTitleLabel;
	private final AutoSaveControl autoSaveControl;

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
		this.helpAction = helpAction;
		this.autoSaveControl = autoSaveControl == null ? AutoSaveControl.DISABLED : autoSaveControl;
		this.searchField = new JTextField(28);
		this.documentTitleLabel = createDocumentTitleLabel(frame == null ? "" : frame.getTitle());
		setName(NAME);
		setOpaque(true);
		setBackground(CHROME_BACKGROUND);
		add(buildHeader(), BorderLayout.NORTH);
		add(ribbonPanel, BorderLayout.CENTER);
		if (frame != null) {
			PropertyChangeListener titleListener = event -> updateDocumentTitle((String) event.getNewValue());
			frame.addPropertyChangeListener("title", titleListener);
		}
	}

	private JComponent buildHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(true);
		header.setBackground(CHROME_BACKGROUND);
		header.setPreferredSize(new Dimension(0, FlatUiSupport.ribbonChromeHeight()));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FlatUiSupport.ribbonTopLineColor()));
		header.add(buildHeaderContent(), BorderLayout.CENTER);
		return header;
	}

	private JComponent buildHeaderContent() {
		JPanel content = new JPanel(new GridBagLayout()) {
			@Override public void doLayout() {
				// At narrow window widths the search field and document title must
				// yield space to the native help/minimize/maximize/close controls.
				// Leaving their preferred widths active pushes that cluster outside
				// the window, making the right buttons unreachable.
				boolean compact = getWidth() < 640;
				if (getComponentCount() > 1) getComponent(1).setVisible(!compact);
				documentTitleLabel.setVisible(!compact);
				super.doLayout();
			}
		};
		content.setOpaque(false);
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

	private JComponent buildLeftCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		cluster.setName(QUICK_ACCESS_NAME);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 0, 6);
		cluster.add(createApplicationIcon(), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 0, 0, 4);
		cluster.add(createLabel(UsabilityStrings.text("chrome.autoSave"), TEXT_COLOR), constraints);
		constraints.gridx++;
		cluster.add(new OfficeSwitchButton(autoSaveControl), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, CLUSTER_GAP, 0, 6);
		cluster.add(new VerticalDivider(), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 0, 0, 2);
		cluster.add(createActionButton("RibbonTopBarUndo"), constraints);
		constraints.gridx++;
		cluster.add(createActionButton("RibbonTopBarRedo"), constraints);
		constraints.gridx++;
		cluster.add(createActionButton("RibbonTopBarSaveProject"), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 12, 0, 0);
		cluster.add(documentTitleLabel, constraints);
		return cluster;
	}

	private JComponent createApplicationIcon() {
		JLabel icon = new JLabel(IconManager.getRibbonIcon("application.icon.small", 16, 16));
		icon.setName(APPLICATION_ICON_NAME);
		icon.setToolTipText(UsabilityStrings.text("chrome.application"));
		icon.setPreferredSize(new Dimension(16, 16));
		icon.setMinimumSize(new Dimension(16, 16));
		icon.setMaximumSize(new Dimension(16, 16));
		return icon;
	}

	private JComponent buildCenterCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.anchor = GridBagConstraints.CENTER;
		cluster.add(buildSearchBox(), constraints);
		return cluster;
	}

	private JComponent buildRightCluster() {
		JPanel cluster = new JPanel(new GridBagLayout());
		cluster.setOpaque(false);
		cluster.setName(RIGHT_ACTIONS_NAME);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.insets = new Insets(0, 0, 0, 4);
		cluster.add(createHelpButton(), constraints);
		constraints.gridx = 1;
		constraints.insets = new Insets(0, 0, 0, 0);
		cluster.add(createWindowButtonsPlaceholder(), constraints);
		return cluster;
	}

	private JLabel createDocumentTitleLabel(String title) {
		JLabel label = createLabel(compactDocumentTitle(title), TEXT_COLOR);
		label.setName(DOCUMENT_TITLE_NAME);
		label.setToolTipText(title);
		label.setPreferredSize(new Dimension(220, 22));
		label.setMinimumSize(new Dimension(80, 22));
		label.setMaximumSize(new Dimension(240, 22));
		return label;
	}

	private void updateDocumentTitle(String title) {
		documentTitleLabel.setText(compactDocumentTitle(title));
		documentTitleLabel.setToolTipText(title);
	}

	static String compactDocumentTitle(String title) {
		if (title == null || title.isBlank()) {
			return "ProjectLibre";
		}
		int separator = Math.max(title.lastIndexOf('\\'), title.lastIndexOf('/'));
		return separator >= 0 ? title.substring(separator + 1) : title;
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
		box.setLayout(new BorderLayout(4, 0));
		box.setBorder(new EmptyBorder(1, 8, 1, 8));
		box.setMinimumSize(new Dimension(180, FlatUiSupport.ribbonSearchHeight()));
		box.setPreferredSize(new Dimension(
			FlatUiSupport.ribbonSearchPreferredWidth(),
			FlatUiSupport.ribbonSearchHeight()));
		box.setMaximumSize(new Dimension(
			FlatUiSupport.ribbonSearchMaxWidth(),
			FlatUiSupport.ribbonSearchHeight()));

		AbstractButton searchButton = createGlyphButton("Search", GlyphIcon.search(), false, SEARCH_BOX_NAME + "Button");
		searchButton.addActionListener(event -> triggerFindAction());
		searchButton.setToolTipText(UsabilityStrings.text("chrome.search"));
		int searchButtonSize = Math.max(18, FlatUiSupport.ribbonSearchHeight() - 6);
		searchButton.setPreferredSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMinimumSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMaximumSize(new Dimension(searchButtonSize, searchButtonSize));

		searchField.setName(SEARCH_FIELD_NAME);
		searchField.putClientProperty("JTextField.placeholderText", UsabilityStrings.text("chrome.search"));
		searchField.setBorder(BorderFactory.createEmptyBorder());
		searchField.setOpaque(false);
		searchField.setFont(FlatUiSupport.ribbonChromeLabelFont());
		searchField.addActionListener(event -> triggerFindAction());

		box.add(searchButton, BorderLayout.WEST);
		box.add(searchField, BorderLayout.CENTER);
		return box;
	}

	private JLabel createLabel(String text, Color color) {
		JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setFont(FlatUiSupport.ribbonChromeLabelFont());
		label.setBorder(new EmptyBorder(0, 0, 0, 0));
		return label;
	}

	private AbstractButton createActionButton(String actionId) {
		OfficeIconButton button = new OfficeIconButton(resolveActionIcon(actionId, QUICK_ACCESS_ICON_SIZE), actionId, false);
		Action action = menuManager == null ? null : menuManager.getActionFromId(actionId);
		if (action != null) {
			button.setAction(action);
		}
		button.setText("");
		button.setName(actionId);
		button.setToolTipText(resolveTooltip(actionId));
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
		OfficeIconButton button = new OfficeIconButton(resolveActionIcon("RibbonProjectLibreDocumentation", QUICK_ACCESS_ICON_SIZE), help, false);
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
		if (menuManager != null) {
			String iconName = menuManager.getStringOrNull(actionId + ".icon");
			if (iconName != null) {
				Icon icon = IconManager.getRibbonIcon(iconName, iconSize, iconSize);
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
				int width = getWidth() - 1;
				int height = getHeight() - 1;
				g2.setColor(Color.WHITE);
				g2.fillRoundRect(0, 0, width, height, FlatUiSupport.ribbonCornerRadius(), FlatUiSupport.ribbonCornerRadius());
				g2.setColor(searchField.isFocusOwner() ? ACCENT_COLOR : BORDER_COLOR);
				g2.drawRoundRect(0, 0, width, height, FlatUiSupport.ribbonCornerRadius(), FlatUiSupport.ribbonCornerRadius());
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
				if (getModel().isPressed()) {
					g2.setColor(new Color(0xE2E5E9));
					g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, FlatUiSupport.ribbonButtonArc(), FlatUiSupport.ribbonButtonArc());
				} else if (getModel().isRollover() || active) {
					g2.setColor(active ? new Color(0xEAF3EA) : new Color(0xECECEC));
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
				Color track = isSelected() ? ACCENT_COLOR : new Color(0xC6CBD1);
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
			PROFILE
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
				g2.setColor(new Color(0x404040));
				switch (kind) {
					case COMMENT -> paintComment(g2, x, y);
					case SHARE -> paintShare(g2, x, y);
					case PROFILE -> paintProfile(g2, x, y);
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
	}
}
