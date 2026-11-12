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
package com.microproject.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.IconManager;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.MicrosoftProjectGanttTheme;

class ThemeManagerTest {
	private boolean originalDark;

	@BeforeEach
	void setUp() {
		FlatLafSupport.initialize();
		originalDark = ThemeManager.isDark();
	}

	@AfterEach
	void restore() {
		ThemeManager.applyTheme(originalDark);
	}

	@Test
	void applyThemeTogglesLookAndFeelAndTokens() {
		ThemeManager.applyTheme(true);
		assertTrue(ThemeManager.isDark());
		assertTrue(FlatUiSupport.isDarkTheme());
		assertEquals(MicroProjectThemeTokens.dark().ribbonChromeBackground(), MicroProjectTheme.tokens().ribbonChromeBackground());
		Color darkChrome = FlatUiSupport.officeTitleBarBackground();
		Color darkGantt = MicrosoftProjectGanttTheme.background();

		ThemeManager.applyTheme(false);
		assertFalse(ThemeManager.isDark());
		assertEquals(MicroProjectThemeTokens.light().ribbonChromeBackground(), MicroProjectTheme.tokens().ribbonChromeBackground());
		assertNotEquals(darkChrome, FlatUiSupport.officeTitleBarBackground());
		assertNotEquals(darkGantt, MicrosoftProjectGanttTheme.background());
		assertNotEquals(darkChrome, UIManager.getColor("TitlePane.background"));
	}

	@Test
	void roundTripRestoresOriginalColors() {
		ThemeManager.applyTheme(false);
		Color panel = UIManager.getColor("Panel.background");
		Color accent = UIManager.getColor("MicroProject.ribbonAccentColor");
		Color gantt = MicrosoftProjectGanttTheme.gridLine();

		ThemeManager.applyTheme(true);
		assertNotEquals(panel, UIManager.getColor("Panel.background"));
		ThemeManager.applyTheme(false);

		assertEquals(panel, UIManager.getColor("Panel.background"));
		assertEquals(accent, UIManager.getColor("MicroProject.ribbonAccentColor"));
		assertEquals(gantt, MicrosoftProjectGanttTheme.gridLine());
	}

	@Test
	void listenersFireOncePerSwitchUntilRemoved() {
		AtomicInteger calls = new AtomicInteger();
		Runnable listener = calls::incrementAndGet;
		ThemeManager.addThemeListener(listener);
		ThemeManager.applyTheme(true);
		ThemeManager.applyTheme(false);
		assertEquals(2, calls.get());
		ThemeManager.removeThemeListener(listener);
		ThemeManager.applyTheme(true);
		assertEquals(2, calls.get());
	}

	@Test
	void failingListenerDoesNotStopTheSwitch() {
		Runnable failing = () -> { throw new IllegalStateException("boom"); };
		AtomicInteger calls = new AtomicInteger();
		Runnable after = calls::incrementAndGet;
		ThemeManager.addThemeListener(failing);
		ThemeManager.addThemeListener(after);
		try {
			ThemeManager.applyTheme(true);
			assertEquals(1, calls.get());
		} finally {
			ThemeManager.removeThemeListener(failing);
			ThemeManager.removeThemeListener(after);
		}
	}

	@Test
	void applyThemeClearsIconCaches() {
		ImageIcon before = IconManager.getIcon("application.icon.small");
		assertTrue(before != null && IconManager.cachedIconCount() > 0, "precondition: icon cached");
		ThemeManager.applyTheme(!ThemeManager.isDark());
		assertEquals(0, IconManager.cachedIconCount());
		assertNotSame(before, IconManager.getIcon("application.icon.small"));
	}
}
