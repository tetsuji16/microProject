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
package com.microproject.core.pm.exchange.converters.mpx;

import com.microproject.core.pm.exchange.converters.mpx.type.MpxDurationConverter;
import com.microproject.core.pm.exchange.converters.type.CalendarUTCLongConverter;
import com.microproject.core.time.DefaultTimephasedValue;
import com.microproject.core.time.Duration;
import com.microproject.core.time.TimeUnit;
import com.microproject.core.time.TimephasedType;
import com.microproject.core.time.TimephasedValue;

import net.sf.mpxj.mspdi.DatatypeConverter;

/**
 * @author Laurent Chretienneau
 *
 */
public class MpxTimephasedConverter {
	public TimephasedValue<?> from(net.sf.mpxj.mspdi.schema.TimephasedDataType mpxTimephased, MpxImportState state) {
		TimephasedType type=MpxUtils.safeGetTimephasedType(mpxTimephased.getType());
		if (type == null || !type.isWork())
			return null;
		CalendarUTCLongConverter dateConverter=new CalendarUTCLongConverter();
		long start=(Long)dateConverter.from(mpxTimephased.getStart());
		long finish=(Long)dateConverter.from(mpxTimephased.getFinish());
		
		Duration value;
		String rawValue=mpxTimephased.getValue();
		if (rawValue==null ||
				rawValue.length()==0 ||
				"0".equals(rawValue) ||
				"PT0H0M0S".equals(rawValue))
			value=new Duration(0.0D, TimeUnit.HOURS);
		else if ("PT8H0M0S".equals(rawValue))
			// keep the same scale as the MpxDurationConverter path below (value in
			// natural units, not millis) - 8 hours, not 8*3600000 "hours"
			value=new Duration(8.0D, TimeUnit.HOURS);
		else{
			MpxDurationConverter durationConverter=new MpxDurationConverter();
			net.sf.mpxj.Duration mpxDuration=DatatypeConverter.parseDuration(state.getMpxProjectFile(),null,rawValue);
			value=(Duration)durationConverter.from(mpxDuration);
		}
		TimephasedValue<Duration> timephased=new DefaultTimephasedValue<Duration>(start,finish,value,type);
		return timephased;
	}

	
}
