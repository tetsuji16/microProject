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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.microproject.options.ScheduleOption;

/**
 * 
 */
public final class Rate implements Comparable, Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1748666535067764189L;
	double value = 0.0D;
	int timeUnit = ScheduleOption.getInstance().getRateEnteredIn(); // used for formatting
	
	public final double getValue() {
		return value;
	}
	public final void setValue(double rate) {
		this.value = rate;
	}
	public final int getTimeUnit() {
		return timeUnit;
	}
	public final void setTimeUnit(int timeUnit) {
		this.timeUnit = timeUnit;
	}
	public boolean isNonTemporal() {
		return timeUnit == TimeUnit.NON_TEMPORAL;
	}
	
	public Rate() {
	}

	public Rate(double value) {
		this.value = value;
	}
	public Rate(double value, int timeUnit) {
		this.value = value;
		this.timeUnit = timeUnit;
	}
	public int compareTo(Object arg0) {
		if (!(arg0 instanceof Rate))
			throw new IllegalArgumentException();
		return (int) (value - ((Rate)arg0).value);
	}
	
	public String toString() {
		return RateFormat.getInstance(null, false,false,true).format(this);
	}
	
	public void makeUnitless() {
		value *= Duration.timeUnitFactor(timeUnit);
		timeUnit = TimeUnit.NON_TEMPORAL;
	}
	
	
	public void serialize(ObjectOutputStream s) throws IOException {
	    s.writeDouble(value);
	    s.writeInt(timeUnit);
	}
	
	public static Rate deserialize(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    return new Rate(s.readDouble(),s.readInt());
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	
}
