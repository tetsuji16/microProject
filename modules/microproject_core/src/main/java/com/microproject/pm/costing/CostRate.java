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
package com.microproject.pm.costing;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.microproject.datatype.Rate;
import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.interval.ValueObjectForIntervalTable;
/**
 * Standard and overtime cost rates are expressesed as cost/millisecond
 * Fixed cost is a simple scalar value, not a value.
 */
public class CostRate extends ValueObjectForInterval implements Cost {
	static final long serialVersionUID = 1726666221119L;
	transient Rate standardRate = new Rate();
	transient Rate overtimeRate = new Rate();
	double costPerUse = 0.0;
	/**
	 * @param i
	 */
	public CostRate(ValueObjectForIntervalTable table, long start) {
		super(table,start);
	}

	/**
	 * @return Returns the costPerUse.
	 */
	public double getCostPerUse() {
		return costPerUse;
	}

	/**
	 * @param costPerUse The costPerUse to set.
	 */
	public void setCostPerUse(double costPerUse) {
		this.costPerUse = costPerUse;
	}

	/**
	 * @return Returns the overtimeRate.
	 */
	public Rate getOvertimeRate() {
		return overtimeRate;
	}

	/**
	 * @param overtimeRate The overtimeRate to set.
	 */
	public void setOvertimeRate(Rate overtimeRate) {
		this.overtimeRate = overtimeRate;
	}

	/**
	 * @return Returns the standardRate.
	 */
	public Rate getStandardRate() {
		return standardRate;
	}

	/**
	 * @param standardRate The standardRate to set.
	 */
	public void setStandardRate(Rate standardRate) {
		this.standardRate = standardRate;
	}

	public long getEffectiveDate() {
		return getStart();
	}

	public void setEffectiveDate(long effectiveDate) throws InvalidValueObjectForIntervalException {
		table.adjustStart(effectiveDate,this);
	}

	public boolean isReadOnlyEffectiveDate(FieldContext fieldContext) {
		return this.isDefault();
	}

	public boolean fieldHideOvertimeRate(FieldContext fieldContext) {
		return false;
	}


	private void writeObject(ObjectOutputStream s) throws IOException {
	    s.defaultWriteObject();
	    standardRate.serialize(s);
	    overtimeRate.serialize(s);
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    standardRate=Rate.deserialize(s);
	    overtimeRate=Rate.deserialize(s);
	}


}

