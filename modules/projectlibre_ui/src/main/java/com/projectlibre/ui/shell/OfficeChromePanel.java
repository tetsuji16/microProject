package com.projectlibre.ui.shell;

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

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.util.FlatUiSupport;

final class OfficeChromePanel extends JPanel {
	static final String NAME = "officeChromePanel";
	static final String AUTO_SAVE_NAME = "officeChromeAutoSave";
	static final String SEARCH_BOX_NAME = "officeChromeSearchBox";
	static final String SEARCH_FIELD_NAME = "officeChromeSearchField";
	static final String QUICK_ACCESS_NAME = "officeChromeQuickAccess";
	static final String RIGHT_ACTIONS_NAME = "officeChromeRightActions";
	static final String HELP_BUTTON_NAME = "officeChromeHelpButton";

	private static final Color CHROME_BACKGROUND = FlatUiSupport.ribbonChromeBackground();
	private static final Color BORDER_COLOR = FlatUiSupport.ribbonSurfaceBorderColor();
	private static final Color TEXT_COLOR = FlatUiSupport.labelForeground();
	private static final Color ACCENT_COLOR = FlatUiSupport.accentColor();
	private static final Dimension ICON_BUTTON_SIZE = new Dimension(
		FlatUiSupport.ribbonQuickAccessButtonSize(),
		FlatUiSupport.ribbonQuickAccessButtonSize());
	private static final int CLUSTER_GAP = 10;
	private static final Dimension AUTOSAVE_SIZE = new Dimension(42, 22);
	private static final int QUICK_ACCESS_ICON_SIZE = 16;

	private final MenuManager menuManager;
	private final Runnable helpAction;
	private final JTextField searchField;
	private final AutoSaveControl autoSaveControl;

	OfficeChromePanel(MenuManager menuManager, JComponent ribbonPanel, Runnable helpAction) {
		this(menuManager, ribbonPanel, helpAction, AutoSaveControl.DISABLED);
	}

	OfficeChromePanel(MenuManager menuManager, JComponent ribbonPanel, Runnable helpAction, AutoSaveControl autoSaveControl) {
		super(new BorderLayout());
		this.menuManager = menuManager;
		this.helpAction = helpAction;
		this.autoSaveControl = autoSaveControl == null ? AutoSaveControl.DISABLED : autoSaveControl;
		this.searchField = new JTextField(28);
		setName(NAME);
		setOpaque(true);
		setBackground(CHROME_BACKGROUND);
		add(buildHeader(), BorderLayout.NORTH);
		add(ribbonPanel, BorderLayout.CENTER);
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
		JPanel content = new JPanel(new GridBagLayout());
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
		constraints.insets = new Insets(0, 0, 0, 10);
		content.add(buildLeftCluster(), constraints);

		constraints.gridx = 1;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.insets = new Insets(0, 0, 0, 10);
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
		constraints.insets = new Insets(0, 0, 0, 4);
		cluster.add(createLabel("AutoSave", TEXT_COLOR), constraints);
		constraints.gridx++;
		cluster.add(new OfficeSwitchButton(autoSaveControl), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, CLUSTER_GAP, 0, 6);
		cluster.add(new VerticalDivider(), constraints);
		constraints.gridx++;
		constraints.insets = new Insets(0, 0, 0, 2);
		cluster.add(createActionButton("RibbonTopBarUndo", 24, 24), constraints);
		constraints.gridx++;
		cluster.add(createActionButton("RibbonTopBarRedo", 24, 24), constraints);
		constraints.gridx++;
		cluster.add(createActionButton("RibbonTopBarSaveProject", 24, 24), constraints);
		return cluster;
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
		cluster.add(createHelpButton());
		return cluster;
	}

	private JComponent buildSearchBox() {
		JPanel box = new SearchBoxPanel();
		box.setName(SEARCH_BOX_NAME);
		box.setLayout(new BorderLayout(6, 0));
		box.setBorder(new EmptyBorder(2, 10, 2, 10));
		box.setMinimumSize(new Dimension(220, FlatUiSupport.ribbonSearchHeight()));
		box.setPreferredSize(new Dimension(
			FlatUiSupport.ribbonSearchPreferredWidth(),
			FlatUiSupport.ribbonSearchHeight()));
		box.setMaximumSize(new Dimension(
			FlatUiSupport.ribbonSearchMaxWidth(),
			FlatUiSupport.ribbonSearchHeight()));

		AbstractButton searchButton = createGlyphButton("Search", GlyphIcon.search(), false, SEARCH_BOX_NAME + "Button");
		searchButton.addActionListener(event -> triggerFindAction());
		searchButton.setToolTipText("Search");
		int searchButtonSize = Math.max(18, FlatUiSupport.ribbonSearchHeight() - 6);
		searchButton.setPreferredSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMinimumSize(new Dimension(searchButtonSize, searchButtonSize));
		searchButton.setMaximumSize(new Dimension(searchButtonSize, searchButtonSize));

		searchField.setName(SEARCH_FIELD_NAME);
		searchField.putClientProperty("JTextField.placeholderText", "Search");
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

	private AbstractButton createActionButton(String actionId, int width, int height) {
		OfficeIconButton button = new OfficeIconButton(resolveActionIcon(actionId, QUICK_ACCESS_ICON_SIZE), actionId, false);
		Action action = menuManager == null ? null : menuManager.getActionFromId(actionId);
		if (action != null) {
			button.setAction(action);
		}
		button.setText("");
		button.setName(actionId);
		button.setToolTipText(resolveTooltip(actionId));
		button.setPreferredSize(new Dimension(width, height));
		button.setMinimumSize(new Dimension(width, height));
		button.setMaximumSize(new Dimension(width, height));
		return button;
	}

	private AbstractButton createGlyphButton(String tooltip, Icon icon, boolean active, String name) {
		OfficeIconButton button = new OfficeIconButton(icon, tooltip, active);
		button.setName(name);
		button.setToolTipText(tooltip);
		return button;
	}

	private AbstractButton createHelpButton() {
		OfficeIconButton button = new OfficeIconButton(resolveActionIcon("RibbonProjectLibreDocumentation", QUICK_ACCESS_ICON_SIZE), "Help", false);
		button.setName(HELP_BUTTON_NAME);
		button.setToolTipText("Help");
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
			setPreferredSize(new Dimension(1, 18));
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
			setToolTipText("AutoSave");
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				Color track = isSelected() ? ACCENT_COLOR : new Color(0xC6CBD1);
				g2.setColor(track);
				g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 22, 22));
				int knobDiameter = 16;
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
