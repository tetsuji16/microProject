package com.projectlibre.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

		assertEquals(FlatUiSupport.ribbonButtonFont().getSize2D(), large.getFont().getSize2D());
		assertEquals(FlatUiSupport.ribbonButtonFont().getSize2D(), medium.getFont().getSize2D());
	}

	@Test
	void splitButtonsReserveTheChevronHitAreaWithoutClippingTheLabel() {
		RibbonButtonStyler styler = new RibbonButtonStyler();
		JButton regular = new JButton("フィルター");
		JButton split = new JButton("フィルター");
		split.putClientProperty("ProjectLibre.ribbonSplit", Boolean.TRUE);

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
}
