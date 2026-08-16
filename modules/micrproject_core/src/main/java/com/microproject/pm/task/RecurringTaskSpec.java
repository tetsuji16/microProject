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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RecurringTaskSpec {
	public enum PatternType {
		DAILY,
		WEEKLY,
		MONTHLY
	}

	public enum RangeType {
		END_BY_DATE,
		END_AFTER_OCCURRENCES
	}

	private final String name;
	private final long start;
	private final long duration;
	private final PatternType patternType;
	private final RangeType rangeType;
	private final long endDate;
	private final int occurrenceCount;
	private final Set<Integer> weeklyDays;

	public RecurringTaskSpec(
		String name,
		long start,
		long duration,
		PatternType patternType,
		RangeType rangeType,
		long endDate,
		int occurrenceCount,
		Set<Integer> weeklyDays) {
		validate(patternType, rangeType, start, endDate, occurrenceCount, weeklyDays);
		this.name = name;
		this.start = start;
		this.duration = duration;
		this.patternType = patternType;
		this.rangeType = rangeType;
		this.endDate = endDate;
		this.occurrenceCount = occurrenceCount;
		LinkedHashSet<Integer> days = new LinkedHashSet<Integer>();
		if (weeklyDays != null)
			days.addAll(weeklyDays);
		this.weeklyDays = Collections.unmodifiableSet(days);
	}

	private static void validate(
		PatternType patternType,
		RangeType rangeType,
		long start,
		long endDate,
		int occurrenceCount,
		Set<Integer> weeklyDays) {
		if (patternType == null)
			throw new IllegalArgumentException("patternType is required");
		if (rangeType == null)
			throw new IllegalArgumentException("rangeType is required");
		if (rangeType == RangeType.END_AFTER_OCCURRENCES && occurrenceCount <= 0)
			throw new IllegalArgumentException("occurrenceCount must be positive");
		if (rangeType == RangeType.END_BY_DATE && endDate < start)
			throw new IllegalArgumentException("endDate must not be before start");
		if (patternType == PatternType.WEEKLY && (weeklyDays == null || weeklyDays.isEmpty()))
			throw new IllegalArgumentException("weeklyDays must not be empty");
	}

	public String getName() {
		return name;
	}

	public long getStart() {
		return start;
	}

	public long getDuration() {
		return duration;
	}

	public PatternType getPatternType() {
		return patternType;
	}

	public RangeType getRangeType() {
		return rangeType;
	}

	public long getEndDate() {
		return endDate;
	}

	public int getOccurrenceCount() {
		return occurrenceCount;
	}

	public Set<Integer> getWeeklyDays() {
		return weeklyDays;
	}
}
