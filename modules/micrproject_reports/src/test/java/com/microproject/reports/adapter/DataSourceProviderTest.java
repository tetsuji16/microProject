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
package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.Money;
import com.microproject.datatype.Rate;
import com.microproject.field.Field;

class DataSourceProviderTest {
	@Test
	void reportValueClassMatchesConvertedRateMoneyAndDurationValues() {
		Field rate = fieldWithType(Rate.class);
		rate.setRate(true);
		Field money = fieldWithType(Money.class);
		money.setMoney(true);
		Field duration = fieldWithType(Duration.class);
		duration.setDuration(true);

		assertEquals(Double.class, DataSourceProvider.reportValueClass(rate));
		assertEquals(Double.class, DataSourceProvider.reportValueClass(money));
		assertEquals(Long.class, DataSourceProvider.reportValueClass(duration));
	}

	@Test
	void reportValueClassKeepsOrdinaryDisplayType() {
		assertEquals(String.class, DataSourceProvider.reportValueClass(fieldWithType(String.class)));
	}

	private static Field fieldWithType(Class<?> type) {
		Field field = new Field();
		field.setExternalType(type);
		field.setClass(ValueHolder.class);
		field.setProperty("value");
		field.setId("test.value");
		field.build();
		return field;
	}

	public static final class ValueHolder {
		public String getValue() {
			return "value";
		}

		public void setValue(String value) {
		}
	}
}
