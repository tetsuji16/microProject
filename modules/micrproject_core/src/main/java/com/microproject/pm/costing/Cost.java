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

import com.microproject.datatype.Rate;
import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;

/**
 * 
 */
public interface Cost {
	public double getCostPerUse();
	public Rate getOvertimeRate();
	public Rate getStandardRate();
	public long getEffectiveDate();
	/**
	 * @param costPerUse The costPerUse to set.
	 */
	public void setCostPerUse(double costPerUse);
	/**
	 * @param overtimeRate The overtimeRate to set.
	 */
	public void setOvertimeRate(Rate overtimeRate);
	/**
	 * @param standardRate The standardRate to set.
	 */
	public void setStandardRate(Rate standardRate);

	public void setEffectiveDate(long effectiveDate) throws InvalidValueObjectForIntervalException;
	public boolean isReadOnlyEffectiveDate(FieldContext fieldContext);
	boolean fieldHideOvertimeRate(FieldContext fieldContext);
	
}
