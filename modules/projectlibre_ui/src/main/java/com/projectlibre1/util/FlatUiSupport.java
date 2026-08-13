package com.projectlibre1.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeListener;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;

/**
 * Small FlatLaf-friendly UI helpers for shared Swing styling.
 */
public final class FlatUiSupport {
	public static final String BUTTON_STYLE_ROLE_PROPERTY = "ProjectLibre.buttonStyleRole";
	public static final String BUTTON_STYLE_ROLE_TOOLBAR = "toolbar";
	public static final String BUTTON_STYLE_ROLE_RIBBON_LARGE = "ribbonLarge";
	public static final String BUTTON_STYLE_ROLE_RIBBON_SMALL = "ribbonSmall";
	public static final String BUTTON_STYLE_ROLE_RIBBON_TAB = "ribbonTab";
	private static final String RIBBON_CHROME_BACKGROUND_KEY = "ProjectLibre.ribbonChromeBackground";
	private static final String RIBBON_SURFACE_BACKGROUND_KEY = "ProjectLibre.ribbonSurfaceBackground";
	private static final String RIBBON_ACCENT_COLOR_KEY = "ProjectLibre.ribbonAccentColor";
	private static final String RIBBON_CHROME_HEIGHT_KEY = "ProjectLibre.ribbonChromeHeight";
	private static final String RIBBON_CHROME_VERTICAL_INSET_KEY = "ProjectLibre.ribbonChromeVerticalInset";
	private static final String RIBBON_HORIZONTAL_INSET_KEY = "ProjectLibre.ribbonHorizontalInset";
	private static final String RIBBON_TAB_HEIGHT_KEY = "ProjectLibre.ribbonTabHeight";
	private static final String RIBBON_TAB_HORIZONTAL_PADDING_KEY = "ProjectLibre.ribbonTabHorizontalPadding";
	private static final String RIBBON_TAB_VERTICAL_PADDING_KEY = "ProjectLibre.ribbonTabVerticalPadding";
	private static final String RIBBON_SURFACE_HEIGHT_KEY = "ProjectLibre.ribbonSurfaceHeight";
	private static final String RIBBON_BAND_VERTICAL_INSET_KEY = "ProjectLibre.ribbonBandVerticalInset";
	private static final String RIBBON_BUTTON_VERTICAL_INSET_KEY = "ProjectLibre.ribbonButtonVerticalInset";
	private static final String RIBBON_SEARCH_HEIGHT_KEY = "ProjectLibre.ribbonSearchHeight";
	private static final String RIBBON_SEARCH_PREFERRED_WIDTH_KEY = "ProjectLibre.ribbonSearchPreferredWidth";
	private static final String RIBBON_SEARCH_MAX_WIDTH_KEY = "ProjectLibre.ribbonSearchMaxWidth";
	private static final String RIBBON_CORNER_RADIUS_KEY = "ProjectLibre.ribbonCornerRadius";
	private static final String RIBBON_BUTTON_ARC_KEY = "ProjectLibre.ribbonButtonArc";
	private static final String RIBBON_QUICK_ACCESS_BUTTON_SIZE_KEY = "ProjectLibre.ribbonQuickAccessButtonSize";
	private static final String RIBBON_LARGE_BUTTON_HEIGHT_KEY = "ProjectLibre.ribbonLargeButtonHeight";
	private static final String RIBBON_LARGE_BUTTON_MIN_WIDTH_KEY = "ProjectLibre.ribbonLargeButtonMinWidth";
	private static final String RIBBON_INLINE_BUTTON_HEIGHT_KEY = "ProjectLibre.ribbonInlineButtonHeight";
	private static final String RIBBON_INLINE_BUTTON_MEDIUM_MIN_WIDTH_KEY = "ProjectLibre.ribbonInlineButtonMediumMinWidth";
	private static final String RIBBON_INLINE_BUTTON_SMALL_MIN_WIDTH_KEY = "ProjectLibre.ribbonInlineButtonSmallMinWidth";
	private static final String RIBBON_BAND_TITLE_HEIGHT_KEY = "ProjectLibre.ribbonBandTitleHeight";

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
		return uiFont().deriveFont(Font.BOLD);
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
		return color("Panel.background", FlatUiTheme.APP_BACKGROUND);
	}

	public static Color workspaceBackground() {
		return color("ProjectLibre.workspaceBackground", FlatUiTheme.APP_BACKGROUND);
	}

	public static Color dialogBackground() {
		return color("ProjectLibre.dialogBackground", FlatUiTheme.APP_BACKGROUND);
	}

	public static Color dialogSurfaceBackground() {
		return color("ProjectLibre.dialogSurfaceBackground", Color.WHITE);
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
			color = FlatUiTheme.APP_BACKGROUND;
		return color;
	}

	public static Color ribbonChromeBackground() {
		return color(RIBBON_CHROME_BACKGROUND_KEY, FlatUiTheme.RIBBON_CHROME_BACKGROUND);
	}

	public static Color tableBackground() {
		return FlatUiTheme.TABLE_BACKGROUND;
	}

	public static Color tableContentBackground() {
		return FlatUiTheme.TABLE_CONTENT_BACKGROUND;
	}

	public static Color tableForeground() {
		return color("Table.foreground", FlatUiTheme.TABLE_FOREGROUND);
	}

	public static Color tableSelectionBackground() {
		return color("Table.selectionBackground", FlatUiTheme.TABLE_SELECTION_BACKGROUND);
	}

	public static Color tableSelectionForeground() {
		return color("Table.selectionForeground", FlatUiTheme.TABLE_SELECTION_FOREGROUND);
	}

	public static Color headerBackground() {
		return color("TableHeader.background", FlatUiTheme.HEADER_BACKGROUND);
	}

	public static Color headerForeground() {
		return color("TableHeader.foreground", FlatUiTheme.HEADER_FOREGROUND);
	}

	public static Color disabledForeground() {
		return color("Label.disabledForeground", FlatUiTheme.DISABLED_FOREGROUND);
	}

	public static Color infoForeground() {
		return color("TextField.foreground", FlatUiTheme.INFO_FOREGROUND);
	}

	public static Color errorForeground() {
		Color color = UIManager.getColor("Actions.Red");
		if (color == null)
			color = UIManager.getColor("Component.errorFocusColor");
		if (color == null)
			color = FlatUiTheme.ERROR;
		return color;
	}

	public static Color labelForeground() {
		return color("Label.foreground", FlatUiTheme.LABEL_FOREGROUND);
	}

	public static Color borderColor() {
		return color("Component.borderColor", FlatUiTheme.BORDER);
	}

	public static Color separatorColor() {
		return color("Separator.foreground", FlatUiTheme.SEPARATOR);
	}

	public static Color ribbonTopLineColor() {
		return new Color(0xD1D1D1);
	}

	public static Color ribbonSurfaceColor() {
		return color(RIBBON_SURFACE_BACKGROUND_KEY, surfaceBackground());
	}

	public static Color ribbonSurfaceBorderColor() {
		return new Color(0xD1D1D1);
	}

	public static Color ribbonAccentColor() {
		return color(RIBBON_ACCENT_COLOR_KEY, new Color(0x0F6CBD));
	}

	public static Color ribbonSelectedTabColor() {
		return ribbonChromeBackground();
	}

	public static Color ribbonTabHoverColor() {
		return new Color(0xEAF3FF);
	}

	public static Color ribbonTabBorderHoverColor() {
		return new Color(0xB9D7F5);
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
		return color("Table.gridColor", FlatUiTheme.SPREADSHEET_GRID);
	}

	public static Color ganttHeaderGridColor() {
		return spreadsheetGridColor();
	}

	public static Color spreadsheetBodyBackground() {
		return color("ProjectLibre.spreadsheetBodyBackground", FlatUiTheme.SPREADSHEET_BODY_BACKGROUND);
	}

	public static Color spreadsheetReadOnlyForeground() {
		return color("ProjectLibre.spreadsheetReadOnlyForeground", FlatUiTheme.SPREADSHEET_READ_ONLY_FOREGROUND);
	}

	public static Color spreadsheetHeaderBackground() {
		return color("ProjectLibre.spreadsheetHeaderBackground", FlatUiTheme.SPREADSHEET_HEADER_BACKGROUND);
	}

	public static Color spreadsheetHeaderSelectedBackground() {
		return color("ProjectLibre.spreadsheetHeaderSelectedBackground", FlatUiTheme.SPREADSHEET_HEADER_SELECTED_BACKGROUND);
	}

	public static Color spreadsheetRangeSelectionBackground() {
		return color("ProjectLibre.spreadsheetRangeSelectionBackground", FlatUiTheme.SPREADSHEET_RANGE_SELECTION_BACKGROUND);
	}

	public static Color spreadsheetActiveCellBorderColor() {
		return color("ProjectLibre.spreadsheetActiveCellBorder", FlatUiTheme.SPREADSHEET_ACTIVE_CELL_BORDER);
	}

	public static Color spreadsheetGridColor() {
		return color("ProjectLibre.spreadsheetGridColor", FlatUiTheme.SPREADSHEET_GRID);
	}

	public static Color ribbonBandSeparatorColor() {
		return new Color(0xD8E0EA);
	}

	public static Color ribbonBandTitleForeground() {
		return new Color(0x616161);
	}

	public static Color ribbonIconColor() {
		return new Color(0x323130);
	}

	public static Color ribbonIconHoverColor() {
		return ribbonAccentColor();
	}

	public static Color ribbonIconSelectedColor() {
		return ribbonAccentColor();
	}

	public static Color ribbonIconDisabledColor() {
		return new Color(0xA19F9D);
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
			color = FlatUiTheme.ACCENT;
		return color;
	}

	private static Color buttonStyleBaseBackground(AbstractButton button) {
		Object role = button == null ? null : button.getClientProperty(BUTTON_STYLE_ROLE_PROPERTY);
		if (BUTTON_STYLE_ROLE_TOOLBAR.equals(role))
			return panelBackground();
		if (BUTTON_STYLE_ROLE_RIBBON_TAB.equals(role))
			return ribbonChromeBackground();
		return ribbonSurfaceColor();
	}

	private static Color buttonAccentColor(AbstractButton button) {
		Object role = button == null ? null : button.getClientProperty(BUTTON_STYLE_ROLE_PROPERTY);
		return BUTTON_STYLE_ROLE_RIBBON_LARGE.equals(role) || BUTTON_STYLE_ROLE_RIBBON_SMALL.equals(role)
			? ribbonAccentColor()
			: accentColor();
	}

	private static boolean isRibbonCommandButton(AbstractButton button) {
		Object role = button == null ? null : button.getClientProperty(BUTTON_STYLE_ROLE_PROPERTY);
		return BUTTON_STYLE_ROLE_RIBBON_LARGE.equals(role) || BUTTON_STYLE_ROLE_RIBBON_SMALL.equals(role);
	}

	/**
	 * Installs state-aware background painting for ribbon commands. Swing paints
	 * a button's border after its content (including the icon), so a filled
	 * border would cover the icon in pressed/selected states. Let the button UI
	 * paint the background first, then let the icon and label paint normally.
	 */
	private static void installRibbonCommandStatePainting(AbstractButton button) {
		if (button.getClientProperty("ProjectLibre.ribbonStateChangeListener") != null) {
			updateRibbonCommandStatePainting(button);
			return;
		}
		ChangeListener listener = event -> updateRibbonCommandStatePainting(button);
		button.getModel().addChangeListener(listener);
		button.putClientProperty("ProjectLibre.ribbonStateChangeListener", listener);
		button.addPropertyChangeListener("enabled", event -> updateRibbonCommandStatePainting(button));
		updateRibbonCommandStatePainting(button);
	}

	private static void updateRibbonCommandStatePainting(AbstractButton button) {
		if (button == null) {
			return;
		}
		Color fill = resolveCommandButtonBackground(button);
		button.setBackground(fill == null ? ribbonSurfaceColor() : fill);
		button.setContentAreaFilled(fill != null);
		button.setOpaque(fill != null);
	}

	private static boolean supportsPersistentSelectedState(AbstractButton button) {
		return button instanceof JToggleButton && !BUTTON_STYLE_ROLE_RIBBON_TAB.equals(button.getClientProperty(BUTTON_STYLE_ROLE_PROPERTY));
	}

	public static Color commandButtonHoverBackground(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return new Color(0xEAF3FF);
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.08f);
	}

	public static Color commandButtonPressedBackground(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return new Color(0xCFE8FF);
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.16f);
	}

	public static Color commandButtonSelectedBackground(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return new Color(0xDCEEFF);
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.14f);
	}

	public static Color commandButtonHoverBorderColor(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return null;
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.38f);
	}

	public static Color commandButtonPressedBorderColor(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return null;
		return blend(buttonAccentColor(button), buttonStyleBaseBackground(button), 0.50f);
	}

	public static Color commandButtonSelectedBorderColor(AbstractButton button) {
		if (isRibbonCommandButton(button))
			return ribbonAccentColor();
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

	public static Color resolveRibbonTabBackground(AbstractButton button) {
		if (button == null || !button.isEnabled())
			return null;
		ButtonModel model = button.getModel();
		if (model == null || model.isSelected())
			return null;
		if (model.isPressed() || model.isArmed())
			return new Color(0xE5F1FB);
		if (model.isRollover())
			return ribbonTabHoverColor();
		return null;
	}

	public static Color resolveRibbonTabBorderColor(AbstractButton button) {
		if (button == null || !button.isEnabled())
			return null;
		ButtonModel model = button.getModel();
		if (model == null || model.isSelected())
			return null;
		return null;
	}

	public static Color resolveRibbonTabUnderlineColor(AbstractButton button) {
		return button != null && button.isSelected() ? ribbonTabUnderlineColor() : null;
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

	public static int ribbonTabHeight() {
		return intValue(RIBBON_TAB_HEIGHT_KEY, 28);
	}

	public static int ribbonTabHorizontalPadding() {
		return intValue(RIBBON_TAB_HORIZONTAL_PADDING_KEY, 10);
	}

	public static int ribbonTabVerticalPadding() {
		return intValue(RIBBON_TAB_VERTICAL_PADDING_KEY, 4);
	}

	public static int ribbonSurfaceHeight() {
		return intValue(RIBBON_SURFACE_HEIGHT_KEY, 96);
	}

	public static int ribbonBandVerticalInset() {
		return intValue(RIBBON_BAND_VERTICAL_INSET_KEY, 1);
	}

	public static int ribbonButtonVerticalInset() {
		return intValue(RIBBON_BUTTON_VERTICAL_INSET_KEY, 4);
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

	public static int ribbonLargeButtonHeight() {
		return intValue(RIBBON_LARGE_BUTTON_HEIGHT_KEY, 90);
	}

	public static int ribbonLargeButtonMinWidth() {
		return intValue(RIBBON_LARGE_BUTTON_MIN_WIDTH_KEY, 64);
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
		Object value = UIManager.get("ProjectLibre.contentSpacing");
		return value instanceof Integer ? ((Integer) value).intValue() : 10;
	}

	public static int sectionSpacing() {
		Object value = UIManager.get("ProjectLibre.sectionSpacing");
		return value instanceof Integer ? ((Integer) value).intValue() : 16;
	}

	public static int dialogButtonHeight() {
		Object value = UIManager.get("ProjectLibre.dialogButtonHeight");
		return value instanceof Integer ? ((Integer) value).intValue() : 30;
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

	public static void styleRibbonTabButton(AbstractButton button) {
		if (button == null)
			return;
		button.setFocusable(false);
		button.putClientProperty(BUTTON_STYLE_ROLE_PROPERTY, BUTTON_STYLE_ROLE_RIBBON_TAB);
		button.setOpaque(false);
		button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		button.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
		button.setBorder(new RibbonTabBorder());
		button.setBackground(ribbonChromeBackground());
		button.setForeground(tabUnselectedForeground());
		button.setContentAreaFilled(false);
		button.setBorderPainted(true);
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		installRibbonTabStatePainting(button);
	}

	/**
	 * Paint a tab's rollover fill as button content rather than as part of its
	 * border. Swing paints borders after the label, so painting the fill from
	 * {@link RibbonTabBorder} would cover the label while the tab is hovered.
	 */
	private static void installRibbonTabStatePainting(AbstractButton button) {
		if (button.getClientProperty("ProjectLibre.ribbonTabStateChangeListener") != null) {
			updateRibbonTabStatePainting(button);
			return;
		}
		ChangeListener listener = event -> updateRibbonTabStatePainting(button);
		button.getModel().addChangeListener(listener);
		button.putClientProperty("ProjectLibre.ribbonTabStateChangeListener", listener);
		button.addPropertyChangeListener("enabled", event -> updateRibbonTabStatePainting(button));
		updateRibbonTabStatePainting(button);
	}

	private static void updateRibbonTabStatePainting(AbstractButton button) {
		Color fill = resolveRibbonTabBackground(button);
		button.setBackground(fill == null ? ribbonChromeBackground() : fill);
		button.setContentAreaFilled(fill != null);
		button.setOpaque(fill != null);
	}

	public static void styleRibbonLargeButton(AbstractButton button) {
		if (button == null)
			return;
		button.setFocusable(false);
		button.putClientProperty(BUTTON_STYLE_ROLE_PROPERTY, BUTTON_STYLE_ROLE_RIBBON_LARGE);
		button.setOpaque(false);
		button.setBackground(ribbonSurfaceColor());
		button.setForeground(labelForeground());
		button.setBorder(new CommandButtonBorder(new Insets(2, 5, 2, 5), ribbonButtonArc()));
		button.setContentAreaFilled(false);
		button.setBorderPainted(true);
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(2, 2, 2, 2));
		installRibbonCommandStatePainting(button);
	}

	public static void styleRibbonSmallButton(AbstractButton button) {
		if (button == null)
			return;
		button.setFocusable(false);
		button.putClientProperty(BUTTON_STYLE_ROLE_PROPERTY, BUTTON_STYLE_ROLE_RIBBON_SMALL);
		button.setOpaque(false);
		button.setBackground(ribbonSurfaceColor());
		button.setForeground(labelForeground());
		button.setBorder(new CommandButtonBorder(new Insets(1, 3, 1, 3), ribbonButtonArc()));
		button.setContentAreaFilled(false);
		button.setBorderPainted(true);
		button.setFocusPainted(false);
		button.setRolloverEnabled(true);
		button.setMargin(new Insets(1, 2, 1, 2));
		installRibbonCommandStatePainting(button);
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
		button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 92), dialogButtonHeight()));
		button.setMinimumSize(new Dimension(92, dialogButtonHeight()));
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
				&& Boolean.TRUE.equals(jc.getClientProperty("ProjectLibre.ribbonSplit"));
		}
	}

	private static final class RibbonTabBorder extends AbstractBorder {
		@Override
		public Insets getBorderInsets(Component component) {
			return new Insets(
				ribbonTabVerticalPadding(),
				ribbonTabHorizontalPadding(),
				Math.max(1, ribbonTabVerticalPadding() - 1) + 2,
				ribbonTabHorizontalPadding());
		}

		@Override
		public Insets getBorderInsets(Component component, Insets insetsTarget) {
			Insets computed = getBorderInsets(component);
			insetsTarget.top = computed.top;
			insetsTarget.left = computed.left;
			insetsTarget.bottom = computed.bottom;
			insetsTarget.right = computed.right;
			return insetsTarget;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
			if (!(component instanceof AbstractButton button) || width <= 0 || height <= 0)
				return;
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				enableAntialiasing(g2);
				Color border = resolveRibbonTabBorderColor(button);
				Color underline = resolveRibbonTabUnderlineColor(button);
				if (underline != null) {
					g2.setColor(underline);
					g2.fillRect(x, y + height - 3, width, 3);
				}
			} finally {
				g2.dispose();
			}
		}
	}
}
