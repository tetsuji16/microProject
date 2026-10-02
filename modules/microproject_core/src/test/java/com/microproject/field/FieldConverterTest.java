/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.Money;
import com.microproject.datatype.Work;
import com.microproject.datatype.TimeUnit;
import com.microproject.options.EditOption;

class FieldConverterTest {
	@Test
	void optionFieldPreprocessesStringInputBeforeStoringIt() throws Exception {
		Object target = new Object();
		OptionField field = new OptionField();

		field.setValue(target, this, "raw", null);

		assertSame(target, field.target);
		assertEquals("raw", field.preprocessedText);
		assertEquals("normalized:raw", field.storedValue);
	}

	@Test
	void convertsUsingTypedRuntimeClassMetadata() throws FieldParseException {
		assertEquals(42, FieldConverter.convert("42", Integer.class, null));
		assertEquals(42, FieldConverter.fromString("42", Integer.class));
		assertTrue((Boolean) FieldConverter.convert("true", Boolean.class, null));
		assertTrue((Boolean) FieldConverter.convert(" YES ", Boolean.class, null));
		assertEquals(false, FieldConverter.convert("invalid", Boolean.class, null));
		assertEquals(false, FieldConverter.convert(null, Boolean.class, null));
		assertEquals(0, FieldConverter.convert(null, Integer.class, null));
		assertEquals(0.0D, FieldConverter.convert(null, Double.class, null));
		assertEquals(42L, FieldConverter.convert(" 42 ", Long.class, null));
		assertEquals(42L, FieldConverter.convert(42, Long.class, null));
		assertEquals(12.5D, FieldConverter.convert("12.5", Double.TYPE, null));
		assertEquals(TestMode.ACTIVE, FieldConverter.convert("ACTIVE", TestMode.class, null));
		assertThrows(FieldParseException.class, () -> FieldConverter.convert("not-a-number", Integer.class, null));
	}

	private enum TestMode { ACTIVE }

	@Test
	void returnsTheResultFromAContextSpecificConverter() throws FieldParseException {
		assertEquals("value", FieldConverter.convert("value", String.class,
			FieldConverter.COMPACT_CONVERTER_CONTEXT));
	}

	@Test
	void convertsAFormattedStringIntoCalendarUsingTheTypedConverterPath() throws Exception {
		Date source = new Date(1_700_000_000_000L);
		var dateFormat = EditOption.getInstance().getDateFormat();
		String text = dateFormat.format(source);
		Date expected = dateFormat.parse(text);

		Calendar converted = (Calendar) FieldConverter.convert(text, GregorianCalendar.class, null);

		assertEquals(expected, converted.getTime());
	}

	@Test
	void parsesDateWorkAndMoneyStringsThroughTheirRegisteredConverters() throws Exception {
		Date sourceDate = new Date(1_700_000_000_000L);
		var dateFormat = EditOption.getInstance().getDateFormat();
		String dateText = dateFormat.format(sourceDate);
		assertEquals(dateFormat.parse(dateText), FieldConverter.convert(dateText, Date.class, null));

		Work sourceWork = new Work(Duration.getInstance(3.0D, TimeUnit.HOURS));
		String workText = DurationFormat.getWorkInstance().format(sourceWork);
		Duration convertedWork = (Duration) FieldConverter.convert(workText, Work.class, null);
		assertEquals(sourceWork.getEncodedMillis(), convertedWork.getEncodedMillis());

		Money sourceMoney = Money.getInstance(123.45D);
		String moneyText = Money.getFormat(false).format(sourceMoney);
		Number convertedMoney = (Number) FieldConverter.convert(moneyText, Money.class, null);
		assertEquals(Money.getFormat(false).parse(moneyText), convertedMoney);
	}

	@Test
	void formatsAndConvertsOpenProjTimeValuesWithoutChangingTheirValues() throws Exception {
		Work work = new Work(Duration.getInstance(3.0D, TimeUnit.HOURS));
		String expectedWorkText = DurationFormat.getWorkInstance().format(work);
		assertEquals(expectedWorkText, FieldConverter.toString(work, Work.class, null));
		Money money = Money.getInstance(123.45D);
		assertEquals(Money.formatCurrency(money.doubleValue(), false),
			FieldConverter.toString(money, Money.class, null));

		Date date = new Date(1_700_000_000_000L);
		assertEquals(EditOption.getInstance().getDateFormat().format(date),
			FieldConverter.toString(date, Date.class, null));
		assertEquals(date.getTime(), FieldConverter.convert(date, Long.class, null));
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		assertEquals(date.getTime(), FieldConverter.convert(calendar, Long.class, null));
		assertEquals(work.getEncodedMillis(), FieldConverter.convert(work, Long.class, null));
		assertSame(date, FieldConverter.convert(date, Date.class, null));
		assertSame(money, FieldConverter.convert(money, Money.class, null));
		assertEquals(work.longValue(),
			((Duration) FieldConverter.convert(work, Duration.class, null)).getEncodedMillis());
		assertNull(FieldConverter.convert(0L, Date.class, null));
		assertNull(FieldConverter.convert(0L, GregorianCalendar.class, null));
	}

	private static final class OptionField extends Field {
		private Object target;
		private Object storedValue;
		private String preprocessedText;

		@Override
		public boolean hasOptions() {
			return true;
		}

		@Override
		protected Object preprocessText(Object object, String textValue, FieldContext context) {
			this.preprocessedText = textValue;
			return "normalized:" + textValue;
		}

		@Override
		public boolean setInternalValue(Object object, Object value, FieldContext context) {
			this.target = object;
			this.storedValue = value;
			return true;
		}
	}
}

