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
package com.microproject.datatype;

import java.util.Objects;

import com.microproject.options.CalendarOption;
import com.microproject.options.ScheduleOption;

/**
 * Unit defaults used to interpret non-elapsed project durations.
 *
 * <p>The values are project data, not process-wide display settings. Keeping a
 * snapshot allows two open projects to interpret day and week durations
 * independently.</p>
 */
public record DurationDefaults(double minutesPerDay, double minutesPerWeek, double daysPerMonth,
	int durationEnteredIn, int workUnit) implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public static final DurationDefaults PRODUCT_DEFAULTS = new DurationDefaults(480.0D, 2400.0D, 20.0D,
		TimeUnit.DAYS, TimeUnit.HOURS);

	public DurationDefaults {
		if (!Double.isFinite(minutesPerDay) || minutesPerDay <= 0.0D
				|| !Double.isFinite(minutesPerWeek) || minutesPerWeek <= 0.0D
				|| !Double.isFinite(daysPerMonth) || daysPerMonth <= 0.0D)
			throw new IllegalArgumentException("Duration defaults must be finite and positive");
	}

	public DurationDefaults(double minutesPerDay, double minutesPerWeek, double daysPerMonth) {
		this(minutesPerDay, minutesPerWeek, daysPerMonth, TimeUnit.DAYS, TimeUnit.HOURS);
	}

	public static DurationDefaults from(CalendarOption options) {
		return from(options, ScheduleOption.getInstance());
	}

	public static DurationDefaults from(CalendarOption options, ScheduleOption scheduleOptions) {
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(scheduleOptions, "scheduleOptions");
		return new DurationDefaults(options.getHoursPerDay() * 60.0D,
			options.getHoursPerWeek() * 60.0D, options.getDaysPerMonth(),
			scheduleOptions.getDurationEnteredIn(), scheduleOptions.getWorkUnit());
	}
}
