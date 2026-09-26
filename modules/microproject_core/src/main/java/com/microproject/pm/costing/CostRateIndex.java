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
public interface CostRateIndex { // note that id's are same as mpx
	enum Kind {
		A(0), B(1), C(2), D(3), E(4);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown cost-rate index: " + code);
		}
		public static Kind fromCodeOrNull(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			return null;
		}
	}
	/** @deprecated use {@link Kind#A} at typed boundaries. */
	@Deprecated int A = Kind.A.code();
	/** @deprecated use {@link Kind#B} at typed boundaries. */
	@Deprecated int B = Kind.B.code();
	/** @deprecated use {@link Kind#C} at typed boundaries. */
	@Deprecated int C = Kind.C.code();
	/** @deprecated use {@link Kind#D} at typed boundaries. */
	@Deprecated int D = Kind.D.code();
	/** @deprecated use {@link Kind#E} at typed boundaries. */
	@Deprecated int E = Kind.E.code();
}
