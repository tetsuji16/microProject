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
/**
 * @stereotype enumeration
 */
public interface ExpenseType { // Capitalize Cost, Expense Cost, Overhead Cost, Indirect Cost, Direct Cost (these overlap with each other but for a demo would expose the different options to be reported against)
	enum Kind {
		NONE(0), CAPITALIZE(1), EXPENSE(2), OVERHEAD(3), INDIRECT(4), DIRECT(5);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown expense type: " + code);
		}
	}

	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int NONE=0;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int CAPITALIZE=1;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int EXPENSE=2;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int OVERHEAD=3;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int INDIRECT=4;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int DIRECT=5;
}
