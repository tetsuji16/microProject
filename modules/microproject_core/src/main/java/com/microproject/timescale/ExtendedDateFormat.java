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
package com.microproject.timescale;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.microproject.i18n.Messages;

/**
 * Formats GMT-read milliseconds for the time scale. A pattern may start with
 * the quarter prefix {@code Q} or {@code QQ} or the half-year prefix {@code L}
 * or {@code LL}; the prefix is replaced by the localized label
 * ({@code Date.Quarter1}, {@code Date.Quarter2}, {@code Date.Half1},
 * {@code Date.Half2}) followed by the quarter or half number. The rest of the
 * pattern is a {@link DateTimeFormatter} pattern.
 */
public final class ExtendedDateFormat {
	private static final ZoneId GMT = ZoneId.of("GMT");

	private final DateTimeFormatter formatter;
	private final String text;
	private final int monthDivisor;

	/**
	 * @param pattern the pattern, possibly starting with a quarter or half prefix
	 */
	public ExtendedDateFormat(String pattern) {
		this(pattern, Locale.getDefault());
	}

	/**
	 * @param pattern the pattern, possibly starting with a quarter or half prefix
	 * @param locale the locale of the formatted fields; null uses the default locale
	 */
	public ExtendedDateFormat(String pattern, Locale locale) {
		String remaining = pattern == null ? "" : pattern;
		Locale effectiveLocale = locale == null ? Locale.getDefault() : locale;
		String label = "";
		int divisor = 0;
		if (remaining.startsWith("Q")) {
			int length = remaining.startsWith("QQ") ? 2 : 1;
			label = Messages.getString("Date.Quarter" + length);
			if (length == 2) label += " ";
			remaining = remaining.substring(length);
			divisor = 3;
		} else if (remaining.startsWith("L")) {
			int length = remaining.startsWith("LL") ? 2 : 1;
			label = Messages.getString("Date.Half" + length);
			if (length == 2) label += " ";
			remaining = remaining.substring(length);
			divisor = 6;
		}
		this.text = label;
		this.monthDivisor = divisor;
		this.formatter = remaining.length() == 0 ? null
				: DateTimeFormatter.ofPattern(remaining, effectiveLocale).withZone(GMT);
	}

	/**
	 * Formats a GMT-read millisecond value.
	 *
	 * @param epochMillis the value to format, read in GMT
	 * @return the localized label of the quarter or half, followed by the rest
	 *         of the pattern
	 */
	public String format(long epochMillis) {
		Instant instant = Instant.ofEpochMilli(epochMillis);
		StringBuilder result = new StringBuilder();
		if (monthDivisor != 0) {
			int month = instant.atZone(GMT).getMonthValue() - 1;
			result.append(text).append(month / monthDivisor + 1);
		}
		if (formatter != null) result.append(formatter.format(instant));
		return result.toString();
	}
}
