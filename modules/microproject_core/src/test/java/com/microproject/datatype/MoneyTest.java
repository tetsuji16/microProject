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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.DateTime;
import com.microproject.preference.GlobalPreferences;

class MoneyTest {
	private Locale originalLocale;
	private String originalCurrencyCode;

	@BeforeEach
	void useStableCurrencyLocale() {
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
		originalCurrencyCode = Money.getPreferredCurrencyCode();
		Money.setPreferredCurrencyCode("");
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(originalLocale);
		Money.setPreferredCurrencyCode(originalCurrencyCode);
	}

	@Test
	void selectsFullAndCompactFormatsByFlag() {
		assertEquals(2, Money.getFormat(false).getMaximumFractionDigits());
		assertEquals(0, Money.getFormat(true).getMaximumFractionDigits());
	}

	@Test
	void returnsIndependentFormatters() {
		NumberFormat first = Money.getMoneyFormatInstance();
		first.setMaximumFractionDigits(0);

		NumberFormat second = Money.getMoneyFormatInstance();

		assertNotSame(first, second);
		assertEquals(2, second.getMaximumFractionDigits());
	}

	@Test
	void constructsDecimalFromCanonicalDoubleText() {
		assertEquals("0.1", Money.getInstance(0.1).toPlainString());
	}

	@Test
	void dateAndCurrencyFormatsFollowTheActiveLocale() {
		Locale.setDefault(Locale.US);
		String usCurrency = Money.normalCurrencyFormat(1234.5, false);
		String usDate = DateTime.utcDateFormatInstance().format(new Date(1767312000000L));

		Locale.setDefault(Locale.GERMANY);
		String germanCurrency = Money.normalCurrencyFormat(1234.5, false);
		String germanDate = DateTime.utcDateFormatInstance().format(new Date(1767312000000L));

		assertNotEquals(usCurrency, germanCurrency);
		assertEquals("$1234.50", usCurrency);
		assertEquals("1234,50 €", germanCurrency);
		assertNotEquals(usDate, germanDate);
	}

	@Test
	void currencyPreferenceOverridesTheLocaleCurrency() {
		GlobalPreferences preferences = new GlobalPreferences();
		String originalCurrencyCode = preferences.getCurrencyCode();
		try {
			preferences.setCurrencyCode("EUR");
			preferences.applyFormatPreferences();
			assertEquals("EUR", Money.getMoneyFormatInstance().getCurrency().getCurrencyCode());
			preferences.setCurrencyCode("JPY");
			preferences.applyFormatPreferences();
			assertEquals(0, Money.getMoneyFormatInstance().getMaximumFractionDigits());
		} finally {
			preferences.setCurrencyCode(originalCurrencyCode);
			preferences.applyFormatPreferences();
		}
	}
}
