/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.swing.AbstractButton;
import com.microproject.ribbon.RibbonTheme;
import com.microproject.util.FlatUiSupport;

/** FlatLaf-backed adapter; all look-and-feel details stay in the UI module. */
final class FlatLafRibbonTheme implements RibbonTheme {
	public Color chromeBackground() { return FlatUiSupport.ribbonChromeBackground(); }
	public Color topLineColor() { return FlatUiSupport.ribbonTopLineColor(); }
	public Color surfaceColor() { return FlatUiSupport.ribbonSurfaceColor(); }
	public Color surfaceBorderColor() { return FlatUiSupport.ribbonSurfaceBorderColor(); }
	public Color bandSeparatorColor() { return FlatUiSupport.ribbonBandSeparatorColor(); }
	public Color panelBackground() { return FlatUiSupport.panelBackground(); }
	public Color tabSelectedForeground() { return FlatUiSupport.tabSelectedForeground(); }
	public Color tabUnselectedForeground() { return FlatUiSupport.tabUnselectedForeground(); }
	public Color bandTitleForeground() { return FlatUiSupport.ribbonBandTitleForeground(); }
	public Font buttonFont() { return FlatUiSupport.ribbonButtonFont(); }
	public Font tabFont() { return FlatUiSupport.ribbonTabFont(); }
	public Font bandTitleFont() { return FlatUiSupport.ribbonBandTitleFont(); }
	public int tabHeight() { return FlatUiSupport.ribbonTabHeight(); }
	public int surfaceHeight() { return FlatUiSupport.ribbonSurfaceHeight(); }
	public int bandTitleHeight() { return FlatUiSupport.ribbonBandTitleHeight(); }
	public int largeButtonHeight() { return FlatUiSupport.ribbonLargeButtonHeight(); }
	public int largeButtonMinWidth() { return FlatUiSupport.ribbonLargeButtonMinWidth(); }
	public int inlineButtonHeight() { return FlatUiSupport.ribbonInlineButtonHeight(); }
	public int inlineButtonSmallMinWidth() { return FlatUiSupport.ribbonInlineButtonSmallMinWidth(); }
	public int inlineButtonMediumMinWidth() { return FlatUiSupport.ribbonInlineButtonMediumMinWidth(); }
	public int buttonVerticalInset() { return FlatUiSupport.ribbonButtonVerticalInset(); }
	public int horizontalInset() { return FlatUiSupport.ribbonHorizontalInset(); }
	public int cornerRadius() { return FlatUiSupport.ribbonCornerRadius(); }
	public void styleLargeButton(AbstractButton button) { FlatUiSupport.styleRibbonLargeButton(button); }
	public void styleSmallButton(AbstractButton button) { FlatUiSupport.styleRibbonSmallButton(button); }
	public void styleTabButton(AbstractButton button) { FlatUiSupport.styleRibbonTabButton(button); }
	public void enableAntialiasing(Graphics2D graphics) { FlatUiSupport.enableAntialiasing(graphics); }
}
