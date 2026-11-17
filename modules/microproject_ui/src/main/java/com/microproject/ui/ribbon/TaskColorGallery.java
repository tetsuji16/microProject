/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.microproject.ribbon.RibbonCommandInvocation;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.SwingRibbonModel;

/** Task font/fill gallery shared by the Task tab color controls. */
final class TaskColorGallery {
	static final String AUTOMATIC_VALUE = "__automatic__";
	static final String MORE_COLORS_VALUE = "__more_colors__";
	private static final String BACKGROUND_COMMAND = "RibbonFillColor";
	private static final int COLUMNS = 10;
	private static final int[] THEME_COLORS = {
		0xFFFFFF, 0x000000, 0xE7E6E6, 0x44546A, 0x5B9BD5,
		0xED7D31, 0xA5A5A5, 0xFFC000, 0x4472C4, 0x70AD47
	};
	private static final String[] THEME_NAMES = {
		"Light 1", "Dark 1", "Light 2", "Dark 2", "Accent 1",
		"Accent 2", "Accent 3", "Accent 4", "Accent 5", "Accent 6"
	};
	private static final Swatch[] STANDARD_COLORS = {
		new Swatch("Red", 0xFF0000), new Swatch("Orange", 0xED7D31), new Swatch("Yellow", 0xFFC000),
		new Swatch("Light Green", 0x92D050), new Swatch("Green", 0x00B050), new Swatch("Light Blue", 0x00B0F0),
		new Swatch("Blue", 0x0070C0), new Swatch("Dark Blue", 0x002060), new Swatch("Purple", 0x7030A0),
		new Swatch("Pink", 0xE3008C)
	};
	private static final List<Swatch> THEME_SWATCHES = buildThemeSwatches();

	private TaskColorGallery() {
	}

	static boolean supports(String commandId) {
		return BACKGROUND_COMMAND.equals(commandId) || "RibbonFontColor".equals(commandId);
	}

	static JPopupMenu createPopup(SwingRibbonModel.RibbonButton specification, RibbonCommandSource commandSource,
		UnaryOperator<String> text) {
		JPopupMenu popup = new JPopupMenu();
		popup.setName(RibbonControlPopups.POPUP_NAME_PREFIX + specification.getId());
		popup.setFocusable(true);
		popup.setRequestFocusEnabled(true);
		JPanel content = new JPanel();
		content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
		content.setBorder(new EmptyBorder(8, 8, 8, 8));
		content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.put(KeyStroke.getKeyStroke("ESCAPE"), "dismissColorGallery");
		content.getActionMap().put("dismissColorGallery", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) {
				popup.setVisible(false);
				focusInvoker(popup);
			}
		});
		content.add(sectionTitle(text.apply("RibbonColorGallery.ThemeColors.text")));
		JPanel themeGrid = swatchGrid(THEME_SWATCHES, commandSource, specification.getId(), text, popup);
		content.add(themeGrid);
		content.add(sectionTitle(text.apply("RibbonColorGallery.StandardColors.text")));
		content.add(swatchGrid(List.of(STANDARD_COLORS), commandSource, specification.getId(), text, popup));
		content.add(javax.swing.Box.createVerticalStrut(5));
		JButton automatic = actionRow(popup, "automatic", text.apply("RibbonColorGallery.Automatic.text"));
		automatic.setName(popup.getName() + ".automatic");
		automatic.setActionCommand(AUTOMATIC_VALUE);
		automatic.addActionListener(event -> {
			popup.putClientProperty(RibbonControlPopups.GALLERY_SELECTION_PROPERTY, AUTOMATIC_VALUE);
			popup.setVisible(false);
			commit(commandSource, specification.getId(), automatic, AUTOMATIC_VALUE);
		});
		content.add(automatic);
		JButton moreColors = actionRow(popup, "more-colors", text.apply("RibbonColorGallery.MoreColors.text"));
		moreColors.setName(popup.getName() + ".more-colors");
		moreColors.setActionCommand(MORE_COLORS_VALUE);
		moreColors.addActionListener(event -> {
			popup.setVisible(false);
			commit(commandSource, specification.getId(), moreColors, MORE_COLORS_VALUE);
		});
		content.add(moreColors);
		popup.putClientProperty(RibbonControlPopups.GALLERY_GRID_PROPERTY, themeGrid);
		popup.add(content);
		return popup;
	}

	private static JButton actionRow(JPopupMenu popup, String id, String label) {
		JButton button = new JButton(label == null ? "" : label);
		button.setName(popup.getName() + "." + id);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setFocusable(true);
		button.getAccessibleContext().setAccessibleName(button.getText());
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setMargin(new java.awt.Insets(4, 8, 4, 8));
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		return button;
	}

	private static void focusInvoker(JPopupMenu popup) {
		java.awt.Component invoker = popup.getInvoker();
		if (invoker != null) {
			SwingUtilities.invokeLater(() -> {
				if (invoker.isShowing()) {
					invoker.requestFocus();
					java.awt.Window window = SwingUtilities.getWindowAncestor(invoker);
					while (window != null && window.getOwner() != null) window = window.getOwner();
					if (window != null) window.requestFocus();
				}
			});
		}
	}

	private static JLabel sectionTitle(String title) {
		JLabel label = new JLabel(title == null ? "" : title);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setFont(new FlatLafRibbonTheme().bandTitleFont());
		label.setBorder(new EmptyBorder(4, 2, 4, 2));
		return label;
	}

	private static JPanel swatchGrid(List<Swatch> swatches, RibbonCommandSource commandSource,
		String commandId, UnaryOperator<String> text, JPopupMenu popup) {
		JPanel grid = new JPanel(new java.awt.GridLayout(0, COLUMNS, 3, 3));
		grid.setOpaque(false);
		ButtonGroup selection = new ButtonGroup();
		for (Swatch swatch : swatches) {
			String value = String.format(java.util.Locale.ROOT, "#%06X", swatch.rgb());
			String accessible = swatch.name() + " " + value;
			JToggleButton button = new JToggleButton(new ColorIcon(new Color(swatch.rgb())));
			button.setName(popup.getName() + ".color." + value.substring(1));
			button.setActionCommand(value);
			button.setToolTipText(accessible);
			button.getAccessibleContext().setAccessibleName(accessible);
			button.setFocusable(true);
			button.setMargin(new java.awt.Insets(2, 2, 2, 2));
			button.setPreferredSize(new Dimension(24, 22));
			button.setHorizontalAlignment(SwingConstants.CENTER);
			button.addActionListener(event -> {
				popup.putClientProperty(RibbonControlPopups.GALLERY_SELECTION_PROPERTY, value);
				popup.setVisible(false);
				commit(commandSource, commandId, button, value);
			});
			selection.add(button);
			grid.add(button);
		}
		grid.setAlignmentX(Component.LEFT_ALIGNMENT);
		grid.putClientProperty("MicroProject.ribbon.colorGallery", Boolean.TRUE);
		return grid;
	}

	private static void commit(RibbonCommandSource commandSource, String commandId, Object source, String value) {
		var command = commandSource.createAction(commandId);
		if (command != null && command.isEnabled()) {
			commandSource.dispatch(new RibbonCommandInvocation(commandId,
				RibbonCommandInvocation.Origin.INPUT_CONTROL, source, value));
		}
	}

	private static List<Swatch> buildThemeSwatches() {
		List<Swatch> swatches = new ArrayList<>(THEME_COLORS.length * 6);
		for (int row = 0; row < 6; row++) {
			for (int column = 0; column < THEME_COLORS.length; column++) {
				int base = THEME_COLORS[column];
				int rgb = row == 0 ? base : row < 5 ? blend(base, 0xFFFFFF, 0.20 * row) : blend(base, 0x000000, 0.25);
				String shade = switch (row) {
					case 0 -> "";
					case 5 -> " 25% darker";
					default -> " " + (row * 20) + "% lighter";
				};
				swatches.add(new Swatch(THEME_NAMES[column] + shade, rgb));
			}
		}
		return List.copyOf(swatches);
	}

	private static int blend(int foreground, int background, double backgroundRatio) {
		double foregroundRatio = 1d - backgroundRatio;
		int red = (int) Math.round(((foreground >> 16) & 0xFF) * foregroundRatio
			+ ((background >> 16) & 0xFF) * backgroundRatio);
		int green = (int) Math.round(((foreground >> 8) & 0xFF) * foregroundRatio
			+ ((background >> 8) & 0xFF) * backgroundRatio);
		int blue = (int) Math.round((foreground & 0xFF) * foregroundRatio + (background & 0xFF) * backgroundRatio);
		return (red << 16) | (green << 8) | blue;
	}

	private record Swatch(String name, int rgb) { }

	private record ColorIcon(Color color) implements Icon {
		@Override public int getIconWidth() { return 16; }
		@Override public int getIconHeight() { return 14; }
		@Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(color);
				g.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 2, 2);
				Color border = UIManager.getColor("Component.borderColor");
				g.setColor(border == null ? Color.GRAY : border);
				g.drawRoundRect(x, y, getIconWidth(), getIconHeight(), 2, 2);
			} finally {
				g.dispose();
			}
		}
	}
}
