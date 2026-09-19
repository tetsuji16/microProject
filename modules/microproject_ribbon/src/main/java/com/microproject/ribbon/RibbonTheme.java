/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.swing.AbstractButton;

/**
 * Host-provided visual contract for a ribbon. Implementations may use FlatLaf,
 * Synth, or another Swing look-and-feel without leaking that dependency here.
 */
public interface RibbonTheme {
	Color chromeBackground();
	Color topLineColor();
	Color surfaceColor();
	Color surfaceBorderColor();
	Color bandSeparatorColor();
	Color panelBackground();
	Color tabSelectedForeground();
	Color tabUnselectedForeground();
	Color bandTitleForeground();
	Font buttonFont();
	Font tabFont();
	Font bandTitleFont();
	int tabHeight();
	int surfaceHeight();
	int bandTitleHeight();
	int largeButtonHeight();
	int largeButtonMinWidth();
	int inlineButtonHeight();
	int inlineButtonSmallMinWidth();
	int inlineButtonMediumMinWidth();
	int buttonVerticalInset();
	int horizontalInset();
	int cornerRadius();
	void styleLargeButton(AbstractButton button);
	void styleSmallButton(AbstractButton button);
	void styleTabButton(AbstractButton button);
	void enableAntialiasing(Graphics2D graphics);
}
