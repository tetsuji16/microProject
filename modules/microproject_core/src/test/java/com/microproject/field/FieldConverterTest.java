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
	void convertsUsingTypedRuntimeClassMetadata() throws FieldParseException {
		assertEquals(42, FieldConverter.convert("42", Integer.class, null));
		assertEquals(42, FieldConverter.fromString("42", Integer.class));
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
}

