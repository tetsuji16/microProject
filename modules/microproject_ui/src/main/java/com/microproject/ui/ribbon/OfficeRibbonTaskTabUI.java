/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import org.pushingpixels.flamingo.internal.ui.common.BasicCommandToggleButtonUI;
import org.pushingpixels.flamingo.internal.ui.ribbon.JRibbonTaskToggleButton;

import com.microproject.util.FlatUiSupport;

/** Flamingo task tab presentation following the flat Office tab strip. */
final class OfficeRibbonTaskTabUI extends BasicCommandToggleButtonUI {
	public static ComponentUI createUI(JComponent component) {
		return new OfficeRibbonTaskTabUI();
	}

	@Override
	protected void paintButtonBackground(Graphics graphics, Rectangle bounds, ButtonModel... models) {
		if (!(commandButton instanceof JRibbonTaskToggleButton)) {
			super.paintButtonBackground(graphics, bounds, models);
			return;
		}

		ButtonModel model = commandButton.getActionModel();
		boolean selected = model.isSelected();
		boolean pressed = model.isArmed() && model.isPressed();
		boolean hovered = model.isRollover() && !pressed;
		if (hovered || pressed) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color background = pressed
					? FlatUiSupport.chromeButtonPressedBackground()
					: FlatUiSupport.ribbonTabHoverColor();
				g2.setColor(background);
				g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			} finally {
				g2.dispose();
			}
		}

		if (selected) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setColor(FlatUiSupport.ribbonTabUnderlineColor());
				g2.fillRect(bounds.x, bounds.y + bounds.height - 3, bounds.width, 3);
			} finally {
				g2.dispose();
			}
		}

		if (commandButton.hasFocus()) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			try {
				g2.setColor(FlatUiSupport.ribbonAccentColor());
				g2.drawRect(bounds.x + 2, bounds.y + 2, Math.max(0, bounds.width - 5), Math.max(0, bounds.height - 6));
			} finally {
				g2.dispose();
			}
		}
	}
}
