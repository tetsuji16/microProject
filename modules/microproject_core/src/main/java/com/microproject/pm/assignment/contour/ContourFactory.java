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
package com.microproject.pm.assignment.contour;

/**
 *
 */
public class ContourFactory {
	/** @deprecated use {@link #getInstance(ContourTypes.Kind)} for typed access. */
	@Deprecated
	public static AbstractContour getInstance(int type) {
		return getInstance(ContourTypes.Kind.fromCode(type));
	}

	public static AbstractContour getInstance(ContourTypes.Kind type) {
		return switch (type) {
			case FLAT -> StandardContour.FLAT_CONTOUR;
			case BACK_LOADED -> StandardContour.BACK_LOADED_CONTOUR;
			case FRONT_LOADED -> StandardContour.FRONT_LOADED_CONTOUR;
			case DOUBLE_PEAK -> StandardContour.DOUBLE_PEAK_CONTOUR;
			case EARLY_PEAK -> StandardContour.EARLY_PEAK_CONTOUR;
			case LATE_PEAK -> StandardContour.LATE_PEAK_CONTOUR;
			case BELL -> StandardContour.BELL_CONTOUR;
			case PLATEAU -> StandardContour.PLATEAU_CONTOUR;
			case CONTOURED -> throw new IllegalArgumentException("Unknown contour type: " + type.code());
		};
	}
}
