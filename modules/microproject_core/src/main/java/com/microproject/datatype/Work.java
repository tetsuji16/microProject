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
package com.microproject.datatype;

/**
 * A duration measured in work time rather than calendar time.
 */
public class Work extends Duration {
	private static final long serialVersionUID = 1250598041062656391L;

	/**
	 * @param encodedMillis duration in the encoded millisecond form of {@link Duration}
	 */
	public Work(long encodedMillis) {
		super(encodedMillis);
		setWork(true);
	}

	/**
	 * Creates a work duration from a boxed length. A null argument yields a
	 * zero-length work duration, which is how a missing serialized value is
	 * read back.
	 *
	 * @param doubleObject duration in the encoded millisecond form, or null for zero
	 * @return the work duration, never null
	 */
	public static Work getWorkInstanceFromDouble(Double doubleObject) {
		return new Work(doubleObject == null ? 0L : doubleObject.longValue());
	}

}
