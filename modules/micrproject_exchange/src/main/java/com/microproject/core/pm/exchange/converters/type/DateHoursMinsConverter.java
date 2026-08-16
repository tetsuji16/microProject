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
package com.microproject.core.pm.exchange.converters.type;

import java.util.Date;

import com.microproject.core.fields.FieldTypeConverter;
import com.microproject.core.time.TimeUtil;


/**
 * @author Laurent Chretienneau
 *
 */
public class DateHoursMinsConverter extends FieldTypeConverter {

	@Override
	public Object from(Object o) {
		if (o==null) return -1L;
		Date d=(Date)o;
		return TimeUtil.toHoursAndMinutes(d.getTime());
	}

	@Override
	public Object to(Object o) {
		if (o==null) return null;
		if (!(o instanceof Long)) {
			throw new IllegalArgumentException("Expected Long value but got " + o.getClass().getName());
		}
		long l=(Long)o;
		if (l==-1L) return null;
		return new Date(l);
	}

}
