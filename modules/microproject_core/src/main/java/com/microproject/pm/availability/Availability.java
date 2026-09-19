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
package com.microproject.pm.availability;
import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.util.DateTime;

/**
 * Standard and overtime cost rates are expressesed as cost/millisecond
 * Fixed cost is a simple scalar value, not a value.
 */
public class Availability extends ValueObjectForInterval implements HasAvailability{
	static final long serialVersionUID = 3647989273828L;
	double maximumUnits = 1.0;
	/**
	 * @param i
	 */
	public Availability(ValueObjectForIntervalTable table,long start) {
		super(table,start);
	}

	/**
	 * @return Returns the maximumUnits.
	 */
	public double getMaximumUnits() {
		return maximumUnits;
	}

	/**
	 * @param maximumUnits The maximumUnits to set.
	 */
	public void setMaximumUnits(double maximumUnits) {
		this.maximumUnits = maximumUnits;
	}

	public long getAvailableFrom() {
		return getStart();
	}
	public long getAvailableTo() {
		return getEnd();
	}

	public void setAvailableFrom(long availableFrom) throws InvalidValueObjectForIntervalException {
		table.adjustStart(availableFrom,this);
	}

	public void setAvailableTo(long availableTo) {
		availableTo = Math.min(availableTo,DateTime.getMaxDate().getTime());
		setEnd(availableTo);
	}

	public boolean isReadOnlyAvailableFrom(FieldContext fieldContext) {
		return isDefault();
	}

	public boolean isReadOnlyAvailableTo(FieldContext fieldContext) {
		return isDefault();
	}

	public boolean fieldHideMaximumUnits(FieldContext fieldContext) {
		return false;
	}
	
	

}

