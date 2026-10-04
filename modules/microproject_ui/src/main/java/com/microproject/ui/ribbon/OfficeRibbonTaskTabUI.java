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
				// Office leaves a small ribbon-surface inset below its 2 px tab
				// indicator; pinning the line to the component edge makes it read
				// like a selected button instead of the reference tab treatment.
				g2.fillRect(bounds.x, bounds.y + bounds.height - 5, bounds.width, 2);
			} finally {
				g2.dispose();
			}
		}
	}
}
