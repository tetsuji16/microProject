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
