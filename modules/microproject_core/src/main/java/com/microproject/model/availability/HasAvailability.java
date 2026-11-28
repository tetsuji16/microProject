/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.availability;

import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;

/**
 * Contract for a model object with maximum resource units and an availability interval.
 * Field editability and visibility are evaluated in the caller's {@link FieldContext}.
 */
public interface HasAvailability {
	/** Returns the maximum assigned units allowed for this resource. */
	double getMaximumUnits();
	/** Sets the maximum assigned units allowed for this resource. */
	void setMaximumUnits(double maximumUnits);
	/** Returns the start of the current availability interval. */
	long getAvailableFrom();
	/** Returns the end of the current availability interval. */
	long getAvailableTo();
	/** Sets the start of the availability interval. */
	void setAvailableFrom(long availableFrom) throws InvalidValueObjectForIntervalException;
	/** Sets the end of the availability interval. */
	void setAvailableTo(long availableTo);
	/** Returns whether the start is read-only in this field context. */
	boolean isReadOnlyAvailableFrom(FieldContext fieldContext);
	/** Returns whether the end is read-only in this field context. */
	boolean isReadOnlyAvailableTo(FieldContext fieldContext);
	/** Returns whether the maximum-units field is hidden in this field context. */
	boolean fieldHideMaximumUnits(FieldContext fieldContext);
	
}
