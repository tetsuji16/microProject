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

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

class FlatLafSupportTest {
	@Test
	void initializeUsesRibbonChromeForWindowAndMenuBar() {
		FlatLafSupport.initialize();

		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("TitlePane.background"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("TitlePane.inactiveBackground"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("MenuBar.background"));
		assertEquals(FlatUiTheme.RIBBON_CHROME_BACKGROUND, UIManager.getColor("Menu.background"));
	}

	@Test
	void titlePaneWindowButtonsUseOfficeHoverStates() {
		FlatLafSupport.initialize();

		assertEquals(new java.awt.Color(0xE5F1FB), UIManager.getColor("TitlePane.buttonHoverBackground"));
		assertEquals(new java.awt.Color(0xCCE4F7), UIManager.getColor("TitlePane.buttonPressedBackground"));
		assertEquals(UIManager.getColor("TitlePane.foreground"), UIManager.getColor("TitlePane.buttonHoverForeground"));
		assertEquals(UIManager.getColor("TitlePane.foreground"), UIManager.getColor("TitlePane.buttonPressedForeground"));
	}

	@Test
	void dialogComponentStylingCoversLegacySwingTree() {
		FlatLafSupport.initialize();
		JPanel content = new JPanel(new BorderLayout());
		JButton button = new JButton("Close");
		JTable table = new JTable(2, 2);
		content.add(button, BorderLayout.SOUTH);
		content.add(new JScrollPane(table), BorderLayout.CENTER);

		FlatUiSupport.styleDialogComponents(content);

		assertEquals(FlatUiSupport.dialogSurfaceBackground(), button.getBackground());
		assertEquals(FlatUiSupport.spreadsheetBodyBackground(), table.getBackground());
		assertEquals(FlatUiSupport.spreadsheetGridColor(), table.getGridColor());
		assertEquals(FlatUiSupport.dialogButtonHeight(), button.getMinimumSize().height);
		assertEquals(FlatUiSupport.viewportBackground(), table.getParent().getBackground());
	}
}
