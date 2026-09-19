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
public interface ProjectType { // note that id's are same as mpx
	enum Kind {
		OTHER(0), PROFESSIONAL_SERVICES(1), PRODUCT_DEVELOPMENT(2), EVENT_PLANNING(3),
		MARKETING_CAMPAIGN(4), SALES_CAMPAIGN(5), TECHNICAL_SUPPORT(6), IT(7);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown project type: " + code);
		}
	}

	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int OTHER=0;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int PROFESSIONAL_SERVICES=1;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int PRODUCT_DEVELOPMENT=2;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int EVENT_PLANNING=3;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int MARKETING_CAMPAIGN=4;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int SALES_CAMPAIGN=5;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int TECHNICAL_SUPPORT=6;
	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated
	public static final int IT=7;
}
