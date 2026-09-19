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
package com.microproject.script.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeIntervalsTest {
    private static final long REFERENCE = Instant.parse("2026-01-15T00:00:00Z").toEpochMilli();

    @Test
    void translationSupportsBothDirectionsAndReusesHistory() {
        TimeIntervals intervals = new TimeIntervals();
        intervals.update(REFERENCE, Long.MAX_VALUE, 3);
        List<TimeWindow> original = List.copyOf(intervals.getWin());

        TimeIntervals forward = intervals.translate(2);
        assertEquals(2, forward.getTranslation());
        assertEquals(2, forward.getWin().size());
        assertContiguous(intervals.getWin());

        TimeIntervals backward = intervals.translate(-2);
        assertEquals(-2, backward.getTranslation());
        assertEquals(original, intervals.getWin());
        assertEquals(original.subList(0, 2), backward.getWin());
    }

    @Test
    void translationCanGenerateWindowsBeforeKnownHistory() {
        TimeIntervals intervals = new TimeIntervals();
        intervals.update(REFERENCE, Long.MAX_VALUE, 3);
        long originalStart = intervals.getStart();

        TimeIntervals generated = intervals.translate(-5);

        assertEquals(-5, generated.getTranslation());
        assertEquals(5, generated.getWin().size());
        assertTrue(intervals.getStart() < originalStart);
        assertContiguous(intervals.getWin());
    }

    private static void assertContiguous(List<TimeWindow> windows) {
        for (int index = 1; index < windows.size(); index++)
            assertEquals(windows.get(index - 1).getE(), windows.get(index).getS());
    }
}
