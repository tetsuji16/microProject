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
package com.microproject.functor;

import java.util.function.Consumer;


/**
 * A closure which holds a number
 */
public abstract class NumberClosure extends Number implements Consumer<Object> {
	Number value;

	public NumberClosure(long l) {
		setLongValue(l);
	}
	public NumberClosure(double d) {
		setDoubleValue(d);
	}
	public long getLongValue() {
		return value.longValue();
	}
	public void setLongValue(long l) {
		value = Long.valueOf(l);
	}
	public void setDoubleValue(double d) {
		value = Double.valueOf(d);
	}
	public double doubleValue() {
		return value.doubleValue();
	}
	public float floatValue() {
		return value.floatValue();
	}
	public int intValue() {
		return value.intValue();
	}
	public long longValue() {
		return value.longValue();
	}
}
