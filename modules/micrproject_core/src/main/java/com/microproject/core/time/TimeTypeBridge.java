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
package com.microproject.core.time;

import java.util.Objects;

/**
 * Explicit adapter between MPXJ-compatible mutable time values and the
 * packed domain time types.  Keeping this boundary in one place prevents
 * callers from accidentally mixing the two APIs while preserving old POD/MPX
 * conversion signatures.
 */
public final class TimeTypeBridge {
	private TimeTypeBridge() {
	}

	public static int toDomainUnit(TimeUnit unit) {
		return unitCodeByName(Objects.requireNonNull(unit, "unit").name());
	}

	private static int unitCodeByName(String name) {
		return switch (name) {
			case "NON_TEMPORAL" -> com.microproject.datatype.TimeUnit.NON_TEMPORAL;
			case "NONE" -> com.microproject.datatype.TimeUnit.NONE;
			case "MINUTES" -> com.microproject.datatype.TimeUnit.MINUTES;
			case "HOURS" -> com.microproject.datatype.TimeUnit.HOURS;
			case "DAYS" -> com.microproject.datatype.TimeUnit.DAYS;
			case "WEEKS" -> com.microproject.datatype.TimeUnit.WEEKS;
			case "MONTHS" -> com.microproject.datatype.TimeUnit.MONTHS;
			case "PERCENT" -> com.microproject.datatype.TimeUnit.PERCENT;
			case "YEARS" -> com.microproject.datatype.TimeUnit.YEARS;
			case "ELAPSED_MINUTES" -> com.microproject.datatype.TimeUnit.ELAPSED_MINUTES;
			case "ELAPSED_HOURS" -> com.microproject.datatype.TimeUnit.ELAPSED_HOURS;
			case "ELAPSED_DAYS" -> com.microproject.datatype.TimeUnit.ELAPSED_DAYS;
			case "ELAPSED_WEEKS" -> com.microproject.datatype.TimeUnit.ELAPSED_WEEKS;
			case "ELAPSED_MONTHS" -> com.microproject.datatype.TimeUnit.ELAPSED_MONTHS;
			case "ELAPSED_YEARS" -> com.microproject.datatype.TimeUnit.ELAPSED_YEARS;
			case "ELAPSED_PERCENT" -> com.microproject.datatype.TimeUnit.ELAPSED_PERCENT;
			default -> throw new IllegalArgumentException("Unsupported legacy time unit: " + name);
		};
	}

	public static com.microproject.datatype.Duration toDomainDuration(Duration value) {
		Objects.requireNonNull(value, "value");
		return new com.microproject.datatype.Duration(
				com.microproject.datatype.Duration.getInstance(value.getValue(), toDomainUnit(value.getUnit())));
	}

	public static com.microproject.datatype.Rate toDomainRate(Rate value) {
		Objects.requireNonNull(value, "value");
		return new com.microproject.datatype.Rate(value.getValue(), toDomainUnit(value.getUnit()));
	}

	public static TimeUnit fromDomainUnit(int unit) {
		return switch (unit) {
			case com.microproject.datatype.TimeUnit.NON_TEMPORAL -> TimeUnit.NON_TEMPORAL;
			case com.microproject.datatype.TimeUnit.NONE -> TimeUnit.NONE;
			case com.microproject.datatype.TimeUnit.MINUTES -> TimeUnit.MINUTES;
			case com.microproject.datatype.TimeUnit.HOURS -> TimeUnit.HOURS;
			case com.microproject.datatype.TimeUnit.DAYS -> TimeUnit.DAYS;
			case com.microproject.datatype.TimeUnit.WEEKS -> TimeUnit.WEEKS;
			case com.microproject.datatype.TimeUnit.MONTHS -> TimeUnit.MONTHS;
			case com.microproject.datatype.TimeUnit.PERCENT -> TimeUnit.PERCENT;
			case com.microproject.datatype.TimeUnit.YEARS -> TimeUnit.YEARS;
			case com.microproject.datatype.TimeUnit.ELAPSED_MINUTES -> TimeUnit.ELAPSED_MINUTES;
			case com.microproject.datatype.TimeUnit.ELAPSED_HOURS -> TimeUnit.ELAPSED_HOURS;
			case com.microproject.datatype.TimeUnit.ELAPSED_DAYS -> TimeUnit.ELAPSED_DAYS;
			case com.microproject.datatype.TimeUnit.ELAPSED_WEEKS -> TimeUnit.ELAPSED_WEEKS;
			case com.microproject.datatype.TimeUnit.ELAPSED_MONTHS -> TimeUnit.ELAPSED_MONTHS;
			case com.microproject.datatype.TimeUnit.ELAPSED_YEARS -> TimeUnit.ELAPSED_YEARS;
			case com.microproject.datatype.TimeUnit.ELAPSED_PERCENT -> TimeUnit.ELAPSED_PERCENT;
			default -> throw new IllegalArgumentException("Unsupported domain time unit: " + unit);
		};
	}

	public static Duration fromDomainDuration(com.microproject.datatype.Duration value) {
		Objects.requireNonNull(value, "value");
		return new Duration(com.microproject.datatype.Duration.getValue(value.getEncodedMillis()),
				fromDomainUnit(com.microproject.datatype.Duration.getType(value.getEncodedMillis())));
	}

	public static Rate fromDomainRate(com.microproject.datatype.Rate value) {
		Objects.requireNonNull(value, "value");
		return new Rate(value.getValue(), fromDomainUnit(value.getTimeUnit()));
	}
}
