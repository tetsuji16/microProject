/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Square check box glyph of a {@code CHECK_BOX} ribbon control. The glyph reads the
 * selected and enabled state of the button it is painted on, so one instance serves
 * every button state and the check mark always reflects the model-driven selection.
 */
final class RibbonCheckBoxIcon implements Icon {
	private final int size;

	RibbonCheckBoxIcon(int size) {
		this.size = size;
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
	public void paintIcon(Component component, Graphics graphics, int x, int y) {
		boolean selected = component instanceof AbstractButton button && button.isSelected();
		boolean enabled = component == null || component.isEnabled();
		Color foreground = component == null ? Color.DARK_GRAY : component.getForeground();
		Color background = UIManager.getColor("TextField.background");
		if (background == null) {
			background = Color.WHITE;
		}
		Color outline = enabled ? foreground : UIManager.getColor("Button.disabledText");
		if (outline == null) {
			outline = Color.GRAY;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int box = size - 3;
			int left = x + 1;
			int top = y + 1;
			g.setColor(background);
			g.fillRect(left, top, box, box);
			g.setColor(outline);
			g.setStroke(new BasicStroke(1f));
			g.drawRect(left, top, box, box);
			if (selected) {
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				int midX = left + box * 2 / 5;
				int bottom = top + box - 3;
				g.drawLine(left + 3, top + box / 2, midX, bottom);
				g.drawLine(midX, bottom, left + box - 2, top + 3);
			}
		} finally {
			g.dispose();
		}
	}
}
