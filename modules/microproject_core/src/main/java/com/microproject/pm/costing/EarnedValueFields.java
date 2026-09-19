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

import com.microproject.field.FieldContext;

/**
 * 
 */
public interface EarnedValueFields extends BasicEarnedValueFields /* , EarnedValueIndicatorFields */ {

	
	double getCv(FieldContext fieldContext);	
	double getSv(FieldContext fieldContext);	
	double getEac(FieldContext fieldContext);	
	double getVac(FieldContext fieldContext);	
	double getCpi(FieldContext fieldContext);	
	double getSpi(FieldContext fieldContext);
	double getCsi(FieldContext fieldContext);
	double getCvPercent(FieldContext fieldContext);	
	double getSvPercent(FieldContext fieldContext);
	double getTcpi(FieldContext fieldContext);
	
	boolean fieldHideCv(FieldContext fieldContext);
	boolean fieldHideSv(FieldContext fieldContext);
	boolean fieldHideEac(FieldContext fieldContext);
	boolean fieldHideVac(FieldContext fieldContext);
	boolean fieldHideCpi(FieldContext fieldContext);
	boolean fieldHideSpi(FieldContext fieldContext);
	boolean fieldHideCvPercent(FieldContext fieldContext);
	boolean fieldHideSvPercent(FieldContext fieldContext);
	boolean fieldHideTcpi(FieldContext fieldContext);	
}
