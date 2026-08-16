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

import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

import com.microproject.util.ClassUtils;

/**
 * Adds ability for percentage formatter to parse non percents
 */
public class PercentFormat extends Format {
	private static NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
	private static NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();	
	public static double NULL_VALUE = -987654.321; // a never used value used as flag to indicate multiple values	
	private static Format percentFormatterInstance = null;
	public static Format getInstance() {
		if (percentFormatterInstance == null)
			percentFormatterInstance =	new PercentFormat();
		return percentFormatterInstance;
	}
	public Object parseObject(String arg0, ParsePosition arg1) {
		Number result = PERCENT_FORMAT.parse(arg0,arg1);
		if (result == null) {
			result = NUMBER_FORMAT.parse(arg0,arg1);
			if (result != null)
				result = Double.valueOf(result.doubleValue() / 100.0D);
		}
		return result;
	}
	public StringBuffer format(Object arg0, StringBuffer arg1, FieldPosition arg2) {
		if (ClassUtils.isMultipleValue(arg0)) {
			arg1.append(com.microproject.field.Field.MULTIPLE_VALUES);
			return arg1;
		} else if (ClassUtils.isDefaultValue(arg0)) {
			return arg1; // empty
		}
		Object value;
		if (arg0 instanceof Rate)
			value = Double.valueOf(((Rate)arg0).getValue());
		else
			value = arg0;
		PERCENT_FORMAT.format(value,arg1,arg2);
		return arg1;
	}
	
	public String format(double value) {
		return format(Double.valueOf(value));
	}
	public static boolean isSpecialValue(Double v) {
		return v.equals(ClassUtils.PERCENT_MULTIPLE_VALUES) || v.doubleValue() == NULL_VALUE;
	}

}
