/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JLabel;
import javax.swing.JButton;

import org.junit.jupiter.api.Test;

import com.microproject.util.FlatUiSupport;

class RibbonButtonStylerTest {
	@Test
	void largeButtonsAllowEnoughHeightForIconAndTwoLineText() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("名前を付けて保存");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.saveAs");

		styler.styleActionButton(button, true);

		assertNotNull(button.getIcon());
		JLabel probe = new JLabel(button.getText());
		probe.setFont(button.getFont());
		assertTrue(button.getPreferredSize().height >= button.getIcon().getIconHeight() + probe.getPreferredSize().height + com.microproject.util.FlatUiSupport.ribbonButtonVerticalInset());
	}

	@Test
	void inlineButtonsRemainTallerThanTheirIcons() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("プレビュー");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.printPreview");

		styler.styleActionButton(button, "medium");

		assertNotNull(button.getIcon());
		assertTrue(button.getPreferredSize().height >= button.getIcon().getIconHeight());
		assertTrue(button.getPreferredSize().width > button.getIcon().getIconWidth());
	}

	@Test
	void styledRibbonButtonsExposeStableStateIcons() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("保存");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.save");

		styler.styleActionButton(button, "medium");

		assertNotNull(button.getIcon());
		assertSame(button.getIcon(), button.getRolloverIcon());
		assertNotSame(button.getIcon(), button.getDisabledIcon());
	}

	@Test
	void longLabelsWrapIntoTwoLines() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("名前を付けて保存するには別のファイル名を指定します");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.saveAs");

		styler.styleActionButton(button, true);

		assertTrue(button.getText().startsWith("<html>"));
		assertTrue(button.getText().contains("<br>"));
	}

	@Test
	void largeButtonWrappingKeepsWordsAfterTheFirstLineOverflowInOrder() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("9/29/26 Status Date:");

		styler.styleActionButton(button, "large");

		assertEquals("9/29/26 Status Date:", button.getText()
			.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", ""));
	}

	@Test
	void largeButtonsUseTheCompressedThemeHeight() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("名前を付けて保存");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.saveAs");

		styler.styleActionButton(button, true);

		assertEquals(com.microproject.util.FlatUiSupport.ribbonLargeButtonHeight(), button.getPreferredSize().height);
	}

	@Test
	void inlineButtonsUseTheCompressedInlineHeightWithoutCrushingIcons() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("プレビュー");
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "ribbon.printPreview");

		styler.styleActionButton(button, "medium");

		// The inline height is a floor: text/icon content can legitimately make the
		// button taller than the configured theme height (e.g. larger font metrics on
		// some JDKs), but it must never drop below it or crush the icon.
		assertTrue(button.getPreferredSize().height >= com.microproject.util.FlatUiSupport.ribbonInlineButtonHeight());
		assertTrue(button.getPreferredSize().height >= button.getIcon().getIconHeight());
	}

	@Test
	void ribbonButtonsUseSharedThemeFontHierarchy() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton large = new JButton("保存");
		JButton medium = new JButton("プレビュー");

		styler.styleActionButton(large, true);
		styler.styleActionButton(medium, "medium");

		assertEquals(FlatUiSupport.ribbonFont(), large.getFont());
		assertEquals(FlatUiSupport.ribbonFont(), medium.getFont());
		assertEquals(FlatUiSupport.ribbonFont(), FlatUiSupport.ribbonTabFont());
		assertEquals(FlatUiSupport.ribbonFont(), FlatUiSupport.ribbonBandTitleFont());
		assertEquals(FlatUiSupport.ribbonFont(), FlatUiSupport.ribbonChromeLabelFont());
	}

	@Test
	void splitButtonsReserveTheChevronHitAreaWithoutClippingTheLabel() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton regular = new JButton("フィルター");
		JButton split = new JButton("フィルター");
		split.putClientProperty("MicroProject.ribbonSplit", Boolean.TRUE);

		styler.styleActionButton(regular, "large");
		styler.styleActionButton(split, "large");

		assertEquals(18, split.getPreferredSize().width - regular.getPreferredSize().width);
	}

	@Test
	void longJapaneseLargeLabelsKeepReadableWidthWithoutEllipsis() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton button = new JButton("ベースラインのクリア");

		styler.styleActionButton(button, true);

		assertTrue(button.getText().equals("ベースラインのクリア") || button.getText().startsWith("<html>"));
		assertTrue(button.getPreferredSize().width >= FlatUiSupport.ribbonLargeButtonMinWidth());
		assertTrue(!button.getText().contains("..."));
		assertTrue(button.getPreferredSize().width >= 92);
	}

	@Test
	void checkBoxControlsPaintASquareThatShowsTheCheckMarkOnlyWhenSelected() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		javax.swing.JToggleButton button = new javax.swing.JToggleButton("Timeline");
		button.putClientProperty(RibbonControlPopups.CONTROL_KIND_PROPERTY, com.microproject.ribbon.RibbonControlKind.CHECK_BOX);
		button.putClientProperty(RibbonButtonStyler.ICON_KEY_PROPERTY, "view.timeline");

		styler.styleActionButton(button, "small");

		assertTrue(button.getIcon() instanceof RibbonCheckBoxIcon);
		assertEquals(button.getIcon().getIconWidth(), button.getIcon().getIconHeight());
		assertEquals(16, button.getIcon().getIconHeight());
		assertNotEquals(paint(button, false), paint(button, true));
		assertEquals(paint(button, true), paint(button, true));
	}

	private static java.util.List<Integer> paint(javax.swing.JToggleButton button, boolean selected) {
		button.setSelected(selected);
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		button.getIcon().paintIcon(button, graphics, 0, 0);
		graphics.dispose();
		java.util.List<Integer> pixels = new java.util.ArrayList<>();
		for (int y = 0; y < 16; y++)
			for (int x = 0; x < 16; x++)
				pixels.add(image.getRGB(x, y));
		return pixels;
	}
}
