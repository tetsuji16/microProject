package com.projectlibre.ui.ribbon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;

import com.projectlibre1.menu.ExtToolBarFactory;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre1.util.UiLinkTargets;

public final class SwingRibbonFactory {
	static final String BRAND_COMPONENT_NAME = "projectLibreRibbonBrand";
	static final String BRAND_TARGET_PROPERTY = "ProjectLibre.ribbonBrandTarget";
	private final ExtToolBarFactory buttonFactory;
	private final ResourceBundle[] bundles;

	public SwingRibbonFactory(ExtToolBarFactory buttonFactory, ResourceBundle... bundles) {
		this.buttonFactory = Objects.requireNonNull(buttonFactory);
		this.bundles = Objects.requireNonNull(bundles);
	}

	public SwingRibbonModel createModel(String ribbonId) {
		List<SwingRibbonModel.RibbonTab> tabs = new ArrayList<>();
		for (String tabId : tokens(ribbonId)) {
			String title = getString(tabId + ".title");
			List<SwingRibbonModel.RibbonBand> bands = new ArrayList<>();
			for (String bandId : tokens(tabId)) {
				String bandTitle = getString(bandId + ".title");
				List<SwingRibbonModel.RibbonButton> buttons = new ArrayList<>();
				for (String token : tokens(bandId)) {
					SwingRibbonModel.RibbonButton button = toButton(token);
					if (button != null) {
						buttons.add(button);
					}
				}
				bands.add(new SwingRibbonModel.RibbonBand(bandId, bandTitle, buttons));
			}
			tabs.add(new SwingRibbonModel.RibbonTab(tabId, title, bands));
		}
		return new SwingRibbonModel(ribbonId, tabs, tokens(ribbonId + ".TaskBar"));
	}

	public JPanel createPanel(String ribbonId, Runnable helpAction) {
		return createPanel(createModel(ribbonId), helpAction);
	}

	public JPanel createPanel(SwingRibbonModel model, Runnable helpAction) {
		SwingRibbonPanel panel = new SwingRibbonPanel(model, helpAction);
		panel.build();
		return panel;
	}

	public String getActionStringFromId(String id) {
		return buttonFactory.getActionStringFromId(id);
	}

	private SwingRibbonModel.RibbonButton toButton(String token) {
		if (token == null || token.isBlank() || "|".equals(token) || "-".equals(token) || "\\".equals(token)) {
			return null;
		}
		SwingRibbonModel.ButtonPriority priority = SwingRibbonModel.ButtonPriority.MEDIUM;
		String id = token;
		if (token.endsWith(".TOP")) {
			id = token.substring(0, token.length() - 4);
			priority = SwingRibbonModel.ButtonPriority.TOP;
		} else if (token.endsWith(".LOW")) {
			id = token.substring(0, token.length() - 4);
			priority = SwingRibbonModel.ButtonPriority.LOW;
		}
		return new SwingRibbonModel.RibbonButton(id, priority);
	}

	private List<String> tokens(String key) {
		String value = getStringOrNull(key);
		if (value == null) {
			return List.of();
		}
		String[] pieces = value.trim().split("\\s+");
		List<String> result = new ArrayList<>();
		for (String piece : pieces) {
			if (!piece.isBlank()) {
				result.add(piece);
			}
		}
		return result;
	}

	private String getString(String key) {
		String value = getStringOrNull(key);
		if (value == null) {
			throw new MissingResourceException(key, getClass().getName(), key);
		}
		return value;
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

	private final class SwingRibbonPanel extends JPanel {
		private static final String SIZE_PROPERTY = "ProjectLibre.ribbonButtonSize";
		private final SwingRibbonModel model;
		private final Runnable helpAction;
		private final JPanel cards;
		private final ButtonGroup tabGroup;
		private final Map<String, JPanel> tabBodies;
		private final Map<String, JToggleButton> tabButtons;

		private SwingRibbonPanel(SwingRibbonModel model, Runnable helpAction) {
			super(new BorderLayout());
			this.model = model;
			this.helpAction = helpAction;
			this.cards = new JPanel(new BorderLayout());
			this.tabGroup = new ButtonGroup();
			this.tabBodies = new LinkedHashMap<>();
			this.tabButtons = new LinkedHashMap<>();
			setOpaque(true);
			setBackground(FlatUiSupport.ribbonChromeBackground());
			setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FlatUiSupport.ribbonTopLineColor()));
			setPreferredSize(new Dimension(0, 118));
		}

		private void build() {
			add(buildTabRow(), BorderLayout.NORTH);
			add(cards, BorderLayout.CENTER);
			for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
				tabBodies.computeIfAbsent(tab.getId(), this::buildTabBody);
			}
			if (!model.getTabs().isEmpty()) {
				showTab(model.getTabs().get(0).getId());
			}
		}

		private JComponent buildTabRow() {
			JPanel row = new JPanel(new BorderLayout());
			row.setOpaque(true);
			row.setBackground(FlatUiSupport.ribbonChromeBackground());
			row.setPreferredSize(new Dimension(0, 35));
			row.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));

			row.add(buildBrandArea(), BorderLayout.EAST);

			JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 1));
			tabs.setOpaque(false);
			for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
				tabs.add(createTabButton(tab));
			}
			JPanel tabsHolder = new JPanel(new BorderLayout());
			tabsHolder.setOpaque(false);
			tabsHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
			tabsHolder.add(tabs, BorderLayout.WEST);
			row.add(tabsHolder, BorderLayout.CENTER);
			return row;
		}

		private JComponent buildBrandArea() {
			JPanel brandArea = new JPanel(new BorderLayout());
			brandArea.setName(BRAND_COMPONENT_NAME);
			brandArea.putClientProperty(BRAND_TARGET_PROPERTY, UiLinkTargets.PROJECT_HOME);
			brandArea.setOpaque(false);
			brandArea.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 1, 0, 0, FlatUiSupport.ribbonLogoSeparatorColor()),
				BorderFactory.createEmptyBorder(0, 10, 0, 0)));
			brandArea.add(createBrandLabel(), BorderLayout.CENTER);
			return brandArea;
		}

		private JLabel createBrandLabel() {
			JLabel logo = new JLabel();
			ImageIcon icon = IconManager.getIcon("logo.ProjectLibre");
			if (icon != null) {
				logo.setIcon(icon);
				logo.setPreferredSize(new Dimension(Math.max(144, icon.getIconWidth()), Math.max(31, icon.getIconHeight())));
				logo.setMinimumSize(new Dimension(144, 31));
			} else {
				logo.setText("ProjectLibre");
				logo.setFont(FlatUiSupport.headerFont().deriveFont(Font.BOLD, 26f));
				logo.setPreferredSize(new Dimension(144, 31));
				logo.setMinimumSize(new Dimension(144, 31));
			}
			logo.setName(BRAND_COMPONENT_NAME);
			logo.putClientProperty(BRAND_TARGET_PROPERTY, UiLinkTargets.PROJECT_HOME);
			logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			logo.setHorizontalAlignment(SwingConstants.LEFT);
			logo.setVerticalAlignment(SwingConstants.CENTER);
			logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			logo.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					if (event.getButton() == MouseEvent.BUTTON1) {
						BrowserControl.displayURL(UiLinkTargets.PROJECT_HOME);
					}
				}
			});
			return logo;
		}

		private AbstractButton createTabButton(SwingRibbonModel.RibbonTab tab) {
			JToggleButton button = new JToggleButton(tab.getTitle());
			tabButtons.put(tab.getId(), button);
			tabGroup.add(button);
			FlatUiSupport.styleRibbonTabButton(button);
			button.addItemListener(event -> {
				boolean selected = event.getStateChange() == ItemEvent.SELECTED;
				updateTabButtonAppearance(button, selected);
			});
			button.addActionListener(e -> showTab(tab.getId()));
			if (tabBodies.isEmpty()) {
				button.setSelected(true);
			}
			updateTabButtonAppearance(button, button.isSelected());
			return button;
		}

		private void updateTabButtonAppearance(AbstractButton button, boolean selected) {
			button.setBackground(FlatUiSupport.ribbonChromeBackground());
			button.setForeground(FlatUiSupport.tabUnselectedForeground());
			Border innerBorder = selected
				? BorderFactory.createMatteBorder(0, 0, 2, 0, FlatUiSupport.ribbonTabUnderlineColor())
				: BorderFactory.createEmptyBorder(0, 0, 2, 0);
			button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(6, 13, 4, 13),
				innerBorder));
		}

		private void showTab(String tabId) {
			JPanel tabBody = tabBodies.computeIfAbsent(tabId, this::buildTabBody);
			JToggleButton tabButton = tabButtons.get(tabId);
			if (tabButton != null && !tabButton.isSelected()) {
				tabButton.setSelected(true);
			}
			cards.removeAll();
			cards.add(tabBody, BorderLayout.CENTER);
			cards.revalidate();
			cards.repaint();
		}

		private JPanel buildTabBody(String tabId) {
			SwingRibbonModel.RibbonTab tab = model.getTabs().stream()
				.filter(candidate -> candidate.getId().equals(tabId))
				.findFirst()
				.orElseThrow();

			JPanel shell = new JPanel(new BorderLayout());
			shell.setOpaque(true);
			shell.setBackground(FlatUiSupport.ribbonChromeBackground());
			shell.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));

			JPanel bandRow = new RoundedRibbonSurfacePanel();
			bandRow.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			bandRow.setPreferredSize(new Dimension(0, 80));
			bandRow.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				bandRow.add(buildBand(band));
			}
			shell.add(bandRow, BorderLayout.CENTER);
			return shell;
		}

		private JComponent buildBand(SwingRibbonModel.RibbonBand band) {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setOpaque(false);
			panel.setPreferredSize(new Dimension(computeBandWidth(band), 68));
			panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 0, 1, FlatUiSupport.ribbonBandSeparatorColor()),
				BorderFactory.createEmptyBorder(1, 8, 1, 8)));

			JPanel content = new JPanel();
			content.setOpaque(false);
			content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));

			JPanel largeButtons = new JPanel();
			largeButtons.setOpaque(false);
			largeButtons.setLayout(new BoxLayout(largeButtons, BoxLayout.X_AXIS));

			List<AbstractButton> smallButtonList = new ArrayList<>();

			boolean hasLargeButton = false;
			for (SwingRibbonModel.RibbonButton buttonSpec : band.getButtons()) {
				AbstractButton button = createButton(buttonSpec.getId());
				if (buttonSpec.getPriority() == SwingRibbonModel.ButtonPriority.TOP) {
					hasLargeButton = true;
					largeButtons.add(styleActionButton(button, true));
				} else {
					smallButtonList.add(styleActionButton(button, false));
				}
			}

			if (hasLargeButton) {
				content.add(largeButtons);
				if (!smallButtonList.isEmpty()) {
					content.add(Box.createHorizontalStrut(10));
				}
			}
			if (!smallButtonList.isEmpty()) {
				content.add(buildSmallButtonColumns(smallButtonList));
			}
			if (!hasLargeButton && smallButtonList.isEmpty()) {
				content.add(Box.createRigidArea(new Dimension(8, 46)));
			}

			panel.add(content, BorderLayout.CENTER);

			JLabel title = new JLabel(band.getTitle(), SwingConstants.CENTER);
			title.setOpaque(false);
			title.setForeground(FlatUiSupport.ribbonBandTitleForeground());
			title.setFont(FlatUiSupport.uiFont().deriveFont(Font.PLAIN, 11f));
			title.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
			panel.add(title, BorderLayout.SOUTH);
			return panel;
		}

		private int computeBandWidth(SwingRibbonModel.RibbonBand band) {
			boolean hasLarge = band.getButtons().stream()
				.anyMatch(button -> button.getPriority() == SwingRibbonModel.ButtonPriority.TOP);
			long smallCount = band.getButtons().stream()
				.filter(button -> button.getPriority() != SwingRibbonModel.ButtonPriority.TOP)
				.count();
			int smallColumns = (int) Math.max(0, Math.ceil(smallCount / 3.0));
			if (hasLarge) {
				return Math.max(108, 86 + (smallColumns * 112));
			}
			return Math.max(96, Math.min(228, 12 + (smallColumns * 112)));
		}

		private JComponent buildSmallButtonColumns(List<AbstractButton> buttons) {
			JPanel columns = new JPanel();
			columns.setOpaque(false);
			columns.setLayout(new BoxLayout(columns, BoxLayout.X_AXIS));

			for (int start = 0; start < buttons.size(); start += 3) {
				JPanel column = new JPanel(new GridLayout(0, 1, 0, 2));
				column.setOpaque(false);
				column.setBorder(new EmptyBorder(0, start == 0 ? 0 : 4, 0, 0));
				for (int index = start; index < Math.min(start + 3, buttons.size()); index++) {
					column.add(buttons.get(index));
				}
				columns.add(column);
			}
			return columns;
		}

		private AbstractButton createButton(String buttonId) {
			AbstractButton button;
			try {
				button = buttonFactory.createJButton(buttonId);
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new IllegalStateException("Unable to create ribbon button " + buttonId, ex);
			}
			String text = getStringOrNull(buttonId + ".text");
			if (text != null) {
				button.setText(text);
			}
			String tooltip = getStringOrNull(buttonId + ".tooltip");
			if (tooltip != null) {
				button.setToolTipText(tooltip);
			}
			button.setAlignmentY(Component.TOP_ALIGNMENT);
			button.setActionCommand(buttonId);
			return button;
		}

		private AbstractButton styleActionButton(AbstractButton button, boolean large) {
			if (large) {
				FlatUiSupport.styleRibbonLargeButton(button);
				button.setHorizontalTextPosition(SwingConstants.CENTER);
				button.setVerticalTextPosition(SwingConstants.BOTTOM);
				button.setAlignmentX(Component.CENTER_ALIGNMENT);
				button.putClientProperty(SIZE_PROPERTY, "large");
				button.setMaximumSize(new Dimension(78, 58));
				button.setPreferredSize(new Dimension(78, 58));
			} else {
				FlatUiSupport.styleRibbonSmallButton(button);
				button.setHorizontalAlignment(SwingConstants.LEFT);
				button.setHorizontalTextPosition(SwingConstants.RIGHT);
				button.setVerticalTextPosition(SwingConstants.CENTER);
				button.putClientProperty(SIZE_PROPERTY, "small");
				button.setMaximumSize(new Dimension(108, 22));
				button.setPreferredSize(new Dimension(108, 22));
			}
			return button;
		}
	}

	private static final class RoundedRibbonSurfacePanel extends JPanel {
		private RoundedRibbonSurfacePanel() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				FlatUiSupport.enableAntialiasing(g2);
				Color background = FlatUiSupport.ribbonSurfaceColor();
				Color border = FlatUiSupport.ribbonSurfaceBorderColor();
				int width = getWidth() - 1;
				int height = getHeight() - 1;
				g2.setColor(background);
				g2.fillRoundRect(0, 0, width, height, 10, 10);
				g2.setColor(border);
				g2.drawRoundRect(0, 0, width, height, 10, 10);
			} finally {
				g2.dispose();
			}
			super.paintComponent(graphics);
		}
	}
}
