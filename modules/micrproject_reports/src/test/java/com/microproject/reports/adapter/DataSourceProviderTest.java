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
