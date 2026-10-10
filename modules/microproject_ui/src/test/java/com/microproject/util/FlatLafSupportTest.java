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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import com.formdev.flatlaf.FlatLaf;
import com.microproject.preference.GlobalPreferences;
import com.microproject.ui.theme.MicroProjectThemeTokens;

class FlatLafSupportTest {
	@Test
	void initializeUsesNeutralOfficeTitleBarAndProjectGreenRibbonAccent() {
		FlatLafSupport.initialize();

		assertEquals(FlatLafSupport.isNativeWindowDecorationsEnabled(),
			javax.swing.JFrame.isDefaultLookAndFeelDecorated(),
			"unsupported platforms must retain OS decorations instead of a Java-painted title pane");
		assertEquals(FlatLafSupport.isNativeWindowDecorationsEnabled(),
			javax.swing.JDialog.isDefaultLookAndFeelDecorated());

		boolean dark = new GlobalPreferences().isDarkTheme();
		java.awt.Color titleBar = MicroProjectThemeTokens.dark().ribbonChromeBackground();
		if (!dark) titleBar = MicroProjectThemeTokens.light().ribbonChromeBackground();
		assertEquals(titleBar, UIManager.getColor("TitlePane.background"));
		assertEquals(titleBar, UIManager.getColor("TitlePane.inactiveBackground"));
		assertEquals(new java.awt.Color(dark ? 0x54A878 : 0x0F773D), UIManager.getColor("MicroProject.ribbonAccentColor"));
		assertEquals(FlatUiTheme.ribbonChromeBackground(), UIManager.getColor("MenuBar.background"));
		assertEquals(FlatUiTheme.ribbonChromeBackground(), UIManager.getColor("Menu.background"));
		if (Environment.isWindows() && FlatLaf.supportsNativeWindowDecorations()) {
			assertTrue(FlatLaf.isUseNativeWindowDecorations(),
				"Windows frames must use FlatLaf native decorations when the runtime supports them");
		}
	}

	@Test
	void titlePaneWindowButtonsUseOfficeHoverStates() {
		FlatLafSupport.initialize();

		boolean dark = new GlobalPreferences().isDarkTheme();
		assertEquals(new java.awt.Color(dark ? 0x3A3D42 : 0xE5E5E5), UIManager.getColor("TitlePane.buttonHoverBackground"));
		assertEquals(new java.awt.Color(dark ? 0x464A50 : 0xD6D6D6), UIManager.getColor("TitlePane.buttonPressedBackground"));
		assertEquals(UIManager.getColor("TitlePane.foreground"), UIManager.getColor("TitlePane.buttonHoverForeground"));
		assertEquals(UIManager.getColor("TitlePane.foreground"), UIManager.getColor("TitlePane.buttonPressedForeground"));
	}

	@Test
	void ensureInitializedRestoresFlatLafAfterAnotherLookAndFeelWasInstalled() throws Exception {
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

		FlatLafSupport.ensureInitialized();

		String expected = new GlobalPreferences().isDarkTheme()
			? "com.formdev.flatlaf.FlatDarkLaf" : "com.formdev.flatlaf.FlatLightLaf";
		assertEquals(expected, UIManager.getLookAndFeel().getClass().getName());
	}

	@Test
	void initializeUsesSavedDarkAppearanceAndSemanticPalette() {
		GlobalPreferences preferences = new GlobalPreferences();
		boolean original = preferences.isDarkTheme();
		try {
			preferences.setDarkTheme(true);
			FlatLafSupport.initialize();
			assertEquals("com.formdev.flatlaf.FlatDarkLaf", UIManager.getLookAndFeel().getClass().getName());
			assertEquals(MicroProjectThemeTokens.dark().workspaceBackground(), UIManager.getColor("MicroProject.workspaceBackground"));
			assertEquals(MicroProjectThemeTokens.dark().tableForeground(), FlatUiSupport.tableForeground());
		} finally {
			preferences.setDarkTheme(original);
			FlatLafSupport.initialize();
		}
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

	@Test
	void dialogComponentStylingUsesThemeFontAndSurfacesForStandardControls() {
		FlatLafSupport.initialize();
		JPanel content = new JPanel();
		JComboBox<String> comboBox = new JComboBox<>(new String[] { "A" });
		JSpinner spinner = new JSpinner();
		JList<String> list = new JList<>(new String[] { "A" });
		JTextField textField = new JTextField();
		content.add(comboBox);
		content.add(spinner);
		content.add(list);
		content.add(textField);

		FlatUiSupport.styleDialogComponents(content);

		assertEquals(FlatUiSupport.uiFont(), comboBox.getFont());
		assertEquals(FlatUiSupport.uiFont(), spinner.getFont());
		assertEquals(FlatUiSupport.uiFont(), list.getFont());
		assertEquals(FlatUiSupport.uiFont(), textField.getFont());
		assertEquals(FlatUiSupport.dataSurfaceBackground(), list.getBackground());
	}

	@Test
	void userFontPreferenceUpdatesSwingDefaultsAndResetUsesPlatformBaseline() {
		FlatLafSupport.initialize();
		java.awt.Font platformFont = UIManager.getFont("defaultFont");
		try {
			FlatLafSupport.applyUserFontPreference("Dialog", 14);
			for (String key : new String[] { "defaultFont", "Label.font", "Button.font", "TextField.font",
					"ComboBox.font", "List.font", "Table.font", "TabbedPane.font" }) {
				java.awt.Font font = UIManager.getFont(key);
				assertEquals("Dialog", font.getFamily(), key + " family");
				assertEquals(14.0f, font.getSize2D(), key + " size");
			}
			assertEquals(java.awt.Font.BOLD, UIManager.getFont("TableHeader.font").getStyle());

			FlatLafSupport.applyUserFontPreference("", 0);
			assertEquals(platformFont, UIManager.getFont("defaultFont"), "reset must restore the platform font baseline");
			assertEquals(platformFont, UIManager.getFont("Label.font"));
		} finally {
			FlatLafSupport.initialize();
		}
	}

	@Test
	void userFontWithoutJapaneseGlyphsFallsBackToPlatformFont() {
		FlatLafSupport.initialize();
		java.awt.Font platformFont = UIManager.getFont("defaultFont");
		String unsupportedFamily = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
				.filter(name -> new Font(name, Font.PLAIN, 12).canDisplayUpTo("日本語") >= 0)
				.findFirst()
				.orElse(null);
		Assumptions.assumeTrue(unsupportedFamily != null,
				"This system has no installed font family that lacks Japanese glyphs");
		try {
			FlatLafSupport.applyUserFontPreference(unsupportedFamily, 14);
			Font applied = UIManager.getFont("defaultFont");
			assertEquals(platformFont.getFamily(), applied.getFamily(), "unsupported CJK font should use platform baseline");
			assertEquals(14.0f, applied.getSize2D(), "requested size should be retained on fallback");
			assertTrue(applied.canDisplayUpTo("日本語") < 0, "applied font should display Japanese");
		} finally {
			FlatLafSupport.applyUserFontPreference("", 0);
		}
	}

}
