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
package com.microproject.pm.assignment;

import com.microproject.field.FieldContext;

/**
 * Basic time distributed values.  See also EarnedValueFields for the fields that use them.
 */
public interface TimeDistributedFields {
	double getCost(FieldContext fieldContext);
	long getWork(FieldContext fieldContext);
	void setWork(long work,FieldContext fieldContext);	
	boolean isReadOnlyWork(FieldContext fieldContext);
	double getActualCost(FieldContext fieldContext);
	long getActualWork(FieldContext fieldContext);
	void setActualWork(long actualWork,FieldContext fieldContext);
	boolean isReadOnlyActualWork(FieldContext fieldContext);
	long getRemainingWork(FieldContext fieldContext);
	void setRemainingWork(long remainingWork,FieldContext fieldContext);
	boolean isReadOnlyRemainingWork(FieldContext fieldContext);
	double getBaselineCost(int numBaseline, FieldContext fieldContext);
	long getBaselineWork(int numBaseline, FieldContext fieldContext);
	boolean fieldHideCost(FieldContext fieldContext);
	boolean fieldHideWork(FieldContext fieldContext);
	boolean fieldHideBaselineCost(int numBaseline,FieldContext fieldContext);
	boolean fieldHideBaselineWork(int numBaseline,FieldContext fieldContext);
	boolean fieldHideActualCost(FieldContext fieldContext);
	boolean fieldHideActualWork(FieldContext fieldContext);
	double getFixedCost(FieldContext fieldContext);
	void setFixedCost(double fixedCost, FieldContext fieldContext);
	boolean isReadOnlyFixedCost(FieldContext fieldContext);
	double getActualFixedCost(FieldContext fieldContext);
	boolean fieldHideActualFixedCost(FieldContext fieldContext);
	double getRemainingCost(FieldContext fieldContext);
}
