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
package com.microproject.core.pm.exchange.converters.mpx;

import java.math.BigInteger;

import com.microproject.core.time.TimephasedType;
import com.microproject.datatype.TimeUnit;
import net.sf.mpxj.Duration;

/**
 * Utility class for safe handling of MPXJ nullable wrapper types.
 * MPXJ library methods often return nullable wrapper types (Integer, Long, BigInteger)
 * which can be null for incomplete or malformed .mpp files. This class provides 
 * null-safe unboxing operations.
 * 
 * @author ProjectLibre
 */
public class MpxUtils {

    /**
     * Safely unbox a BigInteger to int, returning 0 for null.
     */
    public static int safeIntValue(BigInteger value) {
        return value != null ? value.intValue() : 0;
    }

    /**
     * Safely unbox an Integer to int, returning 0 for null.
     * Use when null should be treated as zero.
     */
    public static int safeIntValue(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Safely unbox an Integer to int, returning a default value for null.
     */
    public static int safeIntValue(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Safely unbox a Long to long, returning 0L for null.
     */
    public static long safeLongValue(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Safely unbox a Long to long, returning a default value for null.
     */
    public static long safeLongValue(Long value, long defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Safely get TimephasedType from a BigInteger type ID (MPXJ's getType() returns BigInteger).
     * Returns null if typeId is null or if the ID is not recognized.
     */
    public static TimephasedType safeGetTimephasedType(BigInteger typeId) {
        if (typeId == null) {
            return null;
        }
        return TimephasedType.getInstance(typeId.intValue());
    }

    /**
     * Safely get TimephasedType from an Integer type ID.
     * Returns null if typeId is null or if the ID is not recognized.
     */
    public static TimephasedType safeGetTimephasedType(Integer typeId) {
        if (typeId == null) {
            return null;
        }
        return TimephasedType.getInstance(typeId);
    }

    /**
     * Safely get TimephasedType, defaulting to REMAINING_WORK if null.
     * Use when a default type is acceptable for malformed data.
     */
    public static TimephasedType safeGetTimephasedTypeOrDefault(Integer typeId) {
        TimephasedType result = safeGetTimephasedType(typeId);
        return result != null ? result : TimephasedType.REMAINING_WORK;
    }

    /**
     * Number of minutes represented by one unit of the given MPXJ TimeUnit value
     * (see {@code net.sf.mpxj.TimeUnit.getValue()}).
     *
     * MPXJ TimeUnit is 0-based: MINUTES=0, HOURS=1, DAYS=2, WEEKS=3, MONTHS=4,
     * PERCENT=5, YEARS=6, ELAPSED_MINUTES=7 ... ELAPSED_YEARS=12, ELAPSED_PERCENT=13,
     * NULL=14. Elapsed variants use the same multipliers as their non-elapsed forms.
     * PERCENT, NULL and unknown values fall back to minutes (1.0) so that malformed
     * data never indexes out of bounds.
     *
     * @param timeUnitValue MPXJ TimeUnit value (0-based)
     * @return minutes per unit
     */
    public static double minutesPerUnit(int timeUnitValue) {
        switch (timeUnitValue) {
            case 1:  // HOURS
            case 8:  // ELAPSED_HOURS
                return 60.0;
            case 2:  // DAYS
            case 9:  // ELAPSED_DAYS
                return 1440.0;
            case 3:  // WEEKS
            case 10: // ELAPSED_WEEKS
                return 10080.0;
            case 4:  // MONTHS
            case 11: // ELAPSED_MONTHS
                return 43200.0;
            case 6:  // YEARS
            case 12: // ELAPSED_YEARS
                return 518400.0;
            default: // MINUTES(0), ELAPSED_MINUTES(7), PERCENT(5), ELAPSED_PERCENT(13), NULL(14), unknown
                return 1.0;
        }
    }

    /**
     * Convert an MPXJ Duration to milliseconds, returning 0L for null.
     */
    public static long toMillis(Duration d) {
        if (d == null)
            return 0L;
        return (long) (d.getDuration() * minutesPerUnit(d.getUnits().getValue()) * 60000.0);
    }

    /**
     * Converts an MPXJ duration to the encoded duration representation required by
     * task schedules. Raw millisecond values can overlap the internal unit bits and
     * are therefore not safe to pass to {@code Task.setDuration} directly.
     */
    public static long toProjectDuration(net.sf.mpxj.Duration duration) {
        if (duration == null)
            return 0L;
        return com.microproject.datatype.Duration.getInstance(duration.getDuration(), toProjectTimeUnit(duration.getUnits()));
    }

    private static int toProjectTimeUnit(net.sf.mpxj.TimeUnit unit) {
        if (unit == null)
            return TimeUnit.MINUTES;
        return switch (unit) {
            case MINUTES -> TimeUnit.MINUTES;
            case HOURS -> TimeUnit.HOURS;
            case DAYS -> TimeUnit.DAYS;
            case WEEKS -> TimeUnit.WEEKS;
            case MONTHS -> TimeUnit.MONTHS;
            case YEARS -> TimeUnit.YEARS;
            case PERCENT -> TimeUnit.PERCENT;
            case ELAPSED_MINUTES -> TimeUnit.ELAPSED_MINUTES;
            case ELAPSED_HOURS -> TimeUnit.ELAPSED_HOURS;
            case ELAPSED_DAYS -> TimeUnit.ELAPSED_DAYS;
            case ELAPSED_WEEKS -> TimeUnit.ELAPSED_WEEKS;
            case ELAPSED_MONTHS -> TimeUnit.ELAPSED_MONTHS;
            case ELAPSED_YEARS -> TimeUnit.ELAPSED_YEARS;
            case ELAPSED_PERCENT -> TimeUnit.ELAPSED_PERCENT;
            default -> TimeUnit.MINUTES;
        };
    }
}
