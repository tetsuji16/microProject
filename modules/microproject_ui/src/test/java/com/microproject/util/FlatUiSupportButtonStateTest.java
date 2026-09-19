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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JToggleButton;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.IconManager;

class FlatUiSupportButtonStateTest {
	@Test
	void toolbarActionButtonsUseHoverStateButIgnorePersistentSelectionWhenNotToggle() {
		JButton button = new JButton("Save");
		FlatUiSupport.styleToolBarButton(button);

		assertEquals(
			FlatUiSupport.BUTTON_STYLE_ROLE_TOOLBAR,
			button.getClientProperty(FlatUiSupport.BUTTON_STYLE_ROLE_PROPERTY));

		button.getModel().setRollover(true);
		assertEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));

		button.getModel().setRollover(false);
		button.getModel().setSelected(true);
		assertNull(FlatUiSupport.resolveCommandButtonBackground(button));
	}

	@Test
	void toggleButtonsPreferSelectedStateOverHover() {
		JToggleButton button = new JToggleButton("Toggle");
		FlatUiSupport.styleRibbonSmallButton(button);

		button.getModel().setRollover(true);
		assertEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));

		button.getModel().setSelected(true);
		assertEquals(
			FlatUiSupport.commandButtonSelectedBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));
		assertNotEquals(
			FlatUiSupport.commandButtonHoverBackground(button),
			FlatUiSupport.resolveCommandButtonBackground(button));
	}

	@Test
	void ribbonCommandStateBackgroundIsPaintedByButtonBeforeIcon() {
		JToggleButton button = new JToggleButton("Toggle");
		FlatUiSupport.styleRibbonSmallButton(button);

		assertFalse(button.isContentAreaFilled());
		button.getModel().setSelected(true);
		assertTrue(button.isContentAreaFilled());
		assertEquals(
			FlatUiSupport.commandButtonSelectedBackground(button),
			button.getBackground());

		button.getModel().setSelected(false);
		assertFalse(button.isContentAreaFilled());
	}

	@Test
	void selectedRibbonCommandPaintsItsIconAboveTheSelectionBackground() {
		JToggleButton button = new JToggleButton("Language");
		button.setIcon(IconManager.getRibbonIcon("ribbon.locale", 32, 32));
		FlatUiSupport.styleRibbonSmallButton(button);
		button.setPreferredSize(new Dimension(96, 48));
		button.setSize(button.getPreferredSize());
		button.getModel().setSelected(true);

		BufferedImage canvas = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
		var graphics = canvas.createGraphics();
		try {
			button.paint(graphics);
		} finally {
			graphics.dispose();
		}

		assertTrue(hasDarkIconPixel(canvas));
	}

	@Test
	void disabledButtonsDoNotInheritHoverOrSelectedEmphasis() {
		JToggleButton button = new JToggleButton("Disabled");
		FlatUiSupport.styleRibbonLargeButton(button);
		button.getModel().setRollover(true);
		button.getModel().setSelected(true);
		button.setEnabled(false);

		assertNull(FlatUiSupport.resolveCommandButtonBackground(button));
		assertNotNull(FlatUiSupport.resolveCommandButtonBorderColor(button));
		assertNotEquals(
			FlatUiSupport.commandButtonSelectedBorderColor(button),
			FlatUiSupport.resolveCommandButtonBorderColor(button));
	}

	@Test
	void ribbonTabsKeepHoverAndSelectedStatesDistinct() {
		JToggleButton button = new JToggleButton("Task");
		FlatUiSupport.styleRibbonTabButton(button);

		button.getModel().setRollover(true);
		assertEquals(FlatUiSupport.ribbonTabHoverColor(), FlatUiSupport.resolveRibbonTabBackground(button));
		assertTrue(button.isContentAreaFilled());
		assertEquals(FlatUiSupport.ribbonTabHoverColor(), button.getBackground());
		assertNull(FlatUiSupport.resolveRibbonTabBorderColor(button));

		button.getModel().setSelected(true);
		assertNull(FlatUiSupport.resolveRibbonTabBackground(button));
		assertNotNull(FlatUiSupport.resolveRibbonTabUnderlineColor(button));
	}

	@Test
	void hoveredRibbonTabPaintsItsLabelAboveTheHoverFill() {
		JToggleButton button = new JToggleButton("Project");
		FlatUiSupport.styleRibbonTabButton(button);
		button.setSize(new Dimension(100, FlatUiSupport.ribbonTabHeight()));
		button.getModel().setRollover(true);

		BufferedImage canvas = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
		var graphics = canvas.createGraphics();
		try {
			button.paint(graphics);
		} finally {
			graphics.dispose();
		}

		assertTrue(hasDarkTextPixel(canvas), "the rollover fill must be painted before the tab label");
	}

	private static boolean hasDarkIconPixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xff;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				if (alpha > 200 && green > red + 25 && green > blue + 10 && green < 180) return true;
			}
		}
		return false;
	}

	private static boolean hasDarkTextPixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xff;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				if (alpha > 200 && red < 80 && green < 80 && blue < 80) return true;
			}
		}
		return false;
	}
}
