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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.microproject.interval.InvalidValueObjectForIntervalException;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
import com.microproject.core.time.TimeUtil;
import com.microproject.pm.availability.Availability;
import com.microproject.pm.availability.AvailabilityTable;
import com.microproject.util.DateTime;

import net.sf.mpxj.Resource;
import net.sf.mpxj.common.DateHelper;

/** Maps the native interval availability table to MPXJ's inclusive period table. */
public final class MpxResourceAvailabilityConverter {
	private static final long SECOND_MILLIS = 1_000L;
	private static final long DOMAIN_START = DateTime.NA_TIME.getTime();
	private static final long DOMAIN_END = DateTime.getMaxDate().getTime();

	private MpxResourceAvailabilityConverter() {
	}

	/** Validates and imports availability before the caller mutates any resource fields. */
	public static void from(Resource source, AvailabilityTable target) {
		List<Period> periods = readPeriods(source);
		if (periods == null) return; // No source availability fields: preserve the target.
		applyPeriods(periods, target);
	}

	/** Writes every domain row, including default and zero rows, as an MPXJ period. */
	public static void to(AvailabilityTable source, Resource target) {
		List<net.sf.mpxj.Availability> converted = new ArrayList<>();
		long previousStart = DOMAIN_START;
		for (ValueObjectForInterval value : source.getList()) {
			if (!(value instanceof Availability availability))
				throw new IllegalArgumentException("Unsupported resource availability row");
			long start = value.getStart();
			long end = value.getEnd();
			if (start != previousStart || end <= start || end > DOMAIN_END)
				throw new IllegalArgumentException("Invalid resource availability interval");
			validateUnits(availability.getMaximumUnits());
			double percentage = availability.getMaximumUnits() * 100.0D;
			validateUnits(percentage);
			Date mpxStart = start == DOMAIN_START ? DateHelper.START_DATE_NA : finiteDate(start, "start");
			Date mpxEnd = end == DOMAIN_END ? DateHelper.END_DATE_NA : finiteDate(end - SECOND_MILLIS, "end");
			converted.add(new net.sf.mpxj.Availability(mpxStart, mpxEnd, Double.valueOf(percentage)));
			previousStart = end;
		}
		if (previousStart != DOMAIN_END)
			throw new IllegalArgumentException("Resource availability table does not cover its full range");
		target.getAvailability().clear();
		target.getAvailability().addAll(converted);
	}

	private static List<Period> readPeriods(Resource source) {
		List<Period> result = new ArrayList<>();
		if (!source.getAvailability().isEmpty()) {
			long previousEnd = DOMAIN_START;
			for (net.sf.mpxj.Availability value : source.getAvailability()) {
				long start = sourceBoundary(value.getRange().getStart(), true);
				long end = sourceBoundary(value.getRange().getEnd(), false);
				if (start < previousEnd || end <= start)
					throw new IllegalArgumentException("Overlapping or invalid resource availability periods");
				Number units = value.getUnits();
				// MSPDI marks AvailableUnits optional but defines no default; do not invent one.
				if (units == null)
					throw new IllegalArgumentException("Resource availability period is missing AvailableUnits");
				double normalizedUnits = units.doubleValue() / 100.0D;
				validateUnits(normalizedUnits);
				result.add(new Period(start, end, normalizedUnits));
				previousEnd = end;
			}
			return result;
		}

		Number maximumUnits = source.getMaxUnits();
		Date availableFrom = source.getAvailableFrom();
		Date availableTo = source.getAvailableTo();
		if (maximumUnits == null && availableFrom == null && availableTo == null) return null;
		if (maximumUnits == null)
			throw new IllegalArgumentException("Resource availability bounds are missing MaxUnits");
		double units = maximumUnits.doubleValue() / 100.0D;
		validateUnits(units);
		long start = sourceBoundary(availableFrom, true);
		long end = sourceBoundary(availableTo, false);
		if (end <= start) throw new IllegalArgumentException("Invalid resource availability bounds");
		result.add(new Period(start, end, units));
		return result;
	}

	private static void applyPeriods(List<Period> periods, AvailabilityTable target) {
		List<Period> transitions = new ArrayList<>();
		double defaultUnits = 0.0D;
		long cursor = DOMAIN_START;
		for (Period period : periods) {
			if (period.start == DOMAIN_START) {
				if (cursor != DOMAIN_START) throw new IllegalArgumentException("Unbounded availability start is out of order");
				defaultUnits = period.units;
			} else {
				if (period.start < cursor) throw new IllegalArgumentException("Overlapping resource availability periods");
				if (period.start > cursor) transitions.add(new Period(cursor, period.start, 0.0D));
				transitions.add(period);
			}
			cursor = period.end;
			if (period.end == DOMAIN_END) break;
		}
		if (cursor < DOMAIN_END) transitions.add(new Period(cursor, DOMAIN_END, 0.0D));

		// All source periods and units have been checked before the target changes.
		List<ValueObjectForInterval> existing = new ArrayList<>(target.getList());
		for (int index = existing.size() - 1; index > 0; index--) {
			try {
				target.remove(existing.get(index));
			} catch (InvalidValueObjectForIntervalException invalid) {
				throw new IllegalStateException("Could not reset resource availability table", invalid);
			}
		}
		((Availability) target.getList().getFirst()).setMaximumUnits(defaultUnits);
		for (Period transition : transitions) {
			if (transition.start == DOMAIN_START) {
				((Availability) target.getList().getFirst()).setMaximumUnits(transition.units);
			} else {
				try {
					((Availability) target.newValueObject(transition.start)).setMaximumUnits(transition.units);
				} catch (InvalidValueObjectForIntervalException invalid) {
					throw new IllegalStateException("Validated availability transition could not be applied", invalid);
				}
			}
		}
	}

	private static long sourceBoundary(Date value, boolean start) {
		if (value == null) return start ? DOMAIN_START : DOMAIN_END;
		if (start && DateHelper.END_DATE_NA.equals(value)
				|| !start && DateHelper.START_DATE_NA.equals(value))
			throw new IllegalArgumentException("Resource availability uses a sentinel at the wrong boundary");
		if (start && DateHelper.START_DATE_NA.equals(value)) return DOMAIN_START;
		if (!start && DateHelper.END_DATE_NA.equals(value)) return DOMAIN_END;
		long millis = DateUTCConverter.toModelTime(value);
		if (Math.floorMod(millis, SECOND_MILLIS) != 0L)
			throw new IllegalArgumentException("Resource availability boundary is not second-aligned");
		if (start) {
			if (millis < DOMAIN_START || millis >= DOMAIN_END)
				throw new IllegalArgumentException("Resource availability start is outside the supported range");
			return millis;
		}
		if (millis < DOMAIN_START || millis >= DOMAIN_END)
			throw new IllegalArgumentException("Resource availability end is outside the supported range");
		long exclusiveEnd = Math.addExact(millis, SECOND_MILLIS); // MPXJ end is inclusive; domain end is exclusive.
		if (exclusiveEnd >= DOMAIN_END)
			throw new IllegalArgumentException("Finite resource availability end collides with the unbounded sentinel");
		return exclusiveEnd;
	}

	private static Date finiteDate(long millis, String boundary) {
		if (millis == DOMAIN_START || millis >= DOMAIN_END || Math.floorMod(millis, SECOND_MILLIS) != 0L)
			throw new IllegalArgumentException("Resource availability " + boundary + " is not representable at second precision");
		return new Date(TimeUtil.removeTimeZoneOffset(millis));
	}

	private static void validateUnits(double units) {
		if (!Double.isFinite(units) || units < 0.0D)
			throw new IllegalArgumentException("Invalid resource availability units: " + units);
	}

	private record Period(long start, long end, double units) {
	}
}
