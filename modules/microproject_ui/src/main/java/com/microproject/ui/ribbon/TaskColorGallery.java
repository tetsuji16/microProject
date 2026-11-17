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
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ActionMap;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.microproject.ribbon.RibbonCommandInvocation;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ribbon.SwingRibbonModel;

/** Task font/fill gallery shared by the Task tab color controls. */
final class TaskColorGallery {
	static final String AUTOMATIC_VALUE = "__automatic__";
	static final String MORE_COLORS_VALUE = "__more_colors__";
	private static final String OUTER_ESCAPE_POPUP_PROPERTY = "MicroProject.ribbon.galleryEscapePopup";
	private static final String BACKGROUND_COMMAND = "RibbonFillColor";
	private static final int COLUMNS = 10;
	private static final int[] THEME_COLORS = {
		0xFFFFFF, 0x000000, 0xE7E6E6, 0x44546A, 0x5B9BD5,
		0xED7D31, 0xA5A5A5, 0xFFC000, 0x4472C4, 0x70AD47
	};
	private static final String[] THEME_NAME_KEYS = {
		"RibbonColorGallery.Color.Light1.text", "RibbonColorGallery.Color.Dark1.text",
		"RibbonColorGallery.Color.Light2.text", "RibbonColorGallery.Color.Dark2.text",
		"RibbonColorGallery.Color.Accent1.text", "RibbonColorGallery.Color.Accent2.text",
		"RibbonColorGallery.Color.Accent3.text", "RibbonColorGallery.Color.Accent4.text",
		"RibbonColorGallery.Color.Accent5.text", "RibbonColorGallery.Color.Accent6.text"
	};
	private static final String[] SHADE_NAME_KEYS = { null, "RibbonColorGallery.Shade.Light20.text",
		"RibbonColorGallery.Shade.Light40.text", "RibbonColorGallery.Shade.Light60.text",
		"RibbonColorGallery.Shade.Light80.text", "RibbonColorGallery.Shade.Dark25.text" };
	private static final Swatch[] STANDARD_COLORS = {
		new Swatch("RibbonColorGallery.Color.Red.text", 0xFF0000, 0),
		new Swatch("RibbonColorGallery.Color.Orange.text", 0xED7D31, 0),
		new Swatch("RibbonColorGallery.Color.Yellow.text", 0xFFC000, 0),
		new Swatch("RibbonColorGallery.Color.LightGreen.text", 0x92D050, 0),
		new Swatch("RibbonColorGallery.Color.Green.text", 0x00B050, 0),
		new Swatch("RibbonColorGallery.Color.LightBlue.text", 0x00B0F0, 0),
		new Swatch("RibbonColorGallery.Color.Blue.text", 0x0070C0, 0),
		new Swatch("RibbonColorGallery.Color.DarkBlue.text", 0x002060, 0),
		new Swatch("RibbonColorGallery.Color.Purple.text", 0x7030A0, 0),
		new Swatch("RibbonColorGallery.Color.Pink.text", 0xE3008C, 0)
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
				MenuElement[] selectedPath = MenuSelectionManager.defaultManager().getSelectedPath();
				int galleryIndex = -1;
				for (int index = 0; index < selectedPath.length; index++) {
					if (selectedPath[index].getComponent() == popup) {
						galleryIndex = index;
						break;
					}
				}
				int parentPathLength = galleryIndex;
				if (galleryIndex > 0 && selectedPath[galleryIndex - 1].getComponent() == popup.getInvoker()) {
					parentPathLength--;
				}
				MenuElement[] parentMenuPath = galleryIndex < 0 ? selectedPath
					: java.util.Arrays.copyOf(selectedPath, parentPathLength);
				popup.setVisible(false);
				focusInvoker(popup, parentMenuPath);
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

	private static void focusInvoker(JPopupMenu popup, MenuElement[] parentMenuPath) {
		java.awt.Component invoker = popup.getInvoker();
		if (invoker != null) {
			SwingUtilities.invokeLater(() -> {
				if (invoker.isShowing()) {
					java.awt.Window window = SwingUtilities.getWindowAncestor(invoker);
					while (window != null && window.getOwner() != null) window = window.getOwner();
					if (window != null) window.requestFocus();
					invoker.setFocusable(true);
					invoker.requestFocus();
					if (parentMenuPath.length > 0) {
						MenuSelectionManager.defaultManager().setSelectedPath(parentMenuPath);
						if (parentMenuPath[0].getComponent() instanceof JPopupMenu outerPopup
							&& invoker instanceof JComponent focusTarget) {
							installOuterPopupEscape(focusTarget, outerPopup, popup);
						}
					}
				}
			});
		}
	}

	private static void installOuterPopupEscape(JComponent focusTarget, JPopupMenu outerPopup, JPopupMenu gallery) {
		if (focusTarget.getClientProperty(OUTER_ESCAPE_POPUP_PROPERTY) == outerPopup) return;
		KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		InputMap inputMap = focusTarget.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = focusTarget.getActionMap();
		Object previousBinding = inputMap.get(escape);
		String actionKey = "microproject.dismissRibbonOverflow." + gallery.getName();
		Action previousAction = actionMap.get(actionKey);
		focusTarget.putClientProperty(OUTER_ESCAPE_POPUP_PROPERTY, outerPopup);
		inputMap.put(escape, actionKey);
		actionMap.put(actionKey, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(java.awt.event.ActionEvent event) {
				if (outerPopup.isShowing() && !gallery.isShowing()) {
					MenuSelectionManager.defaultManager().clearSelectedPath();
				}
			}
		});
		outerPopup.addPopupMenuListener(new PopupMenuListener() {
			private void restore() {
				if (focusTarget.getClientProperty(OUTER_ESCAPE_POPUP_PROPERTY) == outerPopup) {
					focusTarget.putClientProperty(OUTER_ESCAPE_POPUP_PROPERTY, null);
				}
				if (previousBinding == null) inputMap.remove(escape);
				else inputMap.put(escape, previousBinding);
				if (previousAction == null) actionMap.remove(actionKey);
				else actionMap.put(actionKey, previousAction);
				outerPopup.removePopupMenuListener(this);
			}
			@Override public void popupMenuWillBecomeVisible(PopupMenuEvent event) { }
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent event) { restore(); }
			@Override public void popupMenuCanceled(PopupMenuEvent event) { restore(); }
		});
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
			String accessible = localizedName(swatch, text) + " " + value;
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
				swatches.add(new Swatch(THEME_NAME_KEYS[column], rgb, row));
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

	private static String localizedName(Swatch swatch, UnaryOperator<String> text) {
		String name = text.apply(swatch.nameKey());
		int shade = swatch.shadeRow();
		return shade == 0 ? name : name + " " + text.apply(SHADE_NAME_KEYS[shade]);
	}

	private record Swatch(String nameKey, int rgb, int shadeRow) { }

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
