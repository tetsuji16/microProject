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
package com.microproject.pm.assignment.timesheet;

public interface TimesheetStatus {
	public enum Kind {
		MIXED(-1), NO_DATA(0), ENTERED(1), VALIDATED(2), INTEGRATED(3), REJECTED(4), SAVED(5);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown timesheet status code: " + code);
		}
	}
	/** @deprecated use {@link Kind#NO_DATA}; retained for serialized compatibility. */
	@Deprecated public static final int NO_DATA = 0;
	/** @deprecated use {@link Kind#ENTERED}; retained for serialized compatibility. */
	@Deprecated public static final int ENTERED = 1;
	/** @deprecated use {@link Kind#VALIDATED}; retained for serialized compatibility. */
	@Deprecated public static final int VALIDATED = 2;
	/** @deprecated use {@link Kind#INTEGRATED}; retained for serialized compatibility. */
	@Deprecated public static final int INTEGRATED = 3;
	/** @deprecated use {@link Kind#REJECTED}; retained for serialized compatibility. */
	@Deprecated public static final int REJECTED = 4;
	/** @deprecated use {@link Kind#SAVED}; retained for serialized compatibility. */
	@Deprecated public static final int SAVED = 5;
	/** @deprecated use {@link Kind#MIXED}; retained for serialized compatibility. */
	@Deprecated public static final int MIXED = -1;
}
