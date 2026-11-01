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
package com.microproject.pm.assignment;

import java.util.Collection;
import java.util.function.ToLongFunction;

import com.microproject.association.Association;

/** Stateless calculations over an assignment collection. */
public final class AssignmentValueRollup {
	private AssignmentValueRollup() {
	}

	public static long earliestStart(Collection<? extends Association> assignments) {
		return minimum(assignments, Assignment::getStart);
	}

	public static long earliestResume(Collection<? extends Association> assignments) {
		return minimum(assignments, Assignment::getResume);
	}

	public static long earliestStop(Collection<? extends Association> assignments) {
		return minimum(assignments, Assignment::getEarliestStop);
	}

	public static double maximumLaborUnits(Collection<? extends Association> assignments) {
		double result = 0;
		for (Association association : assignments)
			result = Math.max(result, ((Assignment) association).getLaborUnits());
		return result;
	}

	private static long minimum(Collection<? extends Association> assignments, ToLongFunction<Assignment> value) {
		long result = Long.MAX_VALUE;
		for (Association association : assignments)
			result = Math.min(result, value.applyAsLong((Assignment) association));
		return result;
	}
}
