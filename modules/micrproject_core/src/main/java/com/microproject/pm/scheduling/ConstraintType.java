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
package com.microproject.pm.scheduling;
/**
 * @stereotype enumeration
 */
public interface ConstraintType { // note that id's are same as mpx
	/** Type-safe view of the persisted MPX constraint codes. */
	enum Kind {
		ASAP(0), ALAP(1), MSO(2), MFO(3), SNET(4), SNLT(5), FNET(6), FNLT(7), HAMM(100);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown constraint type: " + code);
		}
	}

	/** @deprecated use {@link Kind} at new API boundaries. */
	@Deprecated static Kind kind(int code) { return Kind.fromCode(code); }

	/** @deprecated use {@link Kind#code()} at new API boundaries. */
	@Deprecated static int code(Kind kind) { return java.util.Objects.requireNonNull(kind, "kind").code(); }

	/** @deprecated use {@link Kind#ASAP}. */
	@Deprecated
	public static final int ASAP = 0;
	/** @deprecated use {@link Kind#ALAP}. */
	@Deprecated
	public static final int ALAP = 1;
	/** @deprecated use {@link Kind#MSO}. */
	@Deprecated
	public static final int MSO  = 2;
	/** @deprecated use {@link Kind#MFO}. */
	@Deprecated
	public static final int MFO  = 3;
	/** @deprecated use {@link Kind#SNET}. */
	@Deprecated
	public static final int SNET = 4;
	/** @deprecated use {@link Kind#SNLT}. */
	@Deprecated
	public static final int SNLT = 5;
	/** @deprecated use {@link Kind#FNET}. */
	@Deprecated
	public static final int FNET = 6;
	/** @deprecated use {@link Kind#FNLT}. */
	@Deprecated
	public static final int FNLT = 7;
	/** @deprecated use {@link Kind#HAMM}. */
	@Deprecated
	public static final int HAMM = 100;
}
