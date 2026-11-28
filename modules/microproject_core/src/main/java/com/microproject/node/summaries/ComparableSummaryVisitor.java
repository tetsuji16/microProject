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
package com.microproject.node.summaries;

/**
 * Base visitor for the summaries that keep one of the summarized values, used by {@link Maximum}
 * and {@link Minimum}.
 *
 * <p>The summarized field decides the value type, so the comparison is the one {@link Comparable}
 * declares on the first stored value: dates compare chronologically, numbers numerically and text
 * lexicographically. {@code null} values are ignored, a value of a different type keeps throwing
 * {@link ClassCastException} as before, and a tie keeps the value summarized first.
 */
public abstract class ComparableSummaryVisitor extends SummaryVisitor {
	protected Comparable summary;

	public ComparableSummaryVisitor() {
		super();
	}

	public void reset() {
		summary = null;
	}

	public Object getSummary() {
		return summary;
	}

	/**
	 * Keeps {@code value} when it is strictly beyond the current summary in the requested
	 * direction; an equal value does not replace the stored one.
	 *
	 * @param value    the value to summarize, ignored when {@code null}
	 * @param greatest {@code true} for a maximum, {@code false} for a minimum
	 */
	protected final void keepBoundary(Object value, boolean greatest) {
		if (value == null) return;
		if (summary == null || (compareSummaryWith(value) < 0) == greatest) summary = asComparable(value);
	}

	@SuppressWarnings("unchecked")
	private static Comparable<Object> asComparable(Object value) {
		return (Comparable<Object>) value;
	}

	private int compareSummaryWith(Object value) {
		return asComparable(summary).compareTo(value);
	}
}
