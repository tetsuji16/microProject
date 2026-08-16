/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * a link to http://www.projectlibre.com
 * Graphic Image provided in the Covered Code as file: projectlibre-logo.png with
 * a link to http://www.projectlibre.com
 *******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import java.math.BigInteger;

import com.microproject.core.time.TimephasedType;
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
}