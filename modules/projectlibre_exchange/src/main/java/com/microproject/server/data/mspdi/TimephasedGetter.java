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

package com.projectlibre1.server.data.mspdi;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.mspdi.schema.ObjectFactory;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;

import org.apache.commons.collections.Closure;

import com.projectlibre1.pm.assignment.functor.AssignmentFieldFunctor;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.util.DateTime;

/**
 *
 */
public class TimephasedGetter implements Closure {
	ObjectFactory factory;
	AssignmentFieldFunctor functor;
	BigInteger type;
	BigInteger id;
	TimephasedConsumer consumer;
	
	public static TimephasedGetter getInstance(ObjectFactory factory, TimephasedConsumer consumer, AssignmentFieldFunctor functor, int type, long id) {
		return new TimephasedGetter(factory, consumer, functor, type, id);
	}
	private TimephasedGetter(ObjectFactory factory, TimephasedConsumer consumer, AssignmentFieldFunctor functor, int type, long id) {
		this.factory = factory;
		this.consumer = consumer;
		this.functor = functor;
		this.type = BigInteger.valueOf(type);
		this.id = BigInteger.valueOf(id);
	}
	public void execute(Object arg0) {
		if (!consumer.acceptValue(functor.getValue())) return;
		
		TimephasedDataType timephasedDataType;
		//try {
			timephasedDataType = factory.createTimephasedDataType();
//		} catch (JAXBException e) {
//			e.printStackTrace();
//			return;
//		}
		timephasedDataType.setType(this.type);
		timephasedDataType.setUID(this.id);
//		System.out.println("Id is " + id);

		timephasedDataType.setUnit(BigInteger.valueOf(3L));
		HasStartAndEnd interval = (HasStartAndEnd)arg0;
		Calendar startCal = DateTime.calendarInstance();
		startCal.setTimeInMillis(DateTime.fromGmt(interval.getStart())); // for 2007, convert from gmt
		Calendar endCal = DateTime.calendarInstance();
		endCal.setTimeInMillis(DateTime.fromGmt(interval.getEnd())); // for 2007, convert from gmt
		timephasedDataType.setStart(startCal.getTime());
		timephasedDataType.setFinish(endCal.getTime());
		double v = functor.getValue() / WorkCalendar.MILLIS_IN_HOUR;
		net.sf.mpxj.Duration d = net.sf.mpxj.Duration.getInstance(v,TimeUnit.HOURS);
		XsdDuration xsdDuration = new XsdDuration(d);
		timephasedDataType.setValue(xsdDuration.toString());
		consumer.consumeTimephased(timephasedDataType);
	}

}
