/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Draws the KeyTip badges over the window. It lives in the layered pane, so the ribbon
 * layout never sees it, and it takes no mouse input.
 */
final class RibbonKeyTipOverlay extends JComponent {
	static final String NAME = "microProjectRibbonKeyTipOverlay";
	private static final int PADDING_X = 4;
	private static final int PADDING_Y = 1;

	private final Supplier<Map<String, RibbonKeyTipSession.Target>> badges;

	RibbonKeyTipOverlay(Supplier<Map<String, RibbonKeyTipSession.Target>> badges) {
		this.badges = Objects.requireNonNull(badges);
		setName(NAME);
		setOpaque(false);
		setFocusable(false);
	}

	@Override
	public boolean contains(int x, int y) {
		return false;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			Font font = getFont() == null ? g2.getFont() : getFont();
			g2.setFont(font.deriveFont(Font.BOLD, 11f));
			FontMetrics metrics = g2.getFontMetrics();
			for (Map.Entry<String, RibbonKeyTipSession.Target> badge : badges.get().entrySet()) {
				JComponent anchor = badge.getValue().anchor();
				if (anchor == null || !anchor.isShowing()) {
					continue;
				}
				String text = badge.getKey();
				Dimension size = new Dimension(metrics.stringWidth(text) + 2 * PADDING_X,
					metrics.getHeight() + 2 * PADDING_Y);
				Point origin = SwingUtilities.convertPoint(anchor, 0, 0, this);
				int x = origin.x + (anchor.getWidth() - size.width) / 2;
				int y = origin.y + anchor.getHeight() - size.height / 2 - 2;
				g2.setColor(new Color(0x2B2B2B));
				g2.fillRoundRect(x, y, size.width, size.height, 4, 4);
				g2.setColor(Color.WHITE);
				g2.drawString(text, x + PADDING_X, y + PADDING_Y + metrics.getAscent());
			}
		} finally {
			g2.dispose();
		}
	}
}
