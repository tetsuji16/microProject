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
package com.microproject.pm.task;
/**
 * @stereotype enumeration
 */
public interface ProjectStatus {
	enum Kind {
		PLANNING(0), PENDING_APPROVAL(1), ACTIVE(2), COMPLETED(3), CANCELLED(4), ON_HOLD(5);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown project status: " + code);
		}
	}

	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int PLANNING=0;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int PENDING_APPROVAL=1;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int ACTIVE=2;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int COMPLETED=3;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int CANCELLED=4;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int ON_HOLD=5;
}
