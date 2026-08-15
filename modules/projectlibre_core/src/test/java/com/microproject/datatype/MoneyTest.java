package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.text.NumberFormat;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoneyTest {
	private Locale originalLocale;

	@BeforeEach
	void useStableCurrencyLocale() {
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(originalLocale);
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
}
