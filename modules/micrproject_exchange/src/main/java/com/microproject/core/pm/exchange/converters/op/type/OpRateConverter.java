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
package com.microproject.core.pm.exchange.converters.op.type;

import com.microproject.core.fields.FieldTypeConverter;
import com.microproject.core.time.Rate;
import com.microproject.core.time.TimeUnit;


/**
 * @author Laurent Chretienneau
 *
 */
public class OpRateConverter extends FieldTypeConverter {

	@Override
	public Object from(Object o) {
		com.microproject.datatype.Rate r=(com.microproject.datatype.Rate)o;
		return new Rate(r.getValue() * com.microproject.datatype.Duration.timeUnitFactor(r.getTimeUnit()),TimeUnit.getInstance(r.getTimeUnit()));
	}

	@Override
	public Object to(Object o) {
		Rate r=(Rate)o;
		return new com.microproject.datatype.Rate(r.getValue() / com.microproject.datatype.Duration.timeUnitFactor(r.getUnit().getId()), r.getUnit().getId());
	}
	
}
