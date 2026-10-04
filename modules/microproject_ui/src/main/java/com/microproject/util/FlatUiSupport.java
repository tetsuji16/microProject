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
package com.microproject.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.event.ChangeListener;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.FontUIResource;

/**
 * Small FlatLaf-friendly UI helpers for shared Swing styling.
 */
public final class FlatUiSupport {
	public static final String BUTTON_STYLE_ROLE_PROPERTY = "MicroProject.buttonStyleRole";
	public static final String BUTTON_STYLE_ROLE_TOOLBAR = "toolbar";
	private static final String THEME_KEY_PREFIX = "MicroProject.";
	private static final String RIBBON_CHROME_BACKGROUND_KEY = THEME_KEY_PREFIX + "ribbonChromeBackground";
	private static final String RIBBON_SURFACE_BACKGROUND_KEY = THEME_KEY_PREFIX + "ribbonSurfaceBackground";
	private static final String RIBBON_ACCENT_COLOR_KEY = THEME_KEY_PREFIX + "ribbonAccentColor";
	private static final String RIBBON_CHROME_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonChromeHeight";
	private static final String RIBBON_CHROME_VERTICAL_INSET_KEY = THEME_KEY_PREFIX + "ribbonChromeVerticalInset";
	private static final String RIBBON_HORIZONTAL_INSET_KEY = THEME_KEY_PREFIX + "ribbonHorizontalInset";
	private static final String RIBBON_TAB_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonTabHeight";
	private static final String RIBBON_TAB_HORIZONTAL_PADDING_KEY = THEME_KEY_PREFIX + "ribbonTabHorizontalPadding";
	private static final String RIBBON_TAB_VERTICAL_PADDING_KEY = THEME_KEY_PREFIX + "ribbonTabVerticalPadding";
	private static final String RIBBON_SURFACE_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonSurfaceHeight";
	private static final String RIBBON_BAND_VERTICAL_INSET_KEY = THEME_KEY_PREFIX + "ribbonBandVerticalInset";
	private static final String RIBBON_BUTTON_VERTICAL_INSET_KEY = THEME_KEY_PREFIX + "ribbonButtonVerticalInset";
	private static final String RIBBON_SEARCH_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonSearchHeight";
	private static final String RIBBON_SEARCH_PREFERRED_WIDTH_KEY = THEME_KEY_PREFIX + "ribbonSearchPreferredWidth";
	private static final String RIBBON_SEARCH_MAX_WIDTH_KEY = THEME_KEY_PREFIX + "ribbonSearchMaxWidth";
	private static final String RIBBON_CORNER_RADIUS_KEY = THEME_KEY_PREFIX + "ribbonCornerRadius";
	private static final String RIBBON_BUTTON_ARC_KEY = THEME_KEY_PREFIX + "ribbonButtonArc";
	private static final String RIBBON_QUICK_ACCESS_BUTTON_SIZE_KEY = THEME_KEY_PREFIX + "ribbonQuickAccessButtonSize";
	private static final String RIBBON_LARGE_BUTTON_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonLargeButtonHeight";
	private static final String RIBBON_LARGE_BUTTON_MIN_WIDTH_KEY = THEME_KEY_PREFIX + "ribbonLargeButtonMinWidth";
	private static final String RIBBON_INLINE_BUTTON_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonInlineButtonHeight";
	private static final String RIBBON_INLINE_BUTTON_MEDIUM_MIN_WIDTH_KEY = THEME_KEY_PREFIX + "ribbonInlineButtonMediumMinWidth";
	private static final String RIBBON_INLINE_BUTTON_SMALL_MIN_WIDTH_KEY = THEME_KEY_PREFIX + "ribbonInlineButtonSmallMinWidth";
	private static final String RIBBON_BAND_TITLE_HEIGHT_KEY = THEME_KEY_PREFIX + "ribbonBandTitleHeight";

	private FlatUiSupport() {
	}

	public static Font uiFont() {
		Font font = UIManager.getFont("defaultFont");
		if (font == null)
			font = UIManager.getFont("Table.font");
		if (font == null)
			font = UIManager.getFont("Label.font");
		if (font == null)
			font = new Font("SansSerif", Font.PLAIN, 12);
		return font;
	}

	public static Font headerFont() {
		return ganttHeaderFont();
	}

	public static Font mediumFont() {
		return uiFont().deriveFont(Font.PLAIN, Math.max(12f, uiFont().getSize2D()));
	}

	public static Font compactFont() {
		return uiFont().deriveFont(Font.PLAIN, Math.max(11f, uiFont().getSize2D() - 1f));
	}

	public static Font ribbonTabFont() {
		return mediumFont();
	}

	public static Font ribbonButtonFont() {
		return mediumFont();
	}

	public static Font ribbonBandTitleFont() {
		return compactFont();
	}

	public static Font ribbonChromeLabelFont() {
		return mediumFont();
	}

	public static Font ganttHeaderFont() {
		return mediumFont();
	}

	public static void applyMinimumSize(JDialog dialog, Dimension minimumSize) {
		if (dialog == null || minimumSize == null) {
			return;
		}
		dialog.setMinimumSize(minimumSize);
	}

	public static Color color(String key, Color fallback) {
		Color value = UIManager.getColor(key);
		return value != null ? value : fallback;
	}

	public static Color appBackground() {
		return workspaceBackground();
	}

	public static Color panelBackground() {
		return color("Panel.background", FlatUiTheme.appBackground());
	}

	public static Color workspaceBackground() {
		return color(THEME_KEY_PREFIX + "workspaceBackground", FlatUiTheme.appBackground());
	}

	public static Color dialogBackground() {
		return color(THEME_KEY_PREFIX + "dialogBackground", FlatUiTheme.appBackground());
	}

	public static Color dialogSurfaceBackground() {
		return color(THEME_KEY_PREFIX + "dialogSurfaceBackground", Color.WHITE);
	}

	public static Color dataSurfaceBackground() {
		return tableContentBackground();
	}

	public static Color viewportBackground() {
		return dataSurfaceBackground();
	}

	public static Color surfaceBackground() {
		Color color = UIManager.getColor("Panel.background");
		if (color == null)
			color = UIManager.getColor("Table.background");
		if (color == null)
			color = FlatUiTheme.appBackground();
		return color;
	}

	public static Color ribbonChromeBackground() {
		return color(RIBBON_CHROME_BACKGROUND_KEY, FlatUiTheme.ribbonChromeBackground());
	}

	public static Color officeTitleBarBackground() {
		return color(THEME_KEY_PREFIX + "officeTitleBarBackground", ribbonChromeBackground());
	}

	public static Color officeTitleBarForeground() {
		return color(THEME_KEY_PREFIX + "officeTitleBarForeground", Color.WHITE);
	}

	public static Color tableBackground() {
		return FlatUiTheme.tableBackground();
	}

	public static Color tableContentBackground() {
		return FlatUiTheme.tableBackground();
	}

	public static Color tableForeground() {
		return color("Table.foreground", FlatUiTheme.tableForeground());
	}

	public static Color tableSelectionBackground() {
		return color("Table.selectionBackground", FlatUiTheme.tableSelectionBackground());
	}

	public static Color tableSelectionForeground() {
		return color("Table.selectionForeground", FlatUiTheme.tableSelectionForeground());
	}

	public static Color headerBackground() {
		return color("TableHeader.background", FlatUiTheme.headerBackground());
	}

	public static Color headerForeground() {
		return color("TableHeader.foreground", FlatUiTheme.headerForeground());
	}

	public static Color disabledForeground() {
		return color("Label.disabledForeground", FlatUiTheme.disabledForeground());
	}

	public static Color infoForeground() {
		return color("TextField.foreground", FlatUiTheme.infoForeground());
	}

	public static Color errorForeground() {
		Color color = UIManager.getColor("Actions.Red");
		if (color == null)
			color = UIManager.getColor("Component.errorFocusColor");
		if (color == null)
			color = FlatUiTheme.error();
		return color;
	}

	public static Color labelForeground() {
		return color("Label.foreground", FlatUiTheme.labelForeground());
	}

	public static Color borderColor() {
		return color("Component.borderColor", FlatUiTheme.border());
	}

	public static Color separatorColor() {
		return color("Separator.foreground", FlatUiTheme.separator());
	}

	public static Color ribbonTopLineColor() {
		return color(THEME_KEY_PREFIX + "ribbonTopLineColor", new Color(0xD1D1D1));
	}

	public static Color ribbonSurfaceColor() {
		return color(RIBBON_SURFACE_BACKGROUND_KEY, surfaceBackground());
	}

	public static Color ribbonSurfaceBorderColor() {
		return color(THEME_KEY_PREFIX + "ribbonSurfaceBorderColor", new Color(0xD1D1D1));
	}

	public static Color ribbonAccentColor() {
		return color(RIBBON_ACCENT_COLOR_KEY, new Color(0x0F6CBD));
	}

	public static Color ribbonSelectedTabColor() {
		return ribbonChromeBackground();
	}

	public static Color ribbonTabHoverColor() {
		return color(THEME_KEY_PREFIX + "ribbonTabHoverColor", new Color(0xEAF3FF));
	}

	public static Color ribbonTabBorderHoverColor() {
		return color(THEME_KEY_PREFIX + "ribbonTabBorderHoverColor", new Color(0xB9D7F5));
	}

	public static Color ribbonTabUnderlineColor() {
		return ribbonAccentColor();
	}

	public static Color tabSelectedForeground() {
		return labelForeground();
	}

	public static Color tabUnselectedForeground() {
		return labelForeground();
	}

	public static Color tableGridColor() {
		return color("Table.gridColor", FlatUiTheme.spreadsheetGrid());
	}

	public static Color ganttHeaderGridColor() {
		return spreadsheetGridColor();
	}

	public static Color spreadsheetBodyBackground() {
		return color(THEME_KEY_PREFIX + "spreadsheetBodyBackground", FlatUiTheme.spreadsheetBodyBackground());
	}

	public static Color assignmentCompleteBackground() {
		return color(THEME_KEY_PREFIX + "assignmentCompleteBackground", new Color(0xE5F0E5));
	}

	public static Color assignmentPartialBackground() {
		return color(THEME_KEY_PREFIX + "assignmentPartialBackground", new Color(0xFFF3D9));
	}

	public static Color spreadsheetReadOnlyForeground() {
		return color(THEME_KEY_PREFIX + "spreadsheetReadOnlyForeground", FlatUiTheme.spreadsheetReadOnlyForeground());
	}

	public static Color spreadsheetHeaderBackground() {
		return color(THEME_KEY_PREFIX + "spreadsheetHeaderBackground", FlatUiTheme.spreadsheetHeaderBackground());
	}

	public static Color spreadsheetHeaderSelectedBackground() {
		return color(THEME_KEY_PREFIX + "spreadsheetHeaderSelectedBackground", FlatUiTheme.spreadsheetHeaderSelectedBackground());
	}

	public static Color spreadsheetRangeSelectionBackground() {
		return color(THEME_KEY_PREFIX + "spreadsheetRangeSelectionBackground", FlatUiTheme.spreadsheetRangeSelectionBackground());
	}

	public static Color spreadsheetActiveCellBorderColor() {
		return color(THEME_KEY_PREFIX + "spreadsheetActiveCellBorder", FlatUiTheme.spreadsheetActiveCellBorder());
	}

	public static Color spreadsheetGridColor() {
		return color(THEME_KEY_PREFIX + "spreadsheetGridColor", FlatUiTheme.spreadsheetGrid());
	}

	public static Color ribbonBandSeparatorColor() {
		return color(THEME_KEY_PREFIX + "ribbonBandSeparatorColor", new Color(0xD8E0EA));
	}

	public static Color ribbonBandTitleForeground() {
		return color(THEME_KEY_PREFIX + "ribbonBandTitleForeground", new Color(0x616161));
	}

	public static Color ribbonIconColor() {
		return color(THEME_KEY_PREFIX + "ribbonIconColor", new Color(0x323130));
	}

	public static Color ribbonIconHoverColor() {
		return ribbonAccentColor();
	}

	public static Color ribbonIconSelectedColor() {
		return ribbonAccentColor();
	}

	public static Color ribbonIconDisabledColor() {
		return color(THEME_KEY_PREFIX + "ribbonIconDisabledColor", new Color(0xA19F9D));
	}

	public static Color chromeButtonPressedBackground() {
		return color(THEME_KEY_PREFIX + "chromeButtonPressedBackground", new Color(0xE2E5E9));
	}

	public static Color chromeButtonActiveBackground() {
		return color(THEME_KEY_PREFIX + "chromeButtonActiveBackground", new Color(0xEAF3EA));
	}

	public static Color chromeButtonHoverBackground() {
		return color(THEME_KEY_PREFIX + "chromeButtonHoverBackground", new Color(0xECECEC));
	}

	public static Color switchTrackBackground() {
		return color(THEME_KEY_PREFIX + "switchTrackBackground", new Color(0xC6CBD1));
	}

	public static Color ribbonLogoSeparatorColor() {
		return blend(borderColor(), ribbonChromeBackground(), 0.88f);
	}

	public static Color accentColor() {
		Color color = UIManager.getColor("Component.focusColor");
		if (color == null)
			color = UIManager.getColor("ProgressBar.foreground");
		if (color == null)
			color = UIManager.getColor("Actions.Blue");
		if (color == null)
			color = FlatUiTheme.accent();
		return color;
	}

	private static Color buttonStyleBaseBackground(AbstractButton button) {
		Object role = button == null ? null : button.getClientProperty(BUTTON_STYLE_ROLE_PROPERTY);
		if (BUTTON_STYLE_ROLE_TOOLBAR.equals(role))
			return panelBackground();
		return ribbonSurfaceColor();
	}

	private static Color buttonAccentColor(AbstractButton button) {
		return accentColor();
	}

	private static boolean supportsPersistentSelectedState(AbstractButton button) {
		return button instanceof JToggleButton;
	}

	public static Color commandButtonHoverBackground(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.08f);
	}

	public static Color commandButtonPressedBackground(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.16f);
	}

	public static Color commandButtonSelectedBackground(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.14f);
	}

	public static Color commandButtonHoverBorderColor(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.38f);
	}

	public static Color commandButtonPressedBorderColor(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.50f);
	}

	public static Color commandButtonSelectedBorderColor(AbstractButton button) {
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.46f);
	}

	public static Color resolveCommandButtonBackground(AbstractButton button) {
		if (button == null)
			return null;
		ButtonModel model = button.getModel();
		if (model == null || !button.isEnabled())
			return null;
		if (model.isPressed() || model.isArmed())
			return commandButtonPressedBackground(button);
		if (supportsPersistentSelectedState(button) && model.isSelected())
			return commandButtonSelectedBackground(button);
		if (model.isRollover())
			return commandButtonHoverBackground(button);
		return null;
	}

	public static Color resolveCommandButtonBorderColor(AbstractButton button) {
		if (button == null)
			return null;
		ButtonModel model = button.getModel();
		if (model == null || !button.isEnabled())
			return blend(borderColor(), buttonStyleBaseBackground(button), 0.20f);
		if (model.isPressed() || model.isArmed())
			return commandButtonPressedBorderColor(button);
		if (supportsPersistentSelectedState(button) && model.isSelected())
			return commandButtonSelectedBorderColor(button);
		if (model.isRollover())
			return commandButtonHoverBorderColor(button);
		return null;
	}

	private static int intValue(String key, int fallback) {
		Object value = UIManager.get(key);
		return value instanceof Integer integer ? integer.intValue() : fallback;
	}

	public static int ribbonChromeHeight() {
		return intValue(RIBBON_CHROME_HEIGHT_KEY, 40);
	}

	public static int ribbonChromeVerticalInset() {
		return intValue(RIBBON_CHROME_VERTICAL_INSET_KEY, 4);
	}

	public static int ribbonHorizontalInset() {
		return intValue(RIBBON_HORIZONTAL_INSET_KEY, 10);
	}

	public static int ribbonSearchHeight() {
		return intValue(RIBBON_SEARCH_HEIGHT_KEY, 28);
	}

	public static int ribbonSearchPreferredWidth() {
		return intValue(RIBBON_SEARCH_PREFERRED_WIDTH_KEY, 352);
	}

	public static int ribbonSearchMaxWidth() {
		return intValue(RIBBON_SEARCH_MAX_WIDTH_KEY, 392);
	}

	public static int ribbonCornerRadius() {
		return intValue(RIBBON_CORNER_RADIUS_KEY, 0);
	}

	public static int ribbonButtonArc() {
		return intValue(RIBBON_BUTTON_ARC_KEY, 0);
	}

	public static int ribbonQuickAccessButtonSize() {
		return intValue(RIBBON_QUICK_ACCESS_BUTTON_SIZE_KEY, 24);
	}

	/** Icon size inside a quick-access button (MS Project QAT uses ~16px icons). */
	public static int ribbonQuickAccessIconSize() {
		return 16;
	}

	public static int ribbonInlineButtonHeight() {
		return intValue(RIBBON_INLINE_BUTTON_HEIGHT_KEY, 24);
	}

	public static int ribbonInlineButtonMediumMinWidth() {
		return intValue(RIBBON_INLINE_BUTTON_MEDIUM_MIN_WIDTH_KEY, 96);
	}

	public static int ribbonInlineButtonSmallMinWidth() {
		return intValue(RIBBON_INLINE_BUTTON_SMALL_MIN_WIDTH_KEY, 84);
	}

	public static int ribbonBandTitleHeight() {
		return intValue(RIBBON_BAND_TITLE_HEIGHT_KEY, 9);
	}

	public static int compactSpacing() {
		Object value = UIManager.get(THEME_KEY_PREFIX + "contentSpacing");
		return value instanceof Integer ? ((Integer) value).intValue() : 10;
	}

	public static int sectionSpacing() {
		Object value = UIManager.get(THEME_KEY_PREFIX + "sectionSpacing");
		return value instanceof Integer ? ((Integer) value).intValue() : 16;
	}

	public static int dialogButtonHeight() {
		Object value = UIManager.get(THEME_KEY_PREFIX + "dialogButtonHeight");
		return value instanceof Integer ? ((Integer) value).intValue() : 30;
	}

	/**
	 * Returns preferred-height tracks for a form built with
	 * {@code DefaultFormBuilder.nextLine(2)}.
	 *
	 * Each logical form line occupies a preferred-height track followed by a
	 * small spacer track.  The trailing spacer is omitted because the builder
	 * never advances beyond the last component line.
	 */
	public static String preferredFormRows(int rowCount) {
		if (rowCount < 1)
			throw new IllegalArgumentException("rowCount must be positive");
		StringBuilder rows = new StringBuilder(rowCount * 8);
		for (int row = 0; row < rowCount; row++) {
			if (row > 0)
				rows.append(',');
			rows.append('p');
			if (row + 1 < rowCount)
				rows.append(",3dlu");
		}
		return rows.toString();
	}

	public static Color blend(Color first, Color second, float firstWeight) {
		float weight = Math.max(0f, Math.min(1f, firstWeight));
		float secondWeight = 1f - weight;
		int red = Math.round(first.getRed() * weight + second.getRed() * secondWeight);
		int green = Math.round(first.getGreen() * weight + second.getGreen() * secondWeight);
		int blue = Math.round(first.getBlue() * weight + second.getBlue() * secondWeight);
		return new Color(red, green, blue);
	}

	public static void enableAntialiasing(Graphics2D g2) {
		if (g2 == null)
			return;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static void styleToolBar(JToolBar toolBar) {
		if (toolBar == null)
			return;
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		toolBar.setOpaque(false);
		toolBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		toolBar.setMargin(new Insets(2, 6, 2, 6));
		toolBar.setBackground(panelBackground());
		toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
	}

	public static void styleToolBarButton(AbstractButton button) {
		if (button == null)
			return;
		button.setFocusable(false);
		button.putClientProperty(BUTTON_STYLE_ROLE_PROPERTY, BUTTON_STYLE_ROLE_TOOLBAR);
		button.setBorder(new CommandButtonBorder(new Insets(3, 5, 3, 5), 8));
		button.setBorderPainted(true);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(3, 5, 3, 5));
		button.setForeground(labelForeground());
	}

	public static void styleTabbedPane(JTabbedPane tabbedPane) {
		if (tabbedPane == null)
			return;
		tabbedPane.putClientProperty("JTabbedPane.tabType", "underlined");
		tabbedPane.putClientProperty("JTabbedPane.showTabSeparators", Boolean.TRUE);
		tabbedPane.putClientProperty("JTabbedPane.tabSeparatorsFullHeight", Boolean.TRUE);
		tabbedPane.putClientProperty("JTabbedPane.showContentSeparator", Boolean.FALSE);
		tabbedPane.putClientProperty("JTabbedPane.tabHeight", Integer.valueOf(36));
		tabbedPane.setOpaque(true);
		tabbedPane.setBackground(appBackground());
		tabbedPane.setForeground(tabUnselectedForeground());
	}

	public static Border focusBorder() {
		Border border = UIManager.getBorder("TextField.border");
		if (border == null)
			border = BorderFactory.createLineBorder(borderColor());
		return border;
	}

	public static Border spreadsheetActiveCellBorder() {
		return BorderFactory.createLineBorder(spreadsheetActiveCellBorderColor(), 1);
	}

	public static Border spreadsheetEditingCellBorder() {
		return BorderFactory.createLineBorder(spreadsheetActiveCellBorderColor(), 2);
	}

	public static Border tableCellBorder() {
		Border border = UIManager.getBorder("Table.cellNoFocusBorder");
		if (border == null)
			border = BorderFactory.createEmptyBorder(2, 4, 2, 4);
		return border;
	}

	public static Border withRowGridOverlay(Border baseBorder, Color gridColor) {
		return new RowGridOverlayBorder(baseBorder, gridColor == null ? tableGridColor() : gridColor);
	}

	private static final class RowGridOverlayBorder extends AbstractBorder {
		private final Border baseBorder;
		private final Color gridColor;

		private RowGridOverlayBorder(Border baseBorder, Color gridColor) {
			this.baseBorder = baseBorder;
			this.gridColor = gridColor;
		}

		@Override
		public Insets getBorderInsets(Component component) {
			return baseBorder == null ? new Insets(0, 0, 0, 0) : baseBorder.getBorderInsets(component);
		}

		@Override
		public Insets getBorderInsets(Component component, Insets target) {
			Insets resolved = getBorderInsets(component);
			target.set(resolved.top, resolved.left, resolved.bottom, resolved.right);
			return target;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			if (width <= 0 || height <= 0)
				return;
			Color oldColor = graphics.getColor();
			graphics.setColor(gridColor);
			graphics.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
			graphics.setColor(oldColor);
			if (baseBorder != null)
				baseBorder.paintBorder(component, graphics, x, y, width, height);
		}
	}

	public static Color spreadsheetAlternateRowBackground(int row) {
		Color alternate = UIManager.getColor("Table.alternateRowColor");
		if (alternate != null)
			return alternate;
		return spreadsheetBodyBackground();
	}

	public static Border spreadsheetCellBorder() {
		return tableCellBorder();
	}

	public static void applySpreadsheetTableStyle(JTable table) {
		if (table == null)
			return;
		table.setOpaque(true);
		table.setBackground(spreadsheetBodyBackground());
		table.setForeground(tableForeground());
		table.setSelectionBackground(spreadsheetRangeSelectionBackground());
		table.setSelectionForeground(tableSelectionForeground());
		table.setGridColor(spreadsheetGridColor());
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setRowMargin(0);
		table.setFillsViewportHeight(true);
	}

	public static Border tableEditorBorder() {
		Border border = UIManager.getBorder("TextField.border");
		if (border == null)
			border = BorderFactory.createLineBorder(borderColor());
		return border;
	}

	public static Border tableHeaderBorder() {
		Border border = UIManager.getBorder("TableHeader.cellBorder");
		if (border == null)
			border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor());
		return border;
	}

	public static FontUIResource asFontUIResource(Font font) {
		return font == null ? null : new FontUIResource(font);
	}

	public static void applyDataSurface(JComponent component) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setBackground(dataSurfaceBackground());
	}

	public static void applyPanelSurface(JComponent component) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setBackground(panelBackground());
		component.setForeground(labelForeground());
	}

	public static void applyWorkspaceSurface(JComponent component) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setBackground(workspaceBackground());
		component.setForeground(labelForeground());
	}

	public static void applyViewportSurface(JViewport viewport) {
		if (viewport == null)
			return;
		viewport.setOpaque(true);
		viewport.setBackground(viewportBackground());
	}

	public static void styleDialogRoot(JRootPane rootPane) {
		if (rootPane == null)
			return;
		rootPane.setBorder(BorderFactory.createLineBorder(borderColor()));
	}

	public static void styleDialogContent(JComponent component) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setBackground(dialogBackground());
		component.setForeground(labelForeground());
	}

	/**
	 * Applies the shared FlatLaf dialog contract to a dialog assembled from
	 * ordinary Swing components.  Legacy dialogs cannot all extend
	 * {@link com.microproject.dialog.AbstractDialog}, so this is the single
	 * migration path for their component tree.
	 */
	public static void styleDialogComponents(Component component) {
		if (component == null)
			return;
		if (component instanceof JTable table)
			applySpreadsheetTableStyle(table);
		else if (component instanceof JTabbedPane tabs)
			styleTabbedPane(tabs);
		else if (component instanceof JScrollPane scrollPane) {
			applyPanelSurface(scrollPane);
			applyViewportSurface(scrollPane.getViewport());
		}
		else if (component instanceof JSplitPane splitPane) {
			applyPanelSurface(splitPane);
			splitPane.setBorder(BorderFactory.createEmptyBorder());
		}
		else if (component instanceof JToolBar toolBar)
			styleToolBar(toolBar);
		else if (component instanceof JButton button)
			styleDialogButton(button, false);
		else if (component instanceof JComboBox<?> comboBox)
			styleDialogControl(comboBox);
		else if (component instanceof JSpinner spinner)
			styleDialogControl(spinner);
		else if (component instanceof JList<?> list)
			styleDialogList(list);
		else if (component instanceof JTextComponent textComponent)
			styleDialogControl(textComponent);
		else if (component instanceof JLabel label)
			label.setForeground(labelForeground());
		else if (component instanceof JPanel panel)
			applyPanelSurface(panel);

		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				styleDialogComponents(child);
			}
		}
	}

	private static void styleDialogControl(JComponent component) {
		component.setFont(uiFont());
		component.setForeground(labelForeground());
	}

	private static void styleDialogList(JList<?> list) {
		list.setFont(uiFont());
		list.setForeground(tableForeground());
		list.setBackground(dataSurfaceBackground());
		list.setSelectionBackground(tableSelectionBackground());
		list.setSelectionForeground(tableSelectionForeground());
	}

	public static void styleButtonPanel(JPanel panel) {
		if (panel == null)
			return;
		panel.setOpaque(true);
		panel.setBackground(dialogBackground());
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, separatorColor()),
			BorderFactory.createEmptyBorder(compactSpacing(), sectionSpacing(), compactSpacing(), sectionSpacing())));
	}

	public static void styleDialogButton(AbstractButton button, boolean primary) {
		if (button == null)
			return;
		button.setFocusPainted(false);
		button.setMargin(new Insets(4, 12, 4, 12));
		if (primary) {
			button.setOpaque(true);
			button.setForeground(Color.WHITE);
			button.setBackground(accentColor());
			button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(accentColor().darker()),
				BorderFactory.createEmptyBorder(0, 8, 0, 8)));
		} else {
			button.setOpaque(true);
			button.setForeground(labelForeground());
			button.setBackground(dialogSurfaceBackground());
			button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(borderColor()),
				BorderFactory.createEmptyBorder(0, 8, 0, 8)));
		}
		// The theme height is a minimum. Font metrics, margin, and border
		// insets determine the actual content height, especially for Japanese
		// text and when the platform scales fonts independently of UI pixels.
		Dimension preferred = button.getPreferredSize();
		int width = Math.max(preferred.width, 92);
		int height = Math.max(preferred.height, dialogButtonHeight());
		button.setPreferredSize(new Dimension(width, height));
		button.setMinimumSize(new Dimension(92, height));
	}

	public static void applyTableHeaderStyle(JComponent component) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setForeground(headerForeground());
		component.setBackground(spreadsheetHeaderBackground());
		component.setFont(ganttHeaderFont());
		component.setBorder(tableHeaderBorder());
	}

	public static void applyTableHeaderCellStyle(JLabel component, boolean selected) {
		applyTableHeaderCellStyle(component, selected, false);
	}

	public static void applyTableHeaderCellStyle(JLabel component, boolean selected, boolean active) {
		if (component == null)
			return;
		component.setOpaque(true);
		component.setForeground(headerForeground());
		component.setBackground((selected || active) ? spreadsheetHeaderSelectedBackground() : spreadsheetHeaderBackground());
		component.setFont(headerFont());
		component.setBorder(active ? spreadsheetActiveCellBorder() : tableHeaderBorder());
	}

	private static final class CommandButtonBorder extends AbstractBorder {
		private final Insets insets;
		private final int arc;

		private CommandButtonBorder(Insets insets, int arc) {
			this.insets = insets;
			this.arc = arc;
		}

		@Override
		public Insets getBorderInsets(Component component) {
			int splitInset = isRibbonSplit(component) ? 18 : 0;
			return new Insets(insets.top, insets.left, insets.bottom, insets.right + splitInset);
		}

		@Override
		public Insets getBorderInsets(Component component, Insets insetsTarget) {
			insetsTarget.top = insets.top;
			insetsTarget.left = insets.left;
			insetsTarget.bottom = insets.bottom;
			insetsTarget.right = insets.right + (isRibbonSplit(component) ? 18 : 0);
			return insetsTarget;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			if (!(component instanceof AbstractButton button) || width <= 0 || height <= 0)
				return;
			Color border = resolveCommandButtonBorderColor(button);
			if (!button.isEnabled() || border == null)
				return;
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				enableAntialiasing(g2);
				int right = x + width - 1;
				int bottom = y + height - 1;
				g2.setColor(border);
				g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
				if (isRibbonSplit(button)) {
					int splitX = x + width - 18;
					g2.setColor(separatorColor());
					g2.drawLine(splitX, y + 4, splitX, y + height - 5);
					int centerX = splitX + 9;
					int centerY = y + height / 2;
					g2.setColor(button.isEnabled() ? labelForeground() : disabledForeground());
					g2.drawLine(centerX - 3, centerY - 1, centerX, centerY + 2);
					g2.drawLine(centerX, centerY + 2, centerX + 3, centerY - 1);
				}
			} finally {
				g2.dispose();
			}
		}

		private boolean isRibbonSplit(Component component) {
			return component instanceof JComponent jc
				&& Boolean.TRUE.equals(jc.getClientProperty("MicroProject.ribbonSplit"));
		}
	}

}
