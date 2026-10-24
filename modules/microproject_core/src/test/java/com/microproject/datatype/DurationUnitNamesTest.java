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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

/**
 * Issue #893: the Japanese UI displays Japanese duration units (MSP Japanese shows
 * durations such as "3 日" and "4 時間") while still accepting the English
 * abbreviations, so existing habits and documented examples (3d, 24h, 9ed) keep working.
 */
class DurationUnitNamesTest {
	private static final String BUNDLE = "com.microproject.strings.client";
	private static final int LONG_STYLE = 2; // EditOption's default view style
	private static final int COMPACT_STYLE = 3; // DurationFormat.formatCompact

	@Test
	void japaneseDisplaysJapaneseUnitNames() {
		DurationFormat.UnitNames ja = names(Locale.JAPANESE);
		String[][] expected = {
			{"分", "minute"}, {"時間", "hour"}, {"日", "day"}, {"週", "week"},
			{"か月", "month"}, {"年", "year"}};
		for (int type = TimeUnit.MINUTES; type <= TimeUnit.YEARS; type++) {
			for (int style : new int[] {LONG_STYLE, COMPACT_STYLE}) {
				assertEquals(expected[type][0], ja.display(style, true, type), expected[type][1]);
				assertEquals(expected[type][0], ja.display(style, false, type), expected[type][1]);
				assertEquals("経過" + expected[type][0],
					ja.display(style, false, type + TimeUnit.ELAPSED_MINUTES), "elapsed " + expected[type][1]);
			}
		}
		assertEquals("%", ja.display(LONG_STYLE, false, TimeUnit.PERCENT));
		assertEquals("?", ja.estimatedSymbol(), "the estimated marker stays ?");
	}

	@Test
	void japaneseAcceptsJapaneseAndEnglishUnitNames() {
		DurationFormat.UnitNames ja = names(Locale.JAPANESE);
		assertUnit(ja, "日", TimeUnit.DAYS, false);
		assertUnit(ja, "d", TimeUnit.DAYS, false);
		assertUnit(ja, "days", TimeUnit.DAYS, false);
		assertUnit(ja, "day", TimeUnit.DAYS, false);
		assertUnit(ja, "時間", TimeUnit.HOURS, false);
		assertUnit(ja, "h", TimeUnit.HOURS, false);
		assertUnit(ja, "分", TimeUnit.MINUTES, false);
		assertUnit(ja, "m", TimeUnit.MINUTES, false);
		assertUnit(ja, "か月", TimeUnit.MONTHS, false);
		assertUnit(ja, "月", TimeUnit.MONTHS, false);
		assertUnit(ja, "mo", TimeUnit.MONTHS, false);
		assertUnit(ja, "経過日", TimeUnit.ELAPSED_DAYS, false);
		assertUnit(ja, "ed", TimeUnit.ELAPSED_DAYS, false);
		assertUnit(ja, "日?", TimeUnit.DAYS, true);
		assertUnit(ja, "日 ?", TimeUnit.DAYS, true);
		assertUnit(ja, "?", TimeUnit.NONE, true);
		assertUnit(ja, "", TimeUnit.NONE, false);
		assertUnit(ja, "%", TimeUnit.PERCENT, false);
		assertNull(ja.match("日日"), "an unknown suffix is not a duration");
		assertNull(ja.match("days!"), "an unknown suffix is not a duration");
	}

	@Test
	void englishNamesAreUnchanged() {
		DurationFormat.UnitNames en = names(Locale.ROOT);
		assertEquals("day", en.display(LONG_STYLE, true, TimeUnit.DAYS));
		assertEquals("days", en.display(LONG_STYLE, false, TimeUnit.DAYS));
		assertEquals("hours", en.display(LONG_STYLE, false, TimeUnit.HOURS));
		assertEquals("edays", en.display(LONG_STYLE, false, TimeUnit.ELAPSED_DAYS));
		assertEquals("M", en.display(COMPACT_STYLE, false, TimeUnit.MONTHS));
		assertUnit(en, "days", TimeUnit.DAYS, false);
		assertUnit(en, "dys", TimeUnit.DAYS, false);
		assertUnit(en, "m", TimeUnit.MINUTES, false);
		assertUnit(en, "M", TimeUnit.MONTHS, false);
		assertUnit(en, "9ed".substring(1), TimeUnit.ELAPSED_DAYS, false);
		assertNull(en.match("日"), "English does not accept Japanese unit names");
	}

	@Test
	void everyDisplayedNameParsesBackToItsOwnUnit() {
		for (Locale locale : new Locale[] {Locale.ROOT, Locale.JAPANESE}) {
			DurationFormat.UnitNames names = names(locale);
			for (int type = TimeUnit.MINUTES; type <= TimeUnit.ELAPSED_PERCENT; type++) {
				if (type == TimeUnit.PERCENT || type == TimeUnit.ELAPSED_PERCENT)
					continue; // percent symbols are shared by both styles
				for (int style = 0; style < DurationFormat.UnitNames.DISPLAY_STYLE_COUNT; style++) {
					for (boolean singular : new boolean[] {true, false}) {
						String shown = names.display(style, singular, type);
						assertUnit(names, shown, type, false);
					}
				}
			}
		}
	}

	private static void assertUnit(DurationFormat.UnitNames names, String suffix, int unit, boolean estimated) {
		DurationFormat.UnitMatch match = names.match(suffix);
		assertNotNull(match, () -> "'" + suffix + "' must be a duration unit");
		assertEquals(unit, match.timeUnit(), () -> "unit of '" + suffix + "'");
		if (estimated)
			assertTrue(match.estimated(), () -> "'" + suffix + "' is estimated");
		else
			assertFalse(match.estimated(), () -> "'" + suffix + "' is not estimated");
	}

	private static DurationFormat.UnitNames names(Locale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale,
			ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
		assertEquals(locale, bundle.getLocale(), "the bundle for " + locale + " must exist");
		return new DurationFormat.UnitNames(bundle::getString);
	}
}
